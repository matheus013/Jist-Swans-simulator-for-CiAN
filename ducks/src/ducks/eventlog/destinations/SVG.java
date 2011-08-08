/*
 * Ulm University DUCKS project
 * 
 * Author:		Elmar Schoch <elmar.schoch@uni-ulm.de>
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
package ducks.eventlog.destinations;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import jist.swans.Constants;
import jist.swans.misc.Location;
import ducks.driver.SimParams;
import ducks.eventlog.EventLog;

/**
 * Plot log data in SVG File
 * 
 * Required parameter: ducks.eventlog.dest.svg.outputfile
 * 
 * The outputfilename may contain placeholders {your.property.name}, which will 
 * be replaced by the given property of the simulation instance.
 * For example, "trace-{ducks.general.nodes}.txt" will create a file named
 * "trace-100.txt" if the simulation instance had a setting "ducks.general.nodes=100".
 * 
 * @author Elmar Schoch
 *
 */
public class SVG extends EventLog {
	String datafilename;
	float scalingX = 1;
	float scalingY = 1;
	
	int nodeHeight = 6;
	int nodeWidth = 10;
	int relWidth = -nodeWidth / 2;
	int relHeight = -nodeHeight / 2;
	String nodeColor = "red";
	
	int padding = 5;
	PrintWriter out;
	Map<Integer, NodeData> dataout;
	
	
	protected static final Logger logger = Logger.getLogger(SVG.class.getName());
	
	public static class NodeData {
		int nodeID;
		ByteArrayOutputStream out;
		Location lastLoc;
		long lasttime;
		int pathSteps;
	}
	

	@Override
	public void configure(Properties config, String configprefix) {
		dataout = new Hashtable<Integer, NodeData>();
		datafilename = configStringReplacer(config,config.getProperty(configprefix+".outputfile"));
		try {
			out = new PrintWriter(new FileOutputStream(datafilename,false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		logger.info("SVG output goes to "+datafilename);
		
		String scx = config.getProperty(configprefix+".scaling.x");
		if (scx != null ) 
			try { scalingX = Float.parseFloat(scx); } 
		    catch (Exception e) { logger.warn("scaling.x is no valid float: "+scx); }
		
		String scy = config.getProperty(configprefix+".scaling.y");
		if (scy != null ) 
			try { scalingY = Float.parseFloat(scy); } 
		    catch (Exception e) { logger.warn("scaling.y is no valid float: "+scy); }
		                  
		String nWidth  = config.getProperty(configprefix+".node.width");
		if (nWidth != null)
			try { nodeWidth = Integer.parseInt(nWidth); } 
	    	catch (Exception e) { logger.warn("node.width is no valid int: "+nWidth); }
		String nHeight = config.getProperty(configprefix+".node.height");
		if (nHeight != null)
			try { nodeHeight = Integer.parseInt(nHeight); } 
	    	catch (Exception e) { logger.warn("node.height is no valid int: "+nHeight); }
	    String pad = config.getProperty(configprefix+".padding");
	    if (pad != null)
				try { padding = Integer.parseInt(pad); } 
		    	catch (Exception e) { logger.warn("padding is no valid int: "+pad); }
	    String col = config.getProperty(configprefix+".node.color");
	    if (col != null)
	    	nodeColor  = col;
		
		String fsx = config.getProperty(SimParams.SCENE_NAMEPSPACE+"."+SimParams.SCENE_FIELD_SIZE_X);
		if (fsx == null) { fsx = "500"; }
		String fsy = config.getProperty(SimParams.SCENE_NAMEPSPACE+"."+SimParams.SCENE_FIELD_SIZE_Y);
		if (fsy == null) { fsy = "500"; }
		
		int width = (int) Math.floor(Integer.parseInt(fsx) * scalingX);
		int height = (int) Math.floor(Integer.parseInt(fsy) * scalingY);
		
		width += 2 * padding;
		height += 2 * padding;
		
		out.println("<?xml version=\"1.0\" standalone=\"no\"?>");
		out.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		out.println("<!-- SVG Mobility Visualization for JiST/SWANS, (C) Elmar Schoch, Ulm University -->");
		// TODO: Sizes
		out.println("<svg width=\""+width+"\" height=\""+height+"\" xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">");		
	}
	
	@Override
	public void finalize() {
		
		Iterator i = dataout.keySet().iterator();
		
		while (i.hasNext()) {
			NodeData nd = (NodeData) dataout.get(i.next());
			try {
				nd.out.write("</rect>".getBytes());
				nd.out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
			out.println(nd.out.toString());
		}
		out.println("</svg>");
		out.flush();
	}

	@Override
	public void logEvent(int node, long time, Location loc, String type, String comment) {
		// Node movement
		if (type.equals("move")) {
			
			logger.debug("movement log: t="+time+" id="+node+" x="+loc.getX()+" y="+loc.getY());
			
			NodeData nd = (NodeData) dataout.get(node);
			if (nd == null) {
				nd = new NodeData();
				nd.nodeID = node;
				nd.out = new ByteArrayOutputStream();
				nd.lastLoc = loc;
				nd.lasttime = time;
				nd.pathSteps = 0;
				
				dataout.put(node,nd);
				
				String rect = "<rect id=\""+node+"\" x=\""+relWidth+"\" y=\""+relHeight+"\" height=\""+nodeHeight+"\" width=\""+nodeWidth+"\" fill=\""+nodeColor+"\" stroke=\"none\">\n";
				String set = "<set attributeName=\"visibility\" attributeType=\"CSS\" to=\"visible\" begin=\"0s\" fill=\"freeze\" />\n";
				
				try {
					nd.out.write(rect.getBytes());
					nd.out.write(set.getBytes());
					nd.out.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				if (loc.equals(nd.lastLoc) || time == nd.lasttime) {
					// Nothing to do...
				} else {
					float x_old = (scalingX == 1 ? nd.lastLoc.getX() : nd.lastLoc.getX() * scalingX);
					float y_old = (scalingY == 1 ? nd.lastLoc.getY() : nd.lastLoc.getY() * scalingY);
					float x_new = (scalingX == 1 ? loc.getX() : loc.getX() * scalingX);
					float y_new = (scalingY == 1 ? loc.getY() : loc.getY() * scalingY);
					
					x_old += padding; y_old += padding; x_new += padding; y_new += padding;
					
					String ani = "<animateMotion "+
					                 "path=\"M"+x_old+","+y_old+" "+
					                 "L"+x_new+","+y_new+"\" "+
					                 "begin=\""+(nd.lasttime / Constants.MILLI_SECOND)+"ms\" "+
					                 "end=\""+(time / Constants.MILLI_SECOND)+"ms\" "+
					                 "dur=\""+((time-nd.lasttime)/Constants.MILLI_SECOND)+"ms\" "+
					                 "repeatCount=\"1\" rotate=\"auto\" fill=\"freeze\" />\n";

					nd.lastLoc = loc;
					nd.lasttime = time;
					try {
						nd.out.write(ani.getBytes());
						nd.out.flush();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
