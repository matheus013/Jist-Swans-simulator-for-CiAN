/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetMobilityOD.java
 * RCS:          $Id: StreetMobilityOD.java,v 1.1.1.1 2006/02/20 23:26:42 drchoffnes Exp $
 * Description:  StreetMobilityOD class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Feb 22, 2005
 * Language:     Java
 * Package:      jist.swans.field
 * Status:       Release
 *
 * (C) Copyright 2005, Northwestern University, all rights reserved.
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
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package vans.straw;

import jist.swans.Constants;
import jist.swans.field.FieldInterface;
import jist.swans.misc.Location;
import vans.straw.streets.RoadSegment;
import vans.straw.streets.SegmentNode;
import vans.straw.streets.SegmentNodeInfo;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 *         The StreetMobilityOD class supports mobility between origins
 *         and destinations.
 */
public class StreetMobilityOD extends StreetMobility
{
    // TODO add API support for flows (sets of predefined OD pairs)
    // TODO caching of shortest paths will be extremely useful for performance

    /**
     * 
     * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
     *
     *         The StreetMobilityInfoOD class extends the StreetMobilityInfo
     *         state object by including the origin, destination and path.
     */
    public static class StreetMobilityInfoOD extends StreetMobilityInfo
    {
        /** node's destination road segment */
        SegmentNode destinationSN = null;
        /** node's destination */        
        Location destinationLocation = null;
        /** a linked list of road segments along the path */
        LinkedList path = null;

    }

    /** node's origin road segment */
    Vector originRS = new Vector();
    /** node's origin address */
    Vector originAddress = new Vector();
    /** node's destination road segment */        
    Vector destinationRS = new Vector();
    /** node's destination address */
    Vector destinationAddress = new Vector();
    /** cache of SegmentNodes */
    Vector segmentNodes;
    /** for caching paths, not currently implemented */
    HashMap hm = null;
    /** Helper class for referencing StreetMobility objects in the AStarSearch class. */
    SegmentNodeInfo sni = null;
    /** Prints SMOD-specific messages, if true */
    private static final boolean DEBUG_OD = false;
    /** The minimum path to cache. */
    private static final int MIN_CACHED_PATH_LENGTH = 5;
    /** Determines the mode for OD motion. For example, origins and destinations 
     * can be picked at random or fed from a list of sources and sinks. */
    private static int config = Constants.MOBILITY_STREET_RANDOM;
    /** maximum distance between origin and destination (5 mi) 
     * This reduces the time required for A* */
    float threshold = 8046.72f;

    /**
     * StreetMobilityOD constructor.
     *
     * @param segmentFile the location of the file containing segments
     * @param streetFile  the location of the file containing streets
     * @param shapeFile   the location of the file containing shapes
     * @param degree      the degree of the quad tree
     * @param r           the random object to use for repeatability
     */
    public StreetMobilityOD(String segmentFile, String streetFile,
                            String shapeFile, int degree,
                            Location.Location2D bl, Location.Location2D tr, Random r) {

        super(segmentFile, streetFile, shapeFile, degree, bl, tr);
        this.rnd = r;

        segmentNodes = new Vector(segments.size()); // for caching segmentNode objects
        for (int i = 0; i < segments.size(); i++)
        {
            segmentNodes.add(i, null);
        }
        sni = new SegmentNodeInfo(segments, shapes, intersections, streets, segmentNodes);
        SegmentNode.info = sni;
    }

    /* (non-Javadoc)
    * @see jist.swans.field.Mobility#init(jist.swans.field.FieldInterface, java.lang.Integer, jist.swans.misc.Location)
    */
    public MobilityInfo init(FieldInterface f, Integer id, Location loc) {

        StreetMobilityInfoOD smodi = (StreetMobilityInfoOD) mobInfo.lastElement();
        // calculate first path
        if (smodi.path == null)
        {
            calculatePath(smodi, (RoadSegment) originRS.get(id.intValue()), smodi.rsEnd,
                    (RoadSegment) destinationRS.get(id.intValue()));
        }
        smodi.v = v; // set visualizer object
        smodi.id = id; // set vehicle id
        return smodi;
    }

    /* (non-Javadoc)
    * @see jist.swans.field.StreetMobility#setNextRoad(jist.swans.field.StreetMobilityInfo)
    */
    public void setNextRoad(StreetMobilityInfo smi) {

        StreetMobilityInfoOD smiod = (StreetMobilityInfoOD) smi;


        if (smiod.path.size() > 0 || config == Constants.MOBILITY_STREET_RANDOM)
        {
            // get next intersection
            smiod.nextIs = intersections.findIntersectingRoads(smiod.rsEnd);

            if (smiod.path.size() == 0) // calculate new path
            {
                if (DEBUG_OD) System.out.println("Calculating new path...");

                // this section makes sure that a valid path is found:
                // 1) there must be a path from origin to destination on the map
                // 2) the path must be longer than 4 so that it's substantial
                boolean valid = false;
                while (!valid || smiod.path.size() < 4)
                {
                    RoadSegment nextDest = null;
                    int segmentSize = sni.segment.size();

                    while (nextDest == null ||
                            nextDest.getSelfIndex() == smiod.current.getSelfIndex()
                            || nextDest.getEndPoint().distance(smiod.current.getEndPoint()) > threshold) // get new destination
                    {
                        int i = rnd.nextInt(segmentSize);
                        nextDest = (RoadSegment) sni.segment.get(i);
                    }
                    valid = calculatePath(smiod, smi.current, smi.rsEnd, nextDest);
                }
                if (DEBUG_OD) System.out.println("Current rsEnd: " + smi.rsEnd);
                if (DEBUG_OD) System.out.println("Current road: " + smi.current.printStreetName(sni.streets));

                checkPath(smiod);

            }

            SegmentNode sn = (SegmentNode) smiod.path.get(0);
            RoadSegment rs = (RoadSegment) segments.get(sn.segmentID);
            updateNextRoadSegment(smiod, rs); // update next road
            smiod.path.remove(0); // already used the first entry

            while (smiod.path.size() > 0 )
            {
                // weird intersection type that confuses A*
                // getting on a street for no distance...
                sn = (SegmentNode) smiod.path.get(0); // was zero
                rs = (RoadSegment) segments.get(sn.segmentID);
                if (smiod.rsEnd.distance(rs.getStartPoint()) == 0 ||
                        smiod.rsEnd.distance(rs.getEndPoint())==0)
                {
                    if (DEBUG_OD) System.out.println("Fixing A* bug...");
                    updateNextRoadSegment(smiod, rs);
                    smiod.path.remove(0);
                    
                }
                else
                {
                    break;
                }
            }
        }
        else
        {
            smiod.nextRS = null;
            smiod.nextEnd = null;
        }
    }


    /**
     * @param smiod
     */
    public void checkPath(StreetMobilityInfoOD smiod) {
        SegmentNode temp;
        RoadSegment rsTemp;
        // if the current end point is equal to an end point of the second road in 
        // the list, then remove the first road in the list because it's redundant
        if (smiod.path.size() > 1) {
            temp = (SegmentNode) smiod.path.get(1);
            rsTemp = (RoadSegment) segments.get(temp.segmentID);
            if (smiod.rsEnd.distance(rsTemp.getStartPoint()) == 0 ||
                    smiod.rsEnd.distance(rsTemp.getEndPoint())==0)
            {
                if (DEBUG_OD) System.out.println("Removing first node in list because it is redundant!");
                smiod.path.remove(0);

            }
        }


        temp = (SegmentNode) smiod.path.get(0);
        rsTemp = (RoadSegment) segments.get(temp.segmentID);
        // need to add current road onto list to perform U-turn.
        if (smiod.nextEnd.distance(rsTemp.getEndPoint()) > INTERSECTION_RESOLUTION
                && smiod.nextEnd.distance(rsTemp.getStartPoint())> INTERSECTION_RESOLUTION)
        {
            smiod.path.addFirst(new SegmentNode(smiod.nextEnd, smiod.current.getSelfIndex(),
                    true, false));
        }

    }

    /**
     * This method calculates the path from the origin to the destination
     * road segment.
     * @param smiod The SMIOD object.
     */
    public boolean calculatePath(StreetMobilityInfoOD smiod, RoadSegment origin, Location nextEnd,
                                 RoadSegment destination) {

        // TODO the caller should determine direction   
        if (DEBUG_OD)
        {
            System.out.println("Calculating path...");
            System.out.println("Origin:      " + origin.printStreetName(streets));
            System.out.println("Destination: " + destination.printStreetName(streets));
            System.out.println("Distance: " + origin.getStartPoint().distance(destination.getStartPoint()));
        }

        if (nextEnd == null) {
            if (smiod.rsEnd.distance(origin.getEndPoint())==0)
            {
                nextEnd = origin.getEndPoint();
            }
            else
            {
                nextEnd = origin.getStartPoint();
            }
        }

        if (DEBUG_VIS && v!=null)
        {
            v.colorSegment(origin, Color.RED);
        }

        boolean endStart;
        Location endPoint;
        if (destination.getStartPoint().distance(nextEnd)
                < destination.getEndPoint().distance(nextEnd))
            {
            endPoint = destination.getStartPoint();
            endStart = true;
            }
        else {
            endPoint = destination.getEndPoint();
            endStart = false;
        }


        SegmentNode startNode = new SegmentNode(nextEnd,
                origin.getSelfIndex(), origin.getStartPoint().distance(nextEnd) == 0, true);
        SegmentNode endNode = new SegmentNode(endPoint,
                destination.getSelfIndex(), endStart, false);
        sni.dest = endNode;
        


//      TODO support locations at arbitrary points on road
        smiod.destinationSN = endNode;
        smiod.destinationLocation = destination.getEndPoint();

        AStarSearch ass = new AStarSearch(hm, this); // TODO rename variable
        smiod.path = ass.findPath(startNode, endNode); // find the path

        // no path found
        if (smiod.path.get(0) == null)
        {
            smiod.path.remove(0);
            System.out.println("No path found!");
            return false;
        }

        // check for strange double entry in list     
        for (int i = 1; i < smiod.path.size(); i++)
        {
            if (((SegmentNode) smiod.path.get(smiod.path.size() - i)).segmentID ==
                    ((SegmentNode)smiod.path.get(smiod.path.size()-(i+1))).segmentID)
            {
                if (DEBUG_OD) System.out.println("Removed redundant entry!");
                smiod.path.remove(smiod.path.size() - i);
            }
        }
        if (((SegmentNode) smiod.path.get(0)).segmentID == origin.getSelfIndex() ) {
        	if (DEBUG_OD) System.out.println("Removed redundant initial entry!");
        	smiod.path.remove(0);
        }
        
        if (DEBUG_OD) printPath(smiod.path);

        return true; // path found!
    }

    /**
     * Prints the list of roads along a path.
     * @param path The list of roads to print.
     */
    public void printPath(List path)
    {
        System.out.println("Path: ");
        Iterator it = path.iterator();
        while (it.hasNext())
        {
            SegmentNode sn = (SegmentNode) it.next();
            RoadSegment rs = (RoadSegment) segments.get(sn.segmentID);
            System.out.println(rs.printStreetName(streets));
        }
    }
}