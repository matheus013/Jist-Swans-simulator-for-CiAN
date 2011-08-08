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

import java.util.List;

import ext.jist.swans.mobility.MobilityReplay.Waypoint;

import jist.swans.field.Mobility.MobilityInfo;

public class MobilityReplayMobilityInfo implements MobilityInfo {
	
	// base data
	List<Waypoint> waypoints;
	
	// concurrent data
	Waypoint lastWaypoint = null;
	Waypoint nextWaypoint = null;
		
	int steps;
	int remainingSteps;
	long stepTime;
	
	/**
	 * Returns waypoint next to the current time, or null, if no further
	 * waypoint is available
	 * @param curTime Current JiST time (in nanoseconds which is the official JiST time)
	 * @return Next waypoint
	 */
	public Waypoint getNextWaypoint(long curTime) {
		long minTime = Long.MAX_VALUE;
		Waypoint wp = null;
		for( Waypoint w: waypoints) {
			if (w.time >= curTime && w.time < minTime) {
				minTime = w.time;
				wp = w;
			}
		}
		if (wp != null) {
			waypoints.remove(wp);
		}
		return wp;
	}
	
	public Waypoint getNextWaypoint() {
		Waypoint wp = null;
		if (waypoints.size() > 0) {
			wp = waypoints.get(0);
			waypoints.remove(0);
		}
		return wp;
	}
	
}
