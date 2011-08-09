/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StreetPlacementRandom.java
 * RCS:          $Id: StreetPlacementRandom.java,v 1.1.1.1 2006/02/20 23:26:43 drchoffnes Exp $
 * Description:  StreetPlacementRandom class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 10, 2004
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
import jist.swans.field.Placement;
import jist.swans.misc.Location;
import vans.straw.StreetMobilityOD.StreetMobilityInfoOD;
import vans.straw.StreetMobilityRandom.StreetMobilityInfoRandom;
import vans.straw.streets.RoadSegment;
import vans.straw.streets.SegmentNode;
import vans.straw.streets.Shape;
import vans.straw.streets.StreetName;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 *         The StreetPlacementRandom class places nodes at random streets in the
 *         current map.
 */
public class StreetPlacementRandom implements Placement {

	/** placement boundaries. */
	private float xBL, yBL, xTR, yTR;
	/** street mobility object, stores roads */
	private StreetMobility sm;
	/** set true for debugging information */
	final private boolean DEBUG = false;
	/** incremented each time this is called to determine node number */
	private int numberOfNodes = 0;
	/** the random object for repeatability */
	private java.util.Random rnd;
	/** the maximum distance in meters between first point and destination */
	float threshold = 1500.0f;
	/** the std dev for the distribution of speeds about the speed limit. */
	private double stdDev = 0.0;
	/** step time for street mobility */
	private double stepTime = 1;

	/**
	 * Initialize random placement model.
	 * 
	 * @param x
	 *            x-axis upper limit
	 * @param y
	 *            y-axis upper limit
	 * @param smr
	 *            the StreetMobility object
	 */
	public StreetPlacementRandom(float x, float y, StreetMobility smr) {
		init(0, 0, x, y, smr);
	}

	/**
	 * Initialize random placement model.
	 * 
	 * @param loc
	 *            Location describing limits of map
	 * @param smr
	 *            the StreetMobility object
	 */
	public StreetPlacementRandom(Location loc, StreetMobility smr) {
		init(0, 0, loc.getX(), loc.getY(), smr);
	}

	/**
	 * Initialize random placement.
	 * 
	 * @param br
	 *            bottom right corner
	 * @param tl
	 *            upper left corner
	 * @param smr
	 *            the StreetMobility object
	 */
	public StreetPlacementRandom(Location bl, Location tr, StreetMobility smr) {
		init(bl.getX(), bl.getY(), tr.getX(), tr.getY(), smr);
	}

	/**
	 * Initialize random placement.
	 * 
	 * @param br
	 *            bottom right corner
	 * @param tl
	 *            upper left corner
	 * @param smr
	 *            the StreetMobility object
	 * @param stdDev
	 *            the std dev for driver speeds
	 */
	public StreetPlacementRandom(Location bl, Location tr, StreetMobility smr,
			double stdDev) {
		init(bl.getX(), bl.getY(), tr.getX(), tr.getY(), smr);
		this.stdDev = stdDev;
	}

	/**
	 * Initialize random placement.
	 * 
	 * @param br
	 *            bottom right corner
	 * @param tl
	 *            upper left corner
	 * @param smr
	 *            the StreetMobility object
	 * @param stdDev
	 *            the std dev for driver speeds
	 * @param stepTime
	 *            the step time for street mobility
	 */
	public StreetPlacementRandom(Location bl, Location tr, StreetMobility smr,
			double stdDev, double stepTime) {
		init(bl.getX(), bl.getY(), tr.getX(), tr.getY(), smr);
		this.stdDev = stdDev;
		this.stepTime = stepTime;
	}

	/**
	 * Common initialization code.
	 * 
	 * @param xBL
	 *            bottom-left x position (in meters)
	 * @param yBL
	 *            bottom-left y position (in meters)
	 * @param xTR
	 *            top-right x position (in meters)
	 * @param yTR
	 *            top-right x position (in meters)
	 * @param smr
	 *            the StreetMobility object
	 */
	private void init(float xBL, float yBL, float xTR, float yTR,
			StreetMobility smr) {
		this.xBL = xBL;
		this.yBL = yBL;
		this.xTR = xTR;
		this.yTR = yTR;
		this.sm = smr;
		this.rnd = smr.rnd;
	}

	// ////////////////////////////////////////////////
	// Placement interface
	//

	/** {@inheritDoc} */
	public Location getNextLocation() {
		Location initialLocation;
		RoadSegment rs = null;
		int segmentID = 0, direction = 0, position = 0;
		boolean full = false;
		boolean valid = false; // did this find a valid street pair?
		float remainingDist;
		final int TO_START = 0;
		final int TO_END = 1;

		// determine mobility model
		StreetMobilityInfo smri;
		if (sm instanceof StreetMobilityRandom) {
			smri = new StreetMobilityInfoRandom();
		} else {
			smri = new StreetMobilityInfoOD();
		}

		while (!valid) // ensure non-isolated streets for OD mobility
		{
			// get random street
			do {
				segmentID = Constants.random.nextInt(sm.segments.size());
				direction = Constants.random.nextInt(2); // pick direction
				rs = (RoadSegment) sm.segments.elementAt(segmentID);

				// set the rsEnd for calculating path
				if (direction == TO_START)
					smri.rsEnd = rs.getStartPoint();
				else
					smri.rsEnd = rs.getEndPoint();
				// make sure lane isn't full
				if (rs.getFreeLane(smri.rsEnd) == null)
					full = true;
				else
					full = false;

			} while (((StreetName) sm.streets.get(new Integer(rs
					.getStreetIndex()))).getName().equals("") || full);

			// in the case of the SMOD model, some values have to be initialized
			if (sm instanceof StreetMobilityOD) {
				StreetMobilityOD smod = (StreetMobilityOD) sm;
				smri.current = rs;

				// pick random dest road segment
				RoadSegment dest = null;
				while (dest == null
						|| dest.getSelfIndex() == rs.getSelfIndex()
						|| dest.getEndPoint().distance(rs.getEndPoint()) > threshold) {
					// this should pick a relatively local location
					dest = (RoadSegment) smod.segments.get(rnd
							.nextInt(smod.segments.size()));
				}

				// calculate OD path
				valid = smod.calculatePath((StreetMobilityInfoOD) smri, rs,
						smri.rsEnd, dest);

				// only valid path may force car to be in other lane
				if (valid) {
					StreetMobilityInfoOD smiod = (StreetMobilityInfoOD) smri;
					SegmentNode sn = (SegmentNode) smiod.path.getFirst();
					RoadSegment rsTemp = (RoadSegment) sm.segments
							.get(sn.segmentID);

					// needs to be in opposite lane
					if (sn.segmentID == rs.getSelfIndex()
							&& sn.point.distance(smri.rsEnd) < StreetMobility.INTERSECTION_RESOLUTION)

					{
						if (smri.rsEnd.distance(rs.getEndPoint()) == 0) {
							smri.rsEnd = rs.getEndPoint();
							direction = TO_END;
						} else {
							smri.rsEnd = rs.getStartPoint();
							direction = TO_START;
						}
						smiod.path.remove(0);
					}

					// another case where it needs to be in opposite lane
					// diagnosis: rsEnd is not an endpoint in the next segment
					// needs to be in opposite lane
					if (rsTemp.getEndPoint().distance(smri.rsEnd) > StreetMobility.INTERSECTION_RESOLUTION
							&& rsTemp.getStartPoint().distance(smri.rsEnd) > StreetMobility.INTERSECTION_RESOLUTION) {
						if (smri.rsEnd.distance(rs.getEndPoint()) == 0) {
							smri.rsEnd = rs.getStartPoint();
							direction = TO_START;
							if (rs.getFreeLane(smri.rsEnd) == null)
								full = true;
						} else {
							smri.rsEnd = rs.getEndPoint();
							direction = TO_END;
							if (rs.getFreeLane(smri.rsEnd) == null)
								full = true;
						}

						if (full) {
							valid = false;
							continue;
						}
						if (DEBUG)
							System.out.println("Put node in opposite lane!");
					}
				}
			} else // not using OD pairs
			{
				valid = true;
			}
		} // while valid streets have not been picked

		numberOfNodes++; // increment number of nodes
		// update street mobility state
		smri.current = rs;
		smri.currSpeed = 0;
		smri.setMaxSpeed(rs.getSpeedLimit());
		smri.stepTime = (float) stepTime;

		if (direction == TO_START) {
			if (rs.getShapeIndex() >= 0) {
				smri.ShapePointIndex = ((Shape) sm.shapes.get(new Integer(rs
						.getShapeIndex()))).points.length;
			}
			// for calculating remaining distance
			position = rs.getCarsToStart().size();
		} else {
			smri.ShapePointIndex = -1;
			// for calculating remaining distance
			position = rs.getCarsToEnd().size();
		}

		remainingDist = rs.getCarSpacing(smri.currSpeed, smri.spacingBeta,
				smri.spacingGamma) * position;
		smri.remainingDist = remainingDist;

		smri.nextRS = rs;
		// add car to road
		smri.currentLane = rs.addNode(smri, smri.rsEnd, sm.mobInfo);
		if (smri.currentLane == null) {
			if (direction == TO_START) {
				System.out.println("Cars in lane: "
						+ rs.getCarsToStart().size());
				System.out.println("Max cars in lane: " + rs.getMaxCars());
			} else {
				System.out.println("Cars in lane: " + rs.getCarsToEnd().size());
				System.out.println("Max cars in lane: " + rs.getMaxCars());
			}
			throw new RuntimeException("Not enough room for car!");
		}

		sm.setNextRoad(smri);
		// set next car
		if (smri.currentLane.size() > 1) {
			smri.nextCar = (StreetMobilityInfo) smri.currentLane
					.get(smri.currentLane.size() - 2);
		}

		// set the amount by which drivers' speeds will vary from the speed
		// limit
		smri.extraSpeed = (float) (stdDev * rnd.nextGaussian());

		// add car to mobility info object
		sm.mobInfo.add(smri); // TODO this is NOT thread safe

		// now we have to move the vehicle back from the end of the road if
		// there is one or more cars ahead
		if (direction == TO_START) {
			// we have no intermediate segments
			if (smri.current.getShapeIndex() == -1) {
				initialLocation = sm.move(rs.getEndPoint(), smri.rsEnd,
						rs.getLength() - remainingDist);
			} else {
				initialLocation = sm.pointAt(rs.getEndPoint(), smri,
						rs.getLength() - remainingDist);
			}
		} else {
			// we have no intermediate segments
			if (smri.current.getShapeIndex() == -1) {
				initialLocation = sm.move(rs.getStartPoint(), smri.rsEnd,
						rs.getLength() - remainingDist);
			} else {
				initialLocation = sm.pointAt(rs.getStartPoint(), smri,
						rs.getLength() - remainingDist);
			}
		}

		// System.out.print("\rPlaced node " + sm.mobInfo.size());

		if (StreetMobility.ENABLE_LANE_DISPLACEMENT) {
			smri.offset = sm.getLaneDisplacement(smri);
			// initialLocation = initialLocation.getClone();
			// initialLocation.add(smri.offset); // displace due to lane
			initialLocation = initialLocation.add(smri.offset); // displace due
																// to lane
		}

		return initialLocation;
	}

}// class: StreetPlacementRandom
