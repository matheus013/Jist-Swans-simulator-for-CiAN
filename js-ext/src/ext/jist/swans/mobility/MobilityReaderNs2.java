/*
 * Ulm University JiST/SWANS project
 * 
 * Author:		Elmar Schoch <elmar.schoch@uni-ulm.de>
 * Version:		0.2			
 * 
 * (C) Copyright 2006, Ulm University, all rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 */
package ext.jist.swans.mobility;

import java.util.regex.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import jist.swans.misc.Location.Location2D;

/**
 * Implementation of MobilityReader for ns-2 movement files Reader is able to
 * recognize Tcl-statements of the following kinds: 1. $node_(25) set X_ 2908.5
 * $node_(25) set Y_ 3.75 2. $ns_ at 2.5 "$node_(16) setdest 4390.2 3.75 36.5"
 * 
 * @author eschoch
 */
public class MobilityReaderNs2 implements MobilityReader {

	protected static final Logger logger = Logger
			.getLogger(MobilityReaderNs2.class.getName());

	private ArrayList<NodeWaypoints> nodeList = new ArrayList<NodeWaypoints>();

	/**
	 * Binding object for list of waypoints per node ID
	 */
	private static class NodeWaypoints {
		int nodeID;
		ArrayList<MobilityReplay.Waypoint> waypoints;

		public NodeWaypoints(int id) {
			this.nodeID = id;
			this.waypoints = new ArrayList<MobilityReplay.Waypoint>();
		}
	}

	public List<MobilityReplay.Waypoint> getWaypoints(int nodeID) {

		if (nodeList == null)
			return null;

		NodeWaypoints nwp = null;
		Iterator<NodeWaypoints> it = nodeList.iterator();
		while (it.hasNext()) {
			NodeWaypoints curWp = it.next();
			if (curWp.nodeID == nodeID) {
				nwp = curWp;
			}
		}

		if (nwp != null) {
			Collections.sort(nwp.waypoints);
			return nwp.waypoints;
		} else {
			return null;
		}
	}

	/**
	 * Parse movement trace file
	 */
	public void readFile(String filename) throws Exception {

		// /////////////////////////////////////////////////////////////
		// Format of ns2 traces:
		// Set position immediately:
		// $node_(25) set X_ 2908.5
		// $node_(25) set Y_ 3.75
		// Set position at certain time:
		// $ns_ at 2.5 "$node_(16) setdest 4390.2 3.75 36.666666666666664"
		// /////////////////////////////////////////////////////////////

		logger.info("Trying to read " + filename);

		// Get a Channel for the source file
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();

		// Get a CharBuffer from the source file
		ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0,
				(int) fc.size());
		Charset cs = Charset.forName("8859_1");
		CharsetDecoder cd = cs.newDecoder();
		CharBuffer cb = cd.decode(bb);

		logger.info("File opened successfully");

		int count = 0;

		// Read list of starting points
		String pattern = "\\$node_\\((\\d+)\\)\\s+set\\s+X\\_\\s+([\\d\\.]+)\\s+\\$node_\\((\\d+)\\)\\s+set\\s+Y\\_\\s+([\\d\\.]+)";
		Pattern p = Pattern.compile(pattern);

		// Run matcher
		Matcher m = p.matcher(cb);

		while (m.find()) {
			// logger.debug("id="+m.group(1)+" x="+m.group(2)+" y="+m.group(4));

			// Create "starting" waypoint at time 0
			MobilityReplay.Waypoint wp = new MobilityReplay.Waypoint();
			wp.type = MobilityReplay.Waypoint.WAYPOINT_CURRENT;
			wp.location = new Location2D(Float.parseFloat(m.group(2)),
					Float.parseFloat(m.group(4)));
			wp.time = 0;
			wp.speed = 0;
			addWaypoint(Integer.parseInt(m.group(1)), wp);
			count++;
		}

		// Read list of waypoints
		pattern = "^\\$ns_\\s+at\\s+([\\d\\.]+)\\s+\\\"\\$node_\\((\\d+)\\)\\s+setdest\\s+([\\d\\.]+)\\s+([\\d\\.]+)\\s+([\\d\\.]+)\\\"";
		p = Pattern.compile(pattern, Pattern.MULTILINE);
		cb.rewind();
		m = p.matcher(cb);
		while (m.find()) {
			// logger.debug("Found WP: id="+m.group(2)+" t="+m.group(1)+" x="+m.group(3)+" y="+m.group(4)+" v="+m.group(5));

			// Create waypoint
			MobilityReplay.Waypoint wp = new MobilityReplay.Waypoint();
			wp.type = MobilityReplay.Waypoint.WAYPOINT_DESTINATION;
			wp.location = new Location2D(Float.parseFloat(m.group(3)),
					Float.parseFloat(m.group(4)));
			wp.time = (long) (Float.parseFloat(m.group(1)) * jist.swans.Constants.SECOND);
			wp.speed = Float.parseFloat(m.group(5));
			addWaypoint(Integer.parseInt(m.group(2)), wp);
			count++;
		}

		logger.info("Done: " + count + " waypoints loaded.");
		if (count == 0) {
			logger.info("  Reason may be the format of the trace file. "
					+ "Currently, the parser only accepts \"$ns_\" and \"$node_\" as variables in the trace file!");
		}

	}

	/**
	 * Adds new waypoint to appropriate list of the node
	 * 
	 * @param nodeID
	 * @param wp
	 */
	private void addWaypoint(int nodeID, MobilityReplay.Waypoint wp) {

		List<MobilityReplay.Waypoint> waypoints = getWaypoints(nodeID);
		logger.debug("Adding waypoint: node=" + nodeID + " " + wp.toString());

		if (waypoints == null) {
			NodeWaypoints nwp = new NodeWaypoints(nodeID);
			nwp.waypoints.add(wp);
			nodeList.add(nwp);
		} else {
			waypoints.add(wp);
		}
	}

	/**
	 * Retrieve bottom left and top right corner values of all waypoints in the
	 * list
	 * 
	 * @return Array of two Location2D objects, where [0] giving min.
	 *         coordinates, [1] max. coordinates found
	 */
	public Location2D[] getCorners() {

		if (nodeList == null || nodeList.size() == 0) {
			return null;
		}

		Location2D[] corners = new Location2D[2];
		Float minx = Float.MAX_VALUE;
		Float maxx = Float.MIN_VALUE;
		Float miny = Float.MAX_VALUE;
		Float maxy = Float.MIN_VALUE;
		for (NodeWaypoints nwp : nodeList) {
			for (MobilityReplay.Waypoint wp : nwp.waypoints) {
				if (wp.location.getX() < minx) {
					minx = wp.location.getX();
				} else {
					if (wp.location.getX() > maxx) {
						maxx = wp.location.getX();
					}
				}

				if (wp.location.getY() < miny) {
					miny = wp.location.getY();
				} else {
					if (wp.location.getY() > maxy) {
						maxy = wp.location.getY();
					}
				}
			}
		}

		corners[0] = new Location2D(minx, miny);
		corners[1] = new Location2D(maxx, maxy);
		return corners;
	}

	/**
	 * Return number of nodes for which we have traces
	 */
	public int getNodeNumber() {
		if (nodeList != null) {
			return nodeList.size();
		} else {
			return 0;
		}
	}

}
