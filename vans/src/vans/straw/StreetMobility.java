/**
 * C3 - Car to Car Cooperation - Project
 * 
 * File: StreetMobility.java
 * RCS: $Id: StreetMobility.java,v 1.5 2006/06/01 19:49:59 drchoffnes Exp $
 * Description: StreetMobility class (see below)
 * Author: David Choffnes
 * Aqualab (aqualab.cs.northwestern.edu)
 * Northwestern Systems Research Group
 * Northwestern University
 * Created: Tue Dec 7, 2004
 * Modified: Mon Jul 27, 2007 by Bjoern Wiedersheim, Ulm University
 * Language: Java
 * Package: jist.swans.field
 * Status: Release
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package vans.straw;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.FieldInterface;
import jist.swans.field.Mobility;
import jist.swans.misc.Location;
import vans.straw.streets.Intersection;
import vans.straw.streets.RoadSegment;
import vans.straw.streets.Shape;
import vans.straw.streets.SpatialStreets;
import vans.straw.streets.StreetName;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 *         The StreetMobility class is the superclass for other street mobility
 *         classes. Subclasses must define how nodes move about the street;
 *         StreetMobility simply provides functions to load street data,
 *         calculate distances and move nodes along streets.
 */
public abstract class StreetMobility implements Mobility
{

    /* Street mobility constants. */
    /** number of bytes in a road segment object */
    final int                      ROAD_SEGMENT_SIZE           = 44;
    /** number of bytes in a street name object */
    final int                      STREET_NAME_SIZE            = 38;
    /** Meters per degree. */
    // public final static double METERS_PER_DEGREE = 110874.40;
    /** Meters per degree latitude on equator **/
    public final static float      METERS_PER_DEGREE           = 111132;

    /** Degrees per meter. */
    public final static double     DEGREES_PER_METER           = 1 / METERS_PER_DEGREE;

    /**
     * Meters per degree longitude around current map - estimated for middle of
     * current map
     **/
    private float                  currentMetersPerLongitudeDegree;

    /**
     * Maximum number of meters between two streets to be considered part of the
     * same intersection.
     */
    public static final int        INTERSECTION_RESOLUTION     = 4;
    /** average lange width (12 feet) */
    public static final float      LANE_WIDTH                  = 3.6576f;

    /* Street mobility data structures. */
    /** array of road segments */
    Vector<RoadSegment>            segments                    = new Vector<RoadSegment>();
    /** array of street names */
    HashMap<Integer, StreetName>   streets                     = new HashMap<Integer, StreetName>();
    /** array of shapes */
    HashMap<Integer, Shape>        shapes                      = new HashMap<Integer, Shape>();
    /** Quad-tree of road segments, for finding intersections quickly */
    SpatialStreets.HierGrid        intersections;

    /** contains indexes of used streets */
    TreeMap<Integer, StreetName>   usedStreets                 = new TreeMap<Integer, StreetName>();
    /** contains indexes of used shapes */
    TreeMap<Integer, Shape>        usedShapes                  = new TreeMap<Integer, Shape>();

    /** contains array of MobilityInfo objects */
    Vector<StreetMobilityInfo>     mobInfo                     = new Vector<StreetMobilityInfo>();

    /** map boundary specified by user */
    private Location.Location2D    bl;
    private Location.Location2D    tr;

    /** will store the bounds of the map according to segments loaded */
    private float                  maxX                        = (float) (-180 * METERS_PER_DEGREE);
    private float                  maxY                        = (float) (-180 * METERS_PER_DEGREE);
    private float                  minX                        = (float) (180 * METERS_PER_DEGREE);
    private float                  minY                        = (float) (180 * METERS_PER_DEGREE);

    /** maximum number of cars allowed in region */
    int                            maxCars                     = 0;

    /** random object */
    public Random                  rnd                         = new Random();
    /** the visualization object */
    protected VisualizerInterface  v;
    private int                    carToInspect;

    /* debugging constants */
    /** main debug switch */
    final static boolean           DEBUG                       = false;
    /** Records paths if true. */
    private static final boolean   RECORD_STREETS              = false;
    /** Displays real-time debugging info if true. */
    protected static final boolean DEBUG_VIS                   = false;
    /** Vehicles are displaced from center of road if true. */
    public static final boolean    ENABLE_LANE_DISPLACEMENT    = true;
    /** Generate a path on Intersections **/
    public static final boolean    ENABLE_INTERSECTION_TRAFFIC = true;
    /** Number of times to wait at an intersection before turning around. */
    private static final int       WAIT_THRESHOLD              = 30;

    /**
     * Street mobility constructor.
     * 
     * @param segmentFile
     *            segment file path
     * @param streetFile
     *            street name file path
     * @param shapeFile
     *            chain file path
     * @param degree
     *            number of spatial binning levels
     * @param bl
     *            - bottom left coordinate in long/lat
     * @param tr
     *            top right coordinate in long/lat
     */
    public StreetMobility(String segmentFile, String streetFile, String shapeFile, int degree, Location.Location2D bl,
            Location.Location2D tr) {
        RoadSegment rs;

        // init medium meters per degree with given bounds;
        setLongitudeMetersPerDegree(Math.toRadians(bl.getY() + tr.getX()) / 2);

        // convert from degrees to meters
        this.bl = new Location.Location2D((float) (bl.getX() * getLongitudeMetersPerDegree()),
                (float) (bl.getY() * METERS_PER_DEGREE));
        this.tr = new Location.Location2D((float) (tr.getX() * getLongitudeMetersPerDegree()),
                (float) (tr.getY() * METERS_PER_DEGREE));

        System.out.println("Specified region is " + this.bl + ", " + this.tr);

        if (this.bl.getX() > this.tr.getX() || this.bl.getY() > this.tr.getY())
            throw new RuntimeException("StreetMobility constructor: " + "Invalid boundaries!");

        try {
            loadSegmentsFile(segmentFile); // loads the segments
            loadStreetsFile(streetFile); // loads street names
            loadShapesFile(shapeFile); // loads shapes
        } catch (Exception e) {
            System.out.println("Street files not found: " + e.getMessage());
        }

        // update bl and tr
        this.bl = new Location.Location2D(minX, minY);
        this.tr = new Location.Location2D(maxX, maxY);

        updateLocations();

        System.out.println("After loading streets, region is " + this.bl + " ("
                + (this.bl.getX() / getLongitudeMetersPerDegree()) + "�," + (this.bl.getY() * DEGREES_PER_METER) + "�)"
                + ", \n" + this.tr + " (" + (this.tr.getX() / getLongitudeMetersPerDegree()) + "�,"
                + (this.tr.getY() * DEGREES_PER_METER) + "�)" + "  " + (this.tr.getX() - this.bl.getX()) + "m, "
                + (this.tr.getY() - this.bl.getY()) + "m");

        int spacing = INTERSECTION_RESOLUTION * 50;
        degree = (int) (StrictMath.log(StrictMath.max((maxX - minX) / spacing, (maxY - minY) / spacing)) / StrictMath
                .log(2));
        // creates the quad tree to contain the intersection objects
        intersections = new SpatialStreets.HierGrid(new Location.Location2D(0, 0), new Location.Location2D(maxX - minX,
                0), new Location.Location2D(0, maxY - minY), new Location.Location2D(maxX - minX, maxY - minY), degree,
                INTERSECTION_RESOLUTION);

        // insert each segment into the quad tree
        for (int i = 0; i < segments.size(); i++) {
            rs = (RoadSegment) segments.elementAt(i);
            // store segment length
            rs.setLength((Shape) shapes.get(new Integer(rs.getShapeIndex())));

            maxCars += rs.getMaxCars() * rs.getNumberOfLanes() * rs.getNumberOfDirections();

            // this adds each road segment's end to
            // a distinct intersection object
            intersections.add(rs, true);
            intersections.add(rs, false);
        }

        System.out.println("Maximum number of cars for region: " + maxCars);
        System.out.println("Number of segments loaded: " + segments.size());

    }

    /**
     * Moves coordinates to the (0,0) reference frame, the origin being in the
     * top-left corner.
     */
    private void updateLocations() {
        Iterator it = segments.iterator();

        // fix segments
        while (it.hasNext()) {
            RoadSegment rs = (RoadSegment) it.next();

            rs.setEndPoint(convertFromStreets(rs.getEndPoint()));
            rs.setStartPoint(convertFromStreets(rs.getStartPoint()));
        }

        // fix shapes
        it = shapes.values().iterator();
        while (it.hasNext()) {
            Shape sh = (Shape) it.next();
            for (int i = 0; i < sh.points.length; i++) {
                sh.points[i] = convertFromStreets(sh.points[i]);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jist.swans.field.Mobility#init(jist.swans.field.FieldInterface,
     * java.lang.Integer, jist.swans.misc.Location)
     */
    public abstract MobilityInfo init(FieldInterface f, Integer id, Location loc);

    /**
     * calculates the distance in front of the vehicle
     * 
     * @param smi
     *            object of the vehicle
     * @param id
     *            id of the vehicle (just for debug information)
     * @return free distance to drive
     * 
     *         written by
     * @author Bjoern Wiedersheim
     * 
     *         the calculation depends on the following variables:
     *         smi.remainingDist distance to the end of the road segment
     *         smi.nextCar pointer to the car in front smi.current, smi.nextRS
     *         calculation of remaining distance
     * 
     *         the following variables are changed smi.waiting remaining time to
     *         wait at traffic light or stop sign smi.waitCount amount of
     *         timesteps already waiting smi.color color of node status
     * 
     *         replaces the former function "step"
     * 
     *         calculates the free distance of the actual and eventually the
     *         following road segment
     */

    private float calcFreeDistance(StreetMobilityInfo smi, float maxDist, Integer id) {
        // security factor: keeps to vehicles from crashing in most cases
        boolean debug = false;
        float remainingDistance = smi.remainingDist;

        // when vehicle has to wait, return zero distance
        if (smi.waiting >= smi.stepTime) {
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " has to wait " + smi.waiting);
            return remainingDistance;
        }

        boolean firstCar = false;
        // find next car on road, if there is any
        if (smi.nextCar == null || ((StreetMobilityInfo) smi.currentLane.getFirst()) == smi) {
            firstCar = true;
        }

        if (!firstCar) {
            // not the first car, we don't have to check further road segments
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " other car in front");
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = Color.orange;
            return (remainingDistance - smi.nextCar.remainingDist) - RoadSegment.CAR_LENGTH - RoadSegment.SPACE;
        }

        // remaining distance is sufficient
        if (remainingDistance >= maxDist) {
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = Color.green;
            return remainingDistance;
        }

        // goal reached, slow down
        if (smi.nextRS == null) {
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " game over");
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = Color.black;
            return remainingDistance;
        }

        // u-turn, slow down
        if (smi.nextRS == smi.current && remainingDistance > 0.5f) {
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " u-turn ahead");
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = Color.blue;
            return remainingDistance;
        }

        // crossing, slow down
        if (smi.nextRS.getStreetIndex() != smi.current.getStreetIndex() && remainingDistance > 3.0f) {
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " crossing ahead");
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = Color.cyan;
            return remainingDistance;
        }

        // find intersecting roads
        Intersection is = intersections.findIntersectingRoads(smi.rsEnd);
        if (is == null) {
            v.colorSegment(smi.current, Color.RED);
            throw new RuntimeException("Null intersection error! Try reducing the degree of" + " the quad tree");
        }

        // find out if we must stop at the intersection (red traffic lights or
        // stop signs)
        float pause = 0.0f;
        if (smi.waitCount == 0) {
            pause = is.getPauseTime(smi.current, smi.nextRS, smi);
        }
        // don't move at all if at red light or stop sign
        if (pause > 0) {
            // pause at intersection if necessary
            smi.waiting = pause;
            smi.waitCount++;
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " has to wait: " + pause);
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = Color.red;
            return remainingDistance;
        }

        LinkedList nextLane = smi.nextRS.getFreeLane(smi.nextEnd);
        // simple case, waiting to make a turn
        // check if there is room to add car to road
        // if not, wait for a second and exit
        if (nextLane == null) {
            smi.waitCount++;
            smi.waiting = smi.stepTime;
            if (smi.waitCount > WAIT_THRESHOLD) {
                // turn around only if on random mode
                if (smi instanceof StreetMobilityRandom.StreetMobilityInfoRandom)
                    setNextRoad(smi);
            }
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " no free space on next lane");
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = Color.yellow;
            return remainingDistance;

        }

        // calculate distance to next car
        if (nextLane.size() > 0) {
            remainingDistance = +smi.remainingIntersectionDistance + smi.nextRS.length
                    - ((StreetMobilityInfo) nextLane.getLast()).remainingDist - RoadSegment.CAR_LENGTH
                    - RoadSegment.SPACE;
            remainingDistance = StrictMath.max(smi.remainingDist, remainingDistance);
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id
                        + " car in front on new road, new remain " + remainingDistance);
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                smi.color = new Color(255, 100, 0);
            return remainingDistance;
        }

        remainingDistance += smi.remainingIntersectionDistance + smi.nextRS.length;
        if (debug)
            System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " first car on new road, remain "
                    + remainingDistance);
        if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
            smi.color = new Color(150, 255, 150);
        return remainingDistance;
    }

    /**
     * implements jist.swans.field.Mobility.next
     * 
     * sets a node to a new position
     * 
     * @param f
     *            field interface of the node
     * @param id
     *            id of the node
     * @param centerLine
     *            current position of the node
     * @param info
     *            object of the node, needs to be a StreetMobilityInfo otherwise
     *            cast will fail
     * 
     *            the value of centerLine will be updated, if a car reaches a
     *            new road segment, moveToNextRaod is called, which changes a
     *            variety of variables of the smi object
     * 
     *            position of the field interface will be updated
     * 
     *            the mobility model is by far not perfect, so crashes may
     *            happen (but are not detected and have no consequences) due to
     *            that, in special cases (usualy decelerating from high speed),
     *            there may appear larger accelrations/decelerations than
     *            defined in StreetMobilityInfo vehicles don't yield right of
     *            way
     * 
     *            a speed peak may also happen, when a car enters a new road,
     *            because positions are not interpolated on intersections
     * 
     *            modified by
     * @author Bjoern Wiedersheim
     * 
     */

    public void next(FieldInterface f, Integer id, Location centerLine, MobilityInfo info) {
        boolean debug = false;
        if (Visualizer.getActiveInstance() != null)
            Visualizer.getActiveInstance().updateTime(JistAPI.getTime());

        try {
            StreetMobilityInfo smi = (StreetMobilityInfo) info;

            if (debug && smi.current.getShapeIndex() >= 0)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX() + " Y: "
                        + centerLine.getY() + " RS: " + smi.current.getSelfIndex() + " Shape: "
                        + smi.current.getShapeIndex() + " smi.shape " + smi.ShapePointIndex);

            if (ENABLE_LANE_DISPLACEMENT && smi.remainingIntersectionDistance == 0) {
                centerLine = centerLine.add(getInverseDisplacement(smi)); // remove
                                                                          // displacement
                                                                          // from
                                                                          // center
            }

            if (smi.remainingIntersectionDistance == 0) {
                smi.remainingDist = StrictMath.max(centerLine.distance(smi.rsEnd) - smi.current.intersectionDistance,
                        0.0f);
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX()
                            + " Y: " + centerLine.getY() + " remaining distance: " + smi.remainingDist);
            } else {
                smi.remainingDist = StrictMath.max(smi.remainingIntersectionDistance + smi.current.length, 0.0f);
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX()
                            + " Y: " + centerLine.getY() + " remaining distance: " + smi.remainingDist
                            + " smi.distance on intersecrtion " + smi.remainingIntersectionDistance
                            + " length of roadsegment " + smi.current.length);
            }

            smi.speedSum += smi.currSpeed; // update speed

            if (DEBUG_VIS && v != null)
                v.setToolTip(id.intValue(), "Node " + id + ":\nRemaining distance: " + smi.remainingDist);

            // arrived at end point, remove car from road
            if (smi.remainingDist < 0.01 && smi.nextRS == null) {
                Location.Location2D offMap = new Location.Location2D(Float.MAX_VALUE, Float.MAX_VALUE);

                smi.current.removeNode(smi, smi.currentLane, mobInfo); // remove
                                                                       // from
                                                                       // old
                                                                       // one

                JistAPI.sleep((long) (smi.stepTime) * Constants.SECOND);
                f.moveRadio(id, offMap); // make actual move
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX()
                            + " Y: " + centerLine.getY() + " removed");
                return;
            }

            // adjust speed, look for car in front and move appropriately

            // calculate maximum acceleration
            float maxAcc = StrictMath.min(StrictMath.max(((smi.getAdjustedSpeed() - smi.currSpeed) / smi.stepTime), 0),
                    StreetMobilityInfo.acceleration);

            // calculate distance at full acceleration
            float maxDist = (float) (0.5 * maxAcc * Math.pow(smi.stepTime, 2) + smi.currSpeed * smi.stepTime + Math
                    .pow(smi.currSpeed + maxAcc * smi.stepTime, 2.0) / StreetMobilityInfo.deceleration);

            // calculate distance at full deceleration
            float minDist = (float) StrictMath.max(-0.5 * StreetMobilityInfo.deceleration * Math.pow(smi.stepTime, 2)
                    + smi.currSpeed * smi.stepTime, 0);

            // get free space
            float newDist = StrictMath.max(calcFreeDistance(smi, maxDist, id), 0);

            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX() + " Y: "
                        + centerLine.getY() + " maxAcc " + maxAcc + " maxDist " + maxDist + " minDist: " + minDist
                        + " newDist: " + newDist);

            float newAcc;
            // calculate breaking distance with one fourth of maximum
            // deceleration
            double brDistance = Math.pow(smi.currSpeed, 2) / StreetMobilityInfo.deceleration * 2.0f;

            // decision: accelerate or decelerate
            // to smooth decelerations, brDistance is multiplicated with factor
            // > 1
            if (newDist > brDistance && newDist > 0.5 && smi.currSpeed * 2.0f < newDist) {
                if (smi.currSpeed < 5.0f) {
                    newAcc = maxAcc / 2.0f;
                } else {
                    newAcc = (float) StrictMath.min((smi.getAdjustedSpeed() - smi.currSpeed) * 0.5, maxAcc);
                }
            } else {
                if (newDist > 0) {
                    // decelerate 10% more than calculated
                    newAcc = (float) StrictMath.max(-0.5 * Math.pow(smi.currSpeed, 2) / newDist * 1.1,
                            -StreetMobilityInfo.deceleration);
                } else {
                    newAcc = StrictMath.max(-smi.currSpeed, -StreetMobilityInfo.deceleration);
                }
            }
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX() + " Y: "
                        + centerLine.getY() + " brDist " + brDistance + " newAcc " + newAcc);

            // calculate distance driven in this timestep
            float distance = (float) StrictMath.max(0.5 * newAcc * Math.pow(smi.stepTime, 2) + smi.currSpeed
                    * smi.stepTime, 0);
            float oldSpeed = smi.currSpeed;

            // calculate new speed
            smi.currSpeed = StrictMath.max(smi.currSpeed + newAcc * smi.stepTime, 0);

            // set colors depending on acceleration
            if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_ACC) {
                if (newAcc > 0) {
                    float acc = 1.0f - StrictMath.min(1.0f, newAcc / StreetMobilityInfo.acceleration);
                    smi.color = new Color(acc, 1.0f, acc);
                }
                if (newAcc <= 0) {
                    float dec = 1.0f + StrictMath.min(1.0f, newAcc / StreetMobilityInfo.deceleration);
                    smi.color = new Color(1.0f, dec, dec);
                }
                if (smi.currSpeed >= smi.getAdjustedSpeed() * 0.99f) {
                    smi.color = Color.yellow;
                }
            }

            // although it is still possible to have some crashes
            if (distance > newDist) {
                if (distance - newDist > 0.5f) {
                    smi.color = Color.magenta;
                    if (debug)
                        System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX()
                                + " Y: " + centerLine.getY() + " newDist " + newDist + " minDist: " + minDist
                                + " maxDist: " + maxDist + " remain: " + smi.remainingDist);
                    if (debug)
                        System.out.println("entering safety distance -----------------------------------------------");
                }

                if (newDist <= smi.remainingDist && distance > smi.remainingDist) {
                    if (distance - newDist > 0.5f) {
                        if (debug)
                            System.out.println("Our Lord prevented the car from slipping into the next street");
                        smi.color = Color.black;
                    }
                    distance = smi.remainingDist;
                    smi.currSpeed = distance / smi.stepTime;
                }
            }
            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " X: " + centerLine.getX() + " Y: "
                        + centerLine.getY() + " old speed " + oldSpeed + " new speed " + smi.currSpeed + " minDist: "
                        + minDist + " maxDist: " + maxDist + " distance " + distance + " adjspeed "
                        + smi.getAdjustedSpeed());

            // if car entered a new road, set new coordinates
            if (distance > smi.remainingDist) {
                // if really drives into next segment, set on new Lane
                LinkedList<StreetMobilityInfo> nextLane = smi.nextRS.addNode(smi, smi.nextEnd, mobInfo);

                if (nextLane == null) {
                    System.out.println("Exception: nextLane null");
                    throw new RuntimeException("RoadSegment.addNode returns null");
                }

                // the vehicle has been moved to the next segment
                int rsIndex = smi.current.getSelfIndex();

                Intersection is = intersections.findIntersectingRoads(smi.rsEnd);
                if (is == null) {
                    v.colorSegment(smi.current, Color.RED);
                    throw new RuntimeException("Null intersection error! Try reducing the degree of" + " the quad tree");
                }

                if (ENABLE_INTERSECTION_TRAFFIC && JistAPI.getTime() > 0) {
                    Shape startShape = null, endShape = null;
                    if (smi.current.getShapeIndex() >= 0) {
                        startShape = shapes.get(smi.current.getShapeIndex());
                    }
                    if (smi.nextRS.getShapeIndex() >= 0) {
                        endShape = shapes.get(smi.nextRS.getShapeIndex());
                    }
                    smi.laneConnector = smi.current.getLaneConnector(smi, nextLane, startShape, endShape);
                    smi.remainingIntersectionDistance = smi.laneConnector.getLength();
                }

                float rd = smi.remainingDist;

                moveToNextRoad(id, nextLane, smi, is);
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " entered new road "
                            + smi.current.getSelfIndex() + " on lane: " + smi.current.getLane(smi.currentLane));

                if (smi.current.getSelfIndex() == rsIndex) { // dead end and
                                                             // turned around
                    // slow down
                    if (debug)
                        System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " u-turn");
                    if (StreetMobilityInfo.coloring == StreetMobilityInfo.COL_STATUS)
                        smi.color = Color.blue;
                } else {
                    distance -= rd;
                    if (distance < 0.0f) {
                        // already crossed point of no return
                        distance = 0;
                    }
                }
            }

            // if (smi.current.getShapeIndex()>=0) smi.color = Color.blue;

            if (debug)
                System.out.println("Time: " + JistAPI.getTime() + " node: " + id
                        + " remaining distance on intersection " + smi.remainingIntersectionDistance);

            if (smi.remainingIntersectionDistance <= distance) {
                if (smi.remainingIntersectionDistance > 0.0f) {
                    // car left intersection
                    if (debug)
                        System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " old distance: " + distance
                                + " intersection distance " + smi.current.intersectionDistance);
                    distance += smi.current.intersectionDistance - smi.remainingIntersectionDistance;
                    smi.remainingIntersectionDistance = 0;
                    smi.laneConnector = null;
                    if (smi.rsEnd.distance(smi.current.getEndPoint()) < smi.rsEnd.distance(smi.current.getStartPoint())) {
                        centerLine = smi.current.getStartPoint();
                    } else {
                        centerLine = smi.current.getEndPoint();
                    }
                }
                // move node to new position
                centerLine = pointAt(centerLine, smi, distance);
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " set car on new coordinates "
                            + centerLine.toString() + ", moved distance: " + distance);
            } else {
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " length of intersection "
                            + smi.laneConnector.getLength() + " remaining distance on intersection "
                            + smi.remainingIntersectionDistance + " distance to drive " + distance
                            + " resulting distance on intersection "
                            + (smi.laneConnector.getLength() - smi.remainingIntersectionDistance + distance));
                // car still on intersection
                centerLine = smi.laneConnector.getPointOnConnection(smi.laneConnector.getLength()
                        - smi.remainingIntersectionDistance + distance);
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " set car on new coordinates "
                            + centerLine.toString() + ", moved distance: " + distance);
                smi.remainingIntersectionDistance -= distance;
            }

            // to avoid precision errors, recalculate the remaining distance
            if (smi.remainingIntersectionDistance == 0) {
                smi.remainingDist = StrictMath.max(centerLine.distance(smi.rsEnd) - smi.current.intersectionDistance,
                        0.0f);
            } else {
                smi.remainingDist = StrictMath.max(smi.remainingIntersectionDistance + smi.current.length, 0.0f);
            }

            if (ENABLE_LANE_DISPLACEMENT && smi.remainingIntersectionDistance == 0) {
                smi.offset = getLaneDisplacement(smi);
                centerLine = centerLine.add(smi.offset);
                if (debug)
                    System.out.println("Time: " + JistAPI.getTime() + " node: " + id + " new coordinates "
                            + centerLine.toString() + " offset " + smi.offset.toString());
            }

            // advance simulation time
            JistAPI.sleep((long) (smi.stepTime) * Constants.SECOND);

            if (smi.waiting >= smi.stepTime) {
                smi.waiting -= smi.stepTime;
            }

            f.moveRadio(id, centerLine); // make actual move
            if (Visualizer.getActiveInstance() != null) {
                Visualizer.getActiveInstance().updateNodeLocation(centerLine.getX(), centerLine.getY(), id.intValue());
                if (StreetMobilityInfo.coloring > 0) {
                    Visualizer.getActiveInstance().setNodeColor(id, smi.color);
                }
                try {
                    // Thread.sleep(500);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (ClassCastException e) {
            // different mobility model installed
        } catch (RuntimeException e) // very useful for debugging
        {
            printStreetList(id.intValue());
            throw e;
        }

    }

    /**
     * sets a node on a new road segment and updates a variety of values of the
     * StreetMobilityInfo object
     * 
     * @param id
     *            id of the node (just for debug output)
     * @param nextLane
     *            the lane to set the car on (there has to be enough space for
     *            the node
     * @param smi
     *            StreetMobilityInfo object of the node
     * @param is
     *            the intersection which is passed by the node
     * 
     *            enhanced by parts of the old "step" function, which was
     *            replaced by "calcDistance"
     * 
     *            modified by
     * @author Bjoern Wiedersheim
     */

    private void moveToNextRoad(Integer id, LinkedList<StreetMobilityInfo> nextLane, StreetMobilityInfo smi,
            Intersection is) {
        // reset any values from waiting
        smi.waiting = 0;
        smi.waitCount = 0;
        is.removeWaiting(smi);
        smi.current.removeNode(smi, smi.currentLane, mobInfo); // remove from
                                                               // old one
        smi.currentLane = nextLane; // copy over the linked list
        smi.current = smi.nextRS; // update road features
        smi.rsEnd = smi.nextEnd;

        // calculate distance along segment
        smi.remainingDist = smi.current.getLength();
        setNextCar(smi); // set the car in front
        if (smi.nextCar != null && smi.nextCar == smi) {
            throw new RuntimeException("Car is following itself!");
        }

        setNextRoad(smi); // set next road info

        smi.current.checkLane(smi.currentLane); // TODO remove when done
        // now that we've moved to the next street, we must calculate
        // motion-related info
        smi.setMaxSpeed(smi.current.getSpeedLimit());
        // end case not waiting yet
        smi.ShapePointIndex = -1; // set new shapePointIndex
        if (smi.current.getShapeIndex() > 0) {
            // deal with segments
            Shape shape = (Shape) shapes.get(new Integer(smi.current.getShapeIndex()));

            // if starting at segment "end", the first point in the shape
            // will be the one with the highest index
            if (smi.current.getEndPoint().distance(is.getLoc()) <= INTERSECTION_RESOLUTION)
                smi.ShapePointIndex = shape.points.length;
        }
        // caculate distance along segment
        // smi.remainingDist = smi.current.getLength();
    }

    /**
     * Gets the vector to remove a node's displacement from the center of the
     * road.
     * 
     * @param info
     *            the SMI object for the node
     * @return a displacement vector
     */
    public Location getInverseDisplacement(StreetMobilityInfo smi) {

        Location l = smi.offset;
        return new Location.Location2D(l.getX() * (-1), l.getY() * (-1));
    }

    /**
     * Compatibility implementation of Math.signum from Java 1.5
     * 
     * @see java.lang.Math.signum(float f)
     */
    private static float signum(float f) {
        if (f < 0)
            return -1.0f;
        if (f > 0)
            return 1.0f;
        if (f == Float.NaN)
            return Float.NaN;
        return 0.0f;
    }

    /**
     * Gets the vector to add a node's displacement from the center of the road.
     * Assumes that lane 0 is the inside lane regardless of direction.
     * 
     * @param info
     *            the SMI object for the node
     * @return a displacement vector
     */
    public Location getLaneDisplacement(StreetMobilityInfo smi) {

        Location start;
        Location finish;
        Location normalized;
        float displacement = LANE_WIDTH
                * (smi.current.getNumberOfLanes() - 1.0f - smi.current.getLane(smi.currentLane) + 0.5f);

        // calculate normal vector for displacement

        // simple case: no subsegments
        if (smi.current.getShapeIndex() < 0) {
            if (smi.rsEnd.distance(smi.current.getEndPoint()) == 0) {
                start = smi.current.getStartPoint();
                finish = smi.current.getEndPoint();
            } else {
                start = smi.current.getEndPoint();
                finish = smi.current.getStartPoint();
            }

        } // end if no subsegments
        else // use shape points
        {
            Shape s = (Shape) shapes.get(new Integer(smi.current.getShapeIndex()));

            if (smi.rsEnd.distance(smi.current.getEndPoint()) == 0) {
                if (smi.ShapePointIndex == -1) {
                    start = smi.current.getStartPoint();
                } else {
                    if (smi.ShapePointIndex == s.points.length)
                        start = s.points[smi.ShapePointIndex - 1];
                    else
                        start = s.points[smi.ShapePointIndex];
                }
                if (smi.ShapePointIndex >= s.points.length - 1) {
                    finish = smi.current.getEndPoint();
                } else {
                    finish = s.points[smi.ShapePointIndex + 1];
                }
            } // end if heading toward end point
            else // heading toward "start point"
            {
                if (smi.ShapePointIndex == s.points.length) {
                    start = smi.current.getEndPoint();
                } else if (smi.ShapePointIndex < 0) {
                    start = s.points[0];
                } else {
                    start = s.points[smi.ShapePointIndex];
                }
                if (smi.ShapePointIndex > 0) {
                    finish = s.points[smi.ShapePointIndex - 1];
                } else {
                    finish = smi.current.getStartPoint();
                }
            } // end else heading toward starting point
        } // end else using shape points

        // normalize
        double temp = 1.0 / (start.distance(finish));
        normalized = new Location.Location2D((float) (temp * (finish.getX() - start.getX())),
                (float) (temp * (finish.getY() - start.getY())));

        // find normal, multiply by displacement:
        if (signum(normalized.getX()) == signum(normalized.getY()))
            return new Location.Location2D(displacement * normalized.getY() * (-1), normalized.getX() * displacement);
        if (normalized.getX() == 0)
            if (normalized.getY() > 0)
                return new Location.Location2D(displacement * normalized.getY() * (-1), normalized.getX()
                        * displacement);
            else
                return new Location.Location2D(displacement * normalized.getY() * (-1), normalized.getX()
                        * displacement);
        if (normalized.getY() == 0)
            if (normalized.getX() > 0)
                return new Location.Location2D(displacement * normalized.getY(), normalized.getX() * displacement);
            else
                return new Location.Location2D(displacement * normalized.getY(), normalized.getX() * displacement);
        return new Location.Location2D(displacement * normalized.getY() * (-1), normalized.getX() * displacement);
    }

    /**
     * This method returns the point along the road segment that is the
     * specified distance from the current location. The method determines the
     * point using the information contained in the MobilityInfo object.
     * 
     * @param curr
     *            current location
     * @param info
     *            mobility info object
     * @param dist
     *            distance to travel
     * @return 2D location after moving the specified distance
     */
    public Location pointAt(Location curr, MobilityInfo info, float dist) {
        Shape shape;
        float partialDist = 0;
        Location newLocation = curr; // start at current location

        StreetMobilityInfo smi = (StreetMobilityInfo) info;

        // find shape

        if (smi.current.getShapeIndex() < 0) {
            try {
                return move(newLocation, smi.rsEnd, dist);
            } catch (Exception e) {
                System.out.println("Exception: centerLine: " + newLocation.toString() + " rsEnd: "
                        + smi.rsEnd.toString() + " distance " + dist + " rsStart "
                        + smi.current.getStartPoint().toString() + " rsEnd2 " + smi.current.getEndPoint().toString());
                throw new RuntimeException("StreetMobility:move: Move error!");
            }
        }

        shape = (Shape) shapes.get(new Integer(smi.current.getShapeIndex()));
        int shapePointIndex = smi.ShapePointIndex; // find index into set of
                                                   // points

        // case vehicle is moving toward the "start"
        if (smi.current.getStartPoint().distance(smi.rsEnd) <= INTERSECTION_RESOLUTION) // move
                                                                                        // in
                                                                                        // descending
                                                                                        // order
        {
            // case at beginning of shape
            if (shapePointIndex == shape.points.length) {
                // distance to from start point to last point in shape entry
                partialDist = curr.distance(shape.points[shapePointIndex - 1]);

                if (dist <= partialDist) // didn't reach new shape point
                {
                    return move(newLocation, shape.points[shapePointIndex - 1], dist);
                } else // move to next point
                {
                    dist -= partialDist;
                    shapePointIndex--;
                    newLocation = shape.points[shapePointIndex];
                }
            }

            // get distance to next point in shape entry (or end of road
            // segment)
            if (shapePointIndex > 0) {
                partialDist = newLocation.distance(shape.points[shapePointIndex - 1]);
            } else {
                partialDist = newLocation.distance(smi.current.getStartPoint());
            }

            // iterate through shape points until distance has been covered
            while (partialDist < dist && shapePointIndex > 1) {
                shapePointIndex--;
                newLocation = shape.points[shapePointIndex];
                dist -= partialDist;
                partialDist = newLocation.distance(shape.points[shapePointIndex - 1]);
            }

            // fix for poorly written loop
            if (partialDist < dist && shapePointIndex > 0) {
                shapePointIndex--;
                newLocation = shape.points[shapePointIndex];
                dist -= partialDist;
            }

            // update index of shape point in vehicle state
            smi.ShapePointIndex = shapePointIndex;
            if (shapePointIndex > 0) {
                return move(newLocation, shape.points[shapePointIndex - 1], dist);
            } else {
                float realDist = newLocation.distance(smi.rsEnd);
                if (dist > realDist)
                    dist = realDist;
                return move(newLocation, smi.rsEnd, dist);
            }
        } // end if moving toward "start"

        else // move in ascending order through shape
        {
            // beginning of shape
            if (shapePointIndex == -1) {
                partialDist = curr.distance(shape.points[0]);

                if (dist <= partialDist) // didn't reach new shape point
                {
                    return move(newLocation, shape.points[0], dist);
                } else // move to next point
                {
                    dist -= partialDist;
                    shapePointIndex++;
                    newLocation = shape.points[0];
                }
            }

            // iterate through shape points until distance has been covered
            if (shapePointIndex == shape.points.length - 1) {
                partialDist = newLocation.distance(smi.current.getEndPoint());

            } else {
                partialDist = newLocation.distance(shape.points[shapePointIndex + 1]);
            }

            while (partialDist < dist && shapePointIndex < shape.points.length - 2) {
                shapePointIndex++;
                newLocation = shape.points[shapePointIndex];
                dist -= partialDist;
                partialDist = newLocation.distance(shape.points[shapePointIndex + 1]);
            }
            // fix for poorly written loop
            if (partialDist < dist && shapePointIndex < shape.points.length - 1) {
                shapePointIndex++;
                newLocation = shape.points[shapePointIndex];
                dist -= partialDist;
            }

            // update index in shape point
            smi.ShapePointIndex = shapePointIndex;
            if (shapePointIndex < shape.points.length - 1) {
                return move(newLocation, shape.points[shapePointIndex + 1], dist);
            } else {
                float realDist = newLocation.distance(smi.rsEnd);
                if (dist > realDist)
                    dist = realDist;
                return move(newLocation, smi.rsEnd, dist);
            }

        } // end case moving toward "end"

    }

    /**
     * Changes coordinates from 0-based lat/long to coordinates on this map in
     * latitude and longitude.
     * 
     * @param loc
     *            the location in meters
     * @return the location on the map
     */
    public Location metersToDegrees(Location loc) {
        loc = (Location.Location2D) loc;

        return new Location.Location2D((float) (loc.getX() * DEGREES_PER_METER) + minX,
                (float) (loc.getY() * (getLatitudeDegreesPerMeter())) + minY);
    }

    private float getLatitudeDegreesPerMeter() {
        return 1 / currentMetersPerLongitudeDegree;
    }

    /**
     * Changes coordinates degrees latitude and longitude to 0-based meters.
     * 
     * @param loc
     *            the location in meters
     * @return the location on the map
     */
    public Location degreesToMeters(Location loc) {
        loc = (Location.Location2D) loc;

        return new Location.Location2D((loc.getX() - minX) / getLongitudeMetersPerDegree(), (loc.getY() - minY)
                / (float) METERS_PER_DEGREE);
    }

    private void setLongitudeMetersPerDegree(double lat) {
        currentMetersPerLongitudeDegree = (float) (METERS_PER_DEGREE * Math.cos(lat));
        System.out.println("Meters per Degree: " + currentMetersPerLongitudeDegree);
    }

    private float getLongitudeMetersPerDegree() {
        return currentMetersPerLongitudeDegree;
    }

    /**
     * Returns an array Location.Location2D's descibing the bounds of the
     * region.
     * 
     * [0] - (minX, maxY) [1] - (maxX, maxY) (adjusted for lane displacement)
     * [2] - (minX, minY) (adjusted for lane displacement) [3] - (maxX, minY)
     * 
     * @return array of locations
     */
    public Location[] getBounds() {
        Location.Location2D corners[] = new Location.Location2D[4];
        Location topCorner = new Location.Location2D(minX, maxY);
        corners[0] = new Location.Location2D(minX - topCorner.getX(), topCorner.getY() - minY);
        corners[1] = new Location.Location2D(maxX - topCorner.getX() + 20, topCorner.getY() - minY + 20);
        corners[2] = new Location.Location2D(minX - topCorner.getX() - 20, topCorner.getY() - maxY - 20);
        corners[3] = new Location.Location2D(maxX - topCorner.getX(), topCorner.getY() - maxY);

        return corners;
    }

    /**
     * Returns the area of the test region.
     * 
     * @return area of the test region
     */
    public double getArea() {
        return intersections.area();
    }

    /**
     * This function loads the specified file into memory and extracts its road
     * segments. Assumes that files are stored in little-endian format.
     * 
     * @param filename
     *            the file containing segment data
     */
    private void loadSegmentsFile(String filename) throws StrawException {
        long length;
        int numRecs, saLeft, eaLeft, saRight, eaRight, streetIndex, shapeIndex;
        float startX, startY, endX, endY;
        char roadClass;
        Location.Location2D start, end;

        try {
            File f = new File(filename);
            FileInputStream fs = new FileInputStream(filename);

            FileChannel fc = fs.getChannel();

            // map the file into a byte buffer
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            // set byte order to be little-endian
            mbb.order(ByteOrder.LITTLE_ENDIAN);

            // get length of file and number of records
            length = f.length();
            numRecs = (int) (length / ROAD_SEGMENT_SIZE);

            int j = 0; // counter for number of streets

            // read all records from file
            for (int i = 0; i < numRecs; i++) {
                saLeft = mbb.getInt();
                eaLeft = mbb.getInt();
                saRight = mbb.getInt();
                eaRight = mbb.getInt();
                streetIndex = mbb.getInt();
                shapeIndex = mbb.getInt();
                // start point
                startX = (float) (getLongitudeMetersPerDegree() * mbb.getInt() / 1000000.0f);
                startY = (float) (METERS_PER_DEGREE * mbb.getInt() / 1000000.0f);
                start = new Location.Location2D(startX, startY);

                // end point
                endX = (float) (getLongitudeMetersPerDegree() * mbb.getInt() / 1000000.0f);
                endY = (float) (METERS_PER_DEGREE * mbb.getInt() / 1000000.0f);
                end = new Location.Location2D(endX, endY);

                roadClass = (char) mbb.get();
                if (roadClass < 11 || roadClass > 74) {
                    System.out.println("Unknown road class for road: " + roadClass);
                }

                mbb.position(mbb.position() + 3); // advance to next record

                // make sure this segment is within the specified bounds
                if (!start.inside(bl, tr) || !end.inside(bl, tr)) {
                    continue;
                } else {
                    if (start.distance(end) > 0) {
                        if (roadClass > 10 && roadClass < 70 && roadClass != 51 && roadClass != 64) {
                            // update the bounds for the region based on roads
                            // because it may be different than what the user
                            // specified
                            if (startX < minX)
                                minX = startX;
                            if (startX > maxX)
                                maxX = startX;
                            if (startY < minY)
                                minY = startY;
                            if (startY > maxY)
                                maxY = startY;
                            if (endX < minX)
                                minX = endX;
                            if (endX > maxX)
                                maxX = endX;
                            if (endY < minY)
                                minY = endY;
                            if (endY > maxY)
                                maxY = endY;

                            // create RoadSegment and store reference in vector
                            segments.add(new RoadSegment(saLeft, eaLeft, saRight, eaRight, streetIndex, shapeIndex,
                                    start, end, j, roadClass));

                            // mark the street name as used for future reference
                            usedStreets.put(new Integer(streetIndex), null);

                            // mark shape index as used, if there's a shape
                            if (shapeIndex != -1)
                                usedShapes.put(new Integer(shapeIndex), null);

                            j++; // increment index for RoadSegment index in
                                 // Vector
                        } // end if
                    } // end else
                } // end if length > 0
            } // end for

            fs.close(); // we are done with the file
        } // end try
        catch (FileNotFoundException e) {
            System.out
                    .println("StreetMobility::loadSegmentFile: Segments file does not exist at the specified location!");
            throw new StrawException("StreetMobility::loadSegmentFile: file " + filename + " not found!");

        } catch (IOException e) {
            System.out.println("StreetMobility::loadSegmentFile: I/O error");
            throw new StrawException("StreetMobility::loadSegmentFile: I/O error (file: " + filename + ")");
        }

    } // loadSegmentsFile

    /**
     * This function loads the street names into memory.
     * 
     * @param filename
     *            the file containing street names
     */
    private void loadStreetsFile(String filename) throws StrawException {
        long length = 0;
        int numRecs = 0;
        /* prefix, name, type and suffix sizes accoring to TIGER file format */
        char prefix[] = new char[2];
        char name[] = new char[30];
        char suffix[] = new char[4];
        char type[] = new char[2];
        int next = -1;
        int currentPos = 0;

        try {
            File f = new File(filename);
            FileInputStream fs = new FileInputStream(filename);
            DataInputStream ds = new DataInputStream(fs);

            // get length of file and number of records
            length = f.length();
            numRecs = (int) (length / STREET_NAME_SIZE);

            // get iterator for street list
            Iterator streetIt = usedStreets.keySet().iterator();

            // read all records from file
            while (streetIt.hasNext()) {
                // advance to next one
                currentPos = next;
                next = ((Integer) streetIt.next()).intValue();

                if (next > 0) {
                    ds.skip((next - currentPos - 1) * STREET_NAME_SIZE);
                }

                prefix[0] = (char) ds.readUnsignedByte();
                prefix[1] = (char) ds.readUnsignedByte();

                // street name
                for (int j = 0; j < name.length; j++) {
                    name[j] = (char) ds.readUnsignedByte();
                }

                // street suffix
                for (int j = 0; j < suffix.length; j++) {
                    suffix[j] = (char) ds.readUnsignedByte();
                }

                // street type
                for (int j = 0; j < type.length; j++) {
                    type[j] = (char) ds.readUnsignedByte();
                }

                streets.put(new Integer(next), new StreetName(new String(prefix), new String(name), new String(type),
                        new String(suffix)));

            }

            // we are done with the file
            ds.close();
            fs.close();
            usedStreets = null; // no longer needed
        } catch (FileNotFoundException e) {
            System.out.println("Streets file does not exist at the specified location!");
            throw new StrawException("StreetMobility::loadStreetsFile: file " + filename + " not found!");
        } catch (IOException e) {
            System.out.println("Length: " + numRecs + " next: " + next);
            System.out.println("StreetMobility::loadStreetFile: I/O error");
            throw new StrawException("StreetMobility::loadStreetsFile: I/O error (" + filename + ")");
        }

    } // loadStreetsFile

    /**
     * This function loads the shape data into memory. A segment has a shape if
     * it's a multiline segment.
     * 
     * @param filename
     *            the file containing shape data
     */
    private void loadShapesFile(String filename) throws StrawException {
        int numRecs, numPoints;
        double x, y;

        try {
            FileInputStream fs = new FileInputStream(filename);

            FileChannel fc = fs.getChannel();

            // map the file into a byte buffer
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            // set byte order to be little-endian
            mbb.order(ByteOrder.LITTLE_ENDIAN);

            // get number of records
            numRecs = mbb.getInt();

            // find indexes for used shapes for this region
            Iterator shapeIt = usedShapes.keySet().iterator();

            if (!shapeIt.hasNext()) {
                return;
            }

            int next = ((Integer) shapeIt.next()).intValue();

            // read all records from file
            for (int i = 0; i < numRecs; i++) {
                numPoints = mbb.getInt();
                if (i == next) {
                    Location.Location2D points[] = new Location.Location2D[numPoints];

                    // read each shape point
                    for (int j = 0; j < numPoints; j++) {
                        x = (getLongitudeMetersPerDegree() * mbb.getInt() / 1000000.0f);
                        y = (METERS_PER_DEGREE * mbb.getInt() / 1000000.0f);

                        // update the bounds for the region based on roads
                        // because it may be different than what the user
                        // specified
                        if (x < minX)
                            minX = (float) x;
                        if (x > maxX)
                            maxX = (float) x;
                        if (y < minY)
                            minY = (float) y;
                        if (y > maxY)
                            maxY = (float) y;

                        points[j] = new Location.Location2D((float) x, (float) y);
                    }

                    shapes.put(new Integer(i), new Shape(points));

                    if (!shapeIt.hasNext())
                        break;
                    next = ((Integer) shapeIt.next()).intValue();

                } else {
                    mbb.position(mbb.position() + 8 * numPoints); // 8 bytes per
                                                                  // point
                }
            }

            // we are done with the file
            fs.close();
            usedShapes = null; // no longer necessary

        } catch (FileNotFoundException e) {
            System.out.println("Shape file does not exist at the specified location!");
            throw new StrawException("StreetMobility::loadShapesFile: Shape file not found (" + filename + ")");
        } catch (IOException e) {
            System.out.println("loadShapesFile:I/O error");
            throw new StrawException("StreetMobility::loadShapesFile: I/O error (" + filename + ")");
        }

    } // loadShapesFile

    /**
     * Move along a straight line the specified distance.
     * 
     * @param start
     *            Point where motion begins.
     * @param end
     *            End point for line segement.
     * @param distance
     *            Distance (meters) to more along this segment.
     * @return Returns the location after moving distance units from start to
     *         end.
     */
    public Location move(Location start, Location end, float distance) {
        float hyp = end.distance(start);
        if (distance == hyp)
            return end;
        float portion = distance / hyp;

        // handles floating-point imprecision
        if (portion > 1.1f || portion < 0.0f) {
            if (distance < 0.01f && distance > 0.0f)
                return end;
            System.out.println("Length: " + hyp + " / Distance: " + distance);
            System.out.println("Distance/length: " + portion);
            System.out.println("Start: " + start + " End: " + end);
            throw new RuntimeException("StreetMobility:move: Move error!");
        }

        float dx = distance * (end.getX() - start.getX()) / hyp;
        float dy = distance * (end.getY() - start.getY()) / hyp;

        return new Location.Location2D(start.getX() + dx, start.getY() + dy);
    }

    /**
     * Prints the average speed for all nodes in the experiment.
     * 
     * @param seconds
     *            The duration of the simulation.
     * @return A String containing the average speed for all the vehicles.
     */
    public String printAverageSpeed(int seconds, boolean verbose) {
        Iterator it = mobInfo.iterator();
        StreetMobilityInfo smri;
        float total = 0;

        int i = 1;
        while (it.hasNext()) {
            smri = (StreetMobilityInfo) it.next();
            if (verbose)
                System.out.println("Average speed for node: " + i + ": " + smri.speedSum / seconds);
            total += smri.speedSum / seconds;

            if (verbose && smri.speedSum / seconds < 1) {
                printStreetList(i);
            }
            i++;
        }

        System.out.println("Average speed (overall): " + total / mobInfo.size());
        return total / mobInfo.size() + "";

    }

    /**
     * Prints the streets traversed by a vehicle.
     * 
     * @param i
     *            The id of the node to print streets for.
     */
    public void printStreetList(int i) {
        if (RECORD_STREETS) {
            System.out.println("Streets for node: " + i + ": ");
            StreetMobilityInfo smri = (StreetMobilityInfo) mobInfo.get(i - 1);

            Iterator it = smri.roads.iterator();
            RoadSegment rs;

            while (it.hasNext()) {
                rs = (RoadSegment) segments.get(((Integer) it.next()).intValue());
                System.out.println(rs.printStreetName(streets));
            }
        }
    }

    /**
     * Sets the next car for the node belonging to the SMI object.
     * 
     * @param smi
     *            The SMI object corresponding to the node changing streets.
     */
    public void setNextCar(StreetMobilityInfo smi) {
        if (smi.currentLane.size() > 1) {
            smi.nextCar = (StreetMobilityInfo) smi.currentLane.get(smi.currentLane.size() - 2);
        } else
            smi.nextCar = null;
    }

    /**
     * Updates the next road segment and next endpoint in the SMI object.
     * 
     * @param smi
     *            SMI object to update
     * @param rs
     *            new next road segment
     */
    public void updateNextRoadSegment(StreetMobilityInfo smi, RoadSegment newRS) {
        smi.nextRS = newRS;
        // update current road segment and end point for this node
        if (newRS.getStartPoint().distance(smi.rsEnd) < INTERSECTION_RESOLUTION)
            smi.nextEnd = newRS.getEndPoint();
        else if (newRS.getEndPoint().distance(smi.rsEnd) < INTERSECTION_RESOLUTION)
            smi.nextEnd = newRS.getStartPoint();
        else {
            System.out.println("Road Segment: " + newRS.printStreetName(streets));
            System.out.println("Current: " + smi.current.printStreetName(streets));
            System.out.println("Junction: " + smi.rsEnd);
            throw new RuntimeException("Bad intersection!");
        }

        if (RECORD_STREETS)
            smi.roads.add(new Integer(smi.nextRS.getSelfIndex()));
    }

    /**
     * This sets the next road for the vehicle belonging to the specified
     * StreetMobilityInfo object. This is to be called after moving to a new
     * road, so the SMI object's rsEnd and current fields must be set as such.
     * 
     * @param smi
     *            the StreetMobilityInfo object to update
     */
    public abstract void setNextRoad(StreetMobilityInfo smi);

    /**
     * Sets the Random object for use in the mobility model.
     * 
     * @param rnd
     *            The random object to set.
     */
    public void setRnd(Random rnd) {
        this.rnd = rnd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jist.swans.field.Mobility#setGUI(driver.Visualizer)
     */
    public void setGUI(VisualizerInterface visualizer) {
        v = visualizer;
    }

    public Vector getSegments() {
        return this.segments;
    }

    /**
     * Converts from meters that use lat/long as zero points to those that use
     * the upper left corner of the map as the (0,0) point.
     * 
     * @param initX
     *            the x coord using lat/long
     * @param initY
     *            the y coord using lat/long
     * @return a location2D object with the converted values
     */
    public Location convertFromStreets(Location old) {
        float initX = old.getX();
        float initY = old.getY();
        Location topCorner = new Location.Location2D(minX, maxY);
        return new Location.Location2D(initX - topCorner.getX(), topCorner.getY() - initY);
    }

    /**
     * Returns the number of streets for the specified region.
     * 
     * @return the number of streets
     */
    public int getNumberOfStreets() {
        return streets.size();
    }

    /**
     * 
     * @return the HashMap of RoadSegment shapes
     */
    public HashMap getShapes() {
        return shapes;

    }

    /**
     * @return the car to inspect
     */
    public int getCarToInspect() {

        return carToInspect;
    }

    /**
     * Sets the car to inspect
     * 
     * @param ip
     *            the ip of the car to inspect
     */
    public void setCarToInspect(int ip) {
        carToInspect = ip;
    }

    /**
     * Sets the car to inspect to a default value.
     */
    public void unsetCarToInspect() {
        carToInspect = -1;

    }

}
