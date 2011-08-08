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

import org.apache.log4j.Logger;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.FieldInterface;
import jist.swans.field.Mobility;
import jist.swans.misc.Location;
import jist.swans.misc.Location.Location2D;

public class MobilityReplay implements Mobility {
	
	protected static final Logger logger = Logger.getLogger(MobilityReplay.class.getName());
		
	/**
	 * Waypoint gives either
	 * - current waypoints or
	 * - waypoints to go to
	 * 
	 * @author eschoch
	 */
	public static class Waypoint implements Comparable<Waypoint> {
		
		public static final int WAYPOINT_CURRENT = 1;
		public static final int WAYPOINT_DESTINATION = 2;
		
		public int type;

		// location and time values depend on waypoint type:
		// type=WAYPOINT_CURRENT: location at time 
		// type=WAYPOINT_DESTINATION: location to go to, starting at time
		public Location2D location;
		public long time;
		// speed denotes, how fast a node moves towards the location
		public float speed;
		
		public String toString() {
			return "at t="+time+" go to x="+location.getX()+" y="+location.getY()+" v="+speed;
		}

		public int compareTo(Waypoint o) {
			if (this.time < o.time) return -1;
			if (this.time > o.time) return 1;
	        return 0;
        }
	}
	
	private Location2D bounds;
	private Location2D[] foundCorners;
	private int precision;
	private MobilityReader mr;
	
	/**
	 * Initialize the MobilityReplay model which is suitable to replay a waypoint based,
	 * predifined mobility pattern from a file.
	 * 
	 * @param bounds Bounds of the field. Will be checked with contents of file
	 * @param precision Wanted precision in meters (-> mobility makes step every ... meters)
	 * @param file File to read
	 * @param readerClass Classname of the desired Reader
	 *            
	 */
	public MobilityReplay(Location.Location2D bounds, int precision, String file, String readerClass) throws Exception {
		this.bounds = bounds;
		this.precision = precision;
		
		mr = null;
		try {
			mr = (MobilityReader) Class.forName(readerClass).newInstance();
        } catch (Exception e) {
	       throw new Exception("MobilityReplay: Could not load reader: "+readerClass+" :"+e.getMessage());
        }
		try {
			
			mr.readFile(file);
		} catch (Exception e) {
			throw new Exception("Could not load mobility file (file="+file+", workingdir="+System.getProperty("user.dir")+"): "+e.getMessage());
		}
		
		foundCorners = mr.getCorners();
		if (bounds != null) {
			if (Math.abs(foundCorners[1].getX() - foundCorners[0].getX()) > bounds.getX() ||
				Math.abs(foundCorners[1].getY() - foundCorners[0].getY()) > bounds.getY() ) {
				
				logger.warn("Size of field and boundaries of mobility do not fit: "+
						"size_x="+bounds.getX()+" size_y="+bounds.getY()+
						", bottomleft=("+foundCorners[0].getX()+","+foundCorners[0].getY()+"]"+
						", topright=("+foundCorners[1].getX()+","+foundCorners[1].getY());
			}
		} 
		
		if (precision == 0) {
			throw new Exception("MobilityReplay: Precision value in meters must not be zero");
		}
		
	}

	/**
	 * Retrieve the initial location of a node
	 * @param id the node for which the location is needed
	 * @return location object, or null, if no location was available
	 */
	public Location getInitialPosition(Integer id) {
		Location loc = null;
		List<Waypoint> wpl = mr.getWaypoints(id);
		if (wpl != null) {
			if (wpl.size() > 0) {
				loc = wpl.get(0).location;
			}
		}
		return loc;
	}
	
	public int getNodeNumber() {
		if (mr != null) {
			return mr.getNodeNumber();
		} else {
			return 0;
		}
	}
	
	/**
	 * Retrieve smallest and and largest corner coordinates that occur in
	 * the scene
	 * @return Array of two Location2D objects, 
	 *         where [0] giving min. coordinates, [1] max. coordinates found 
	 */
	public Location2D[] getCorners() {
		return foundCorners;
	}
	
	// Methods implementing the Mobility interface ...............................
	public MobilityInfo init(FieldInterface f, Integer id, Location loc) {
		
		MobilityReplayMobilityInfo mrmi = new MobilityReplayMobilityInfo();
		try {
			mrmi.waypoints = mr.getWaypoints(id);
			logger.debug("Found "+mrmi.waypoints.size()+" waypoints for node "+id);
			mrmi.lastWaypoint = null;
			
        } catch (Exception e) {
	        logger.error("init: Could not find waypoints for node "+id);
	        mrmi = null;
        }
        
		return mrmi;
	}

	
	public void next(FieldInterface f, Integer id, Location loc, MobilityInfo info) {
		
		MobilityReplayMobilityInfo mrmi = (MobilityReplayMobilityInfo) info;
		
		// Check for consistency
		if (mrmi == null) {
			logger.error("next: No mobility info available for node "+id);
			return;
		}
		
		// If previous waypoint is null, we are at the beginning
		// and thus go to the starting point
		if (mrmi.lastWaypoint == null) {
			
			mrmi.lastWaypoint = mrmi.getNextWaypoint();
			mrmi.remainingSteps = 0;
			logger.debug("initializing mobility: node="+id+" starts "+mrmi.lastWaypoint.toString());
			
			waitUntil(mrmi.lastWaypoint.time);
			f.moveRadio(id, mrmi.lastWaypoint.location );
			
		} else {
			// last waypoint is not null, so lets go for the next one, or 
			// do still do small steps until the next waypoint is reached
			
			if (mrmi.remainingSteps == 0) {
				// waypoint is reached

				// get next waypoint
				mrmi.nextWaypoint = mrmi.getNextWaypoint();

				// if we have one, let's go to it
				if (mrmi.nextWaypoint != null) {

					float distanceToNextWP = loc.distance(mrmi.nextWaypoint.location);

					logger.debug("state: t="+JistAPI.getTime()+" node="+id+" current loc: x="+loc.getX()+" y="+loc.getY());
					logger.debug("selected next wp: start "+mrmi.nextWaypoint.toString()+" (distance="+distanceToNextWP);

                    // jump to next waypoint if type current
                    if (mrmi.nextWaypoint.type == Waypoint.WAYPOINT_CURRENT) {

                        waitUntil(mrmi.nextWaypoint.time);
                        f.moveRadio(id, mrmi.nextWaypoint.location);
                   
                    } else {
                    	// next wp has a speed assigned, so go for it slowly

                    	// see, if we still need to wait a little, until journey starts
                    	waitUntil(mrmi.nextWaypoint.time);

                    	// calculate number of steps
                    	mrmi.steps = (int) StrictMath.max( StrictMath.ceil(distanceToNextWP / this.precision), 1);

                    	// length of a step piece can be calculated from distance divide by number of steps
                    	double delta = distanceToNextWP / mrmi.steps;
                    	// time it takes for one piece depends on the speed the node travels with
                    	mrmi.stepTime = (int) Math.floor( (delta * Constants.SECOND) / mrmi.nextWaypoint.speed );

                    	mrmi.remainingSteps = mrmi.steps;

                    	logger.debug("... steps="+mrmi.steps+" steptime="+mrmi.stepTime);
                    	logger.debug("    --> total time="+mrmi.steps*mrmi.stepTime+" total dist="+((mrmi.steps*mrmi.stepTime)/ (float) Constants.SECOND) * mrmi.nextWaypoint.speed);
                    } // waypoint current or not
				} // next WP != null
			} // remaining steps == 0

			// take step, if next waypoint available, otherwise do nothing
			if (mrmi.nextWaypoint != null) {
				// only move in steps when moving in destination mode
				if(mrmi.nextWaypoint.type == Waypoint.WAYPOINT_DESTINATION) {
					logger.debug("Stepping: current time: " + JistAPI.getTime() + " steptime: " + mrmi.stepTime);
					JistAPI.sleep(mrmi.stepTime);
					Location step = loc.step(mrmi.nextWaypoint.location,mrmi.remainingSteps--);
					f.moveRadioOff(id, step);
				} else {
					logger.debug("No further Waypoint found for node " + id + "!");
				}
			}
		}
	}

    private void waitUntil(long time) {
        // see, if we still need to wait a little, until journey starts
        long now = JistAPI.getTime();
        long wait = time - now;
        if (wait > 0) {
            logger.debug("... waiting for " + (wait) + " [now=" + now
                    + " nextstart=" + time + "]");
            JistAPI.sleep(wait);
        }
    }
}
