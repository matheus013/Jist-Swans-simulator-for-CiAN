/*
 * Ulm University JiST/SWANS project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
 * Version: 0.2
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package ext.jist.swans.mobility;

import java.util.List;

import jist.swans.misc.Location.Location2D;

/**
 * @author eschoch
 * 
 */
public interface MobilityReader
{

    /**
     * Load mobility from file Note that the file needs to be accessible from
     * the JVM on which the simulation is currently running. In terms of DUCKS,
     * this means, that every JiST server needs its own copy of the file
     * 
     * @param filename
     *            The file to load
     * @throws Exception
     *             Exception in case of errors during loading or parsing
     */
    public void readFile(String filename) throws Exception;

    /**
     * Returns a list of waypoints of a specific node.
     * 
     * @param nodeID
     *            The node ID to get waypoints for
     * @return Either the list containing MobiliyReplay.Waypoint objects or null
     *         if the node ID was not found
     */
    public List<MobilityReplay.Waypoint> getWaypoints(int nodeID);

    /**
     * Retrieves an array of 2 Location2D objects, which represent two corners
     * of the field: bottom left (at index 0) and top right (at index 1).
     * 
     * @return The array with 2 Location2D objects or null, if no waypoints are
     *         loaded
     */
    public Location2D[] getCorners();

    /**
     * Retrieve the number of nodes for which the mobility trace file contains
     * waypoints.
     * 
     * @return The number of nodes in the trace file
     */
    public int getNodeNumber();

}
