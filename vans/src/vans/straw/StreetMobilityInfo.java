/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetMobilityInfo.java
 * RCS:          $Id: StreetMobilityInfo.java,v 1.1.1.1 2006/02/20 23:26:42 drchoffnes Exp $
 * Description:  StreetMobilityInfo class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Feb 24, 2005
 * Modified		 Jul 23, 2007 by Bjoern Wiedersheim, Ulm University
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

import jist.swans.field.Mobility.MobilityInfo;
import jist.swans.misc.Location;
import vans.straw.streets.Intersection;
import vans.straw.streets.LaneConnector;
import vans.straw.streets.RoadSegment;

import java.util.LinkedList;
import java.awt.Color;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 *         The StreetMobilityInfo class contains state for all street
 *         mobility models.
 */
public class StreetMobilityInfo implements MobilityInfo {

    /** the road segment currently being traveled */
    RoadSegment current;
    /** the location of the end of this segment in the current direction */
    Location rsEnd; 
    /** the next intersection */
    Intersection nextIs;

    /** current speed */
    float currSpeed=0;
    /** maximum speed */
    float maxSpeed=0;
    /** average acceleration rate */
    public static float acceleration = 2.23f; // 5 mph per second
    /** average deceleration rate */
    public static float deceleration = 5*2.23f; // 25 mph per second, to avoid crashes
    /** remaining distance along segment */
    float remainingDist=-1;
    /** next car in front of current car */
    StreetMobilityInfo nextCar = null;
    /** current lane */
    LinkedList<StreetMobilityInfo> currentLane;
    /** current lane connector */
    LaneConnector laneConnector = null;
    /** remaining distance on intersection **/
    float remainingIntersectionDistance=0.0f;
    /** next road segment */
    RoadSegment nextRS = null;
    /** next end */
    public Location nextEnd =null;
    /** location of the vehicle at the center of the road */
    Location offset = null;
    
    public int id;
    
    /* Bjoern Wiedersheim: insert */
    /** color of the vehicle, set by StreetMobility.next depending on node status or acceleration */
    Color color = Color.white;
    
    static final int COL_NOCOL = 0;
    /* all nodes are white */

    static final int COL_ACC = 1;
    /* the color of the nodes reflects the acceleration/deceleration 
     * blue:	deceleration
     * white:	no acceleration/deceleratioin
     * red:		acceleration
     * yellow:	maximum speed
     */
    
    static final int COL_STATUS = 2;
    /* the color of the nodes reflects the status of the nodes
     * green:		actual road segment is free
     * lightgreen:	actual and next road segment is free
     * blue:		u-turn
     * orange:		driving behind vehicle
     * darkorange:	driving behind vehicle on next road segment
     * cyan:		slow down before intersection
     * red:			wait at stop sign or traffic light
     * yellow:		wait at intersection for free lane
     */
    static final int coloring = COL_ACC;
    
    /** If this is a multiline segment, determines which point we're at */
    int ShapePointIndex=-1;
    /** time for each step to take place (seconds) */
    float stepTime=1;
    
    /** extra speed added by driver */
    float extraSpeed = 0;
    
    /** reaction time */
    float spacingBeta = 1.0f;
    /** reciprocal of twice the maximum average deceleration (units: s^2/m)*/
    float spacingGamma = 0.070104f;
    
    /** waiting to take a turn? */
    /* Bjoern Wiedersheim change:
     * changed the type of waiting from boolean to float */
    public float waiting = 0.0f;
    
    /** optional statistical information */
    /** accumulator for average speed */
    float speedSum=0;
    /** linked list for roads taken */
    LinkedList<Integer> roads= new LinkedList<Integer>();
    
    public VisualizerInterface v = null;
    /** number of times this node has waited to make a turn */
    public int waitCount = 0;
    
    /**
     * 
     */
    public StreetMobilityInfo() {
        super();
    }
    
    // TODO implement vehicle size in here      
    public float getRemainingDist()
    {
        return remainingDist;
    }

    public StreetMobilityInfo getNextCar()
    {
        return nextCar;
    }

    /**
     * Sets the max speed according to the driver's habits.
     * @param limit the posted speed limit
     */
    public void setMaxSpeed(float limit)
    {
        // vague evidence online seems to indicate that this distribution is Gaussian
        maxSpeed = limit + extraSpeed;

    }

    /**
     * @return current road segment
     */
    public RoadSegment getCurrentRS() {
        return current;
    }

    /**
     * @return end road segment
     */
    public Location getRSEnd() {
        return rsEnd;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#getSpeed()
     */
    public float getSpeed() {

        return currSpeed;
    }

    /* (non-Javadoc)
     * @see jist.swans.field.Mobility.MobilityInfo#getBearing()
     */
    public Location getBearing() {
        if (nextEnd.distance(current.getEndPoint())==0)
        {
            return current.getStartPoint().bearing(current.getEndPoint());
        }
        else
        {
            return current.getEndPoint().bearing(current.getStartPoint());
        }
    }

    /**
     * @return next intersection
     */
    public Intersection getNextIntersection() {
        return nextIs;
    }

    /** Returns the speed adjusted for driver behavior */
    public int getAdjustedSpeed() {
        double max = current.getSpeedLimit() * 0.2;
        double min = current.getSpeedLimit() * (-0.2);
        if (extraSpeed < 0 ) 
        {
            return (int) (current.getSpeedLimit() + (extraSpeed > min ? extraSpeed : min));
        }
        else 
            return (int) (current.getSpeedLimit() + (extraSpeed < max ? extraSpeed : max));
    }

	/** Returns the next road segment. */
    public RoadSegment getNextRS() {
        return nextRS;
    }

    /** Returns the current lane */
    public int getLane() {
    	return current.getLane(currentLane);
    }
}



