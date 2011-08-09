/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         RoadSegment.java
 * RCS:          $Id: RoadSegment.java,v 1.2 2006/02/22 00:24:44 drchoffnes Exp $
 * Description:  RoadSegment class (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Dec 7, 2004
 * Language:     Java
 * Package:      jist.swans.field.streets
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
package vans.straw.streets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import jist.swans.misc.Location;
import vans.straw.StreetMobility;
import vans.straw.StreetMobilityInfo;

/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 *         The RoadSegment class contains information about a piece of road
 *         between two intersections.
 */
public class RoadSegment {

	int startAddressLeft;
	int endAddressLeft;
	int startAddressRight;
	int endAddressRight;
	int streetIndex;
	int shapeIndex;
	int selfIndex; // this segment's index in the array of segments
	Location startPoint;
	Location endPoint;
	char roadClass; // the two numbers after 'A'

	// variables for runtime functions
	/** number of vehicles on segment */
	int numberOfCars = 0;
	/** number of lanes in each direction in segment */
	int numberOfLanes;
	/** length of road segment */
	public float length;
	/** maximum number of cars allowed in each lane for segment */
	int maxCars;
	/**
	 * average vehicle length in meters, from
	 * http://www.ite.org/traffic/documents/AB00H1903.pdf
	 */
	public static final int CAR_LENGTH = 5;
	/** stationary space between vehicles in meters */
	public static final int SPACE = 1;

	/** contains queue of cars on road heading toward endPoint */
	private ArrayList<LinkedList<StreetMobilityInfo>> lanesToEnd;
	/** contains queue of cars on road heading toward startPoint */
	private ArrayList<LinkedList<StreetMobilityInfo>> lanesToStart;
	/** contains list of inter lane connections */
	public ArrayList<Map<LinkedList<StreetMobilityInfo>, LaneConnector>> laneConnectionsToEnd;
	public ArrayList<Map<LinkedList<StreetMobilityInfo>, LaneConnector>> laneConnectionsToStart;

	public float intersectionDistance = 0.0f;

	// private LinkedList<StreetMobilityInfo> carsToEnd[];
	// private LinkedList<StreetMobilityInfo> carsToStart[];

	/**
     * 
     */
	public RoadSegment() {
		startPoint = null;
		endPoint = null;
	}

	/**
	 * @param startAddressLeft
	 * @param endAddressLeft
	 * @param startAddressRight
	 * @param endAddressRight
	 * @param streetIndex
	 * @param shapeIndex
	 * @param startPoint
	 * @param endPoint
	 * @param roadClass
	 */
	public RoadSegment(int startAddressLeft, int endAddressLeft,
			int startAddressRight, int endAddressRight, int streetIndex,
			int shapeIndex, Location startPoint, Location endPoint,
			int selfIndex, char roadClass) {
		super();
		this.startAddressLeft = startAddressLeft;
		this.endAddressLeft = endAddressLeft;
		this.startAddressRight = startAddressRight;
		this.endAddressRight = endAddressRight;
		this.streetIndex = streetIndex;
		this.shapeIndex = shapeIndex;
		this.startPoint = startPoint;
		this.selfIndex = selfIndex;
		this.endPoint = endPoint;
		this.roadClass = roadClass;

		// set number of lanes per segment
		// Primary Road with limited access/ Interstate Highway - unseparated
		if (roadClass >= 11 && roadClass <= 14)
			numberOfLanes = 3;

		// Primary Road with limited access/ Interstate Highway - separated
		else if (roadClass >= 15 && roadClass <= 18)
			numberOfLanes = 3;

		// Primary Road without limited access/ US Highway - unseparated
		else if (roadClass >= 21 && roadClass <= 24)
			numberOfLanes = 3;

		// Primary Road without limited access / US Highway - separated
		else if (roadClass >= 25 && roadClass <= 28)
			numberOfLanes = 3;

		// Secondary and Connecting Roads / State Highways - unseparated
		else if (roadClass >= 31 && roadClass <= 34)
			numberOfLanes = 2;

		// Secondary and Connecting Roads / State Highways - separated
		else if (roadClass >= 35 && roadClass <= 38)
			numberOfLanes = 2;

		// Local, Rural, Neighborhood / City Street - unseparated
		else if (roadClass >= 41 && roadClass <= 44)
			numberOfLanes = 1;

		// Local, Rural, Neighborhood / City Street - separated
		else if (roadClass >= 45 && roadClass <= 48)
			numberOfLanes = 2;
		// access ramp
		else if (roadClass >= 62 && roadClass <= 63)
			numberOfLanes = 1;
		else {
			System.err.println("Unknown road class " + (int) roadClass
					+ " encountered\n");
			numberOfLanes = 1;
		}

		lanesToEnd = new ArrayList<LinkedList<StreetMobilityInfo>>(
				numberOfLanes);
		lanesToStart = new ArrayList<LinkedList<StreetMobilityInfo>>(
				numberOfLanes);
		for (int i = 0; i < numberOfLanes; i++) {
			lanesToEnd.add(new LinkedList<StreetMobilityInfo>());
			lanesToStart.add(new LinkedList<StreetMobilityInfo>());
		}

		laneConnectionsToEnd = new ArrayList<Map<LinkedList<StreetMobilityInfo>, LaneConnector>>(
				numberOfLanes);
		laneConnectionsToStart = new ArrayList<Map<LinkedList<StreetMobilityInfo>, LaneConnector>>(
				numberOfLanes);
		for (int i = 0; i < numberOfLanes; i++) {
			laneConnectionsToEnd
					.add(new HashMap<LinkedList<StreetMobilityInfo>, LaneConnector>());
			laneConnectionsToStart
					.add(new HashMap<LinkedList<StreetMobilityInfo>, LaneConnector>());
		}
	}

	/**
	 * Returns a reference to the lane that the car was added to. The lane
	 * chosen is the one with the fewest cars.
	 * 
	 * @param smi
	 *            id of node added to list... must use 1-based number
	 * @param rsEnd
	 *            The point at the end of the segment in the direction of motion
	 * @param nodes
	 *            a Vector of SMI objects
	 * @return
	 */
	public LinkedList<StreetMobilityInfo> addNode(StreetMobilityInfo smi,
			Location rsEnd, Vector nodes) {
		LinkedList<StreetMobilityInfo> freeLane;

		if (rsEnd.distance(this.endPoint) == 0) {
			freeLane = getCarsToEnd();
		} else {
			freeLane = getCarsToStart();
		}

		int position = -1;
		if (freeLane.size() > 0) {
			// ensure that the car has room to move to next road
			StreetMobilityInfo last = ((StreetMobilityInfo) freeLane.getLast());
			if (last.getRemainingDist() > length - CAR_LENGTH - SPACE) {
				return null;
			}
			if (smi.nextEnd == null) // new car on map
			{
				if (freeLane.size() == 1) {
					if (((StreetMobilityInfo) freeLane.getFirst())
							.getRemainingDist() > 4 * CAR_LENGTH) {
						position = 0;
					}
				} else {
					ListIterator li = freeLane.listIterator();
					StreetMobilityInfo one, two;
					while (li.hasNext())
						li.next();
					one = (StreetMobilityInfo) li.previous();
					while (li.hasPrevious()) {
						two = (StreetMobilityInfo) li.previous();
						if (one.getRemainingDist() - two.getRemainingDist() > CAR_LENGTH * 4) {
							position = li.nextIndex() + 1;
						}
						one = two;
					}

				}
			}
		}
		if (position == -1)
			freeLane.addLast(smi);
		else {
			freeLane.add(position, smi);
		}
		return freeLane;
	}

	public LinkedList<StreetMobilityInfo> getFreeLane(Location rsEnd) {
		LinkedList<StreetMobilityInfo> freeLane;
		if (rsEnd.distance(this.endPoint) == 0) {
			freeLane = getCarsToEnd();
		} else {
			if (rsEnd.distance(this.startPoint) == 0) {
				freeLane = getCarsToStart();
			} else {
				throw new RuntimeException("Placed car on wrong street!");
			}
		}
		if (freeLane == null) {
			return null;
		}
		if (freeLane.size() > 0
				&& freeLane.getLast().getRemainingDist() > length - CAR_LENGTH
						- SPACE) {
			return null;
		}
		return freeLane;
	}

	/**
	 * Determines if the cars in the proper order in a lane
	 * 
	 * @param list
	 */
	public void checkLane(LinkedList<StreetMobilityInfo> list) {
		ListIterator li = list.listIterator();
		StreetMobilityInfo front = (StreetMobilityInfo) li.next(), behind;
		while (li.hasNext()) {
			behind = (StreetMobilityInfo) li.next();
			if (behind.getCurrentRS() != this) {
				throw new RuntimeException("Road added to wrong street!");
			}
			if (behind.getNextCar() != front
					|| front.getRemainingDist() >= behind.getRemainingDist()) {
				System.err.println("Road: " + selfIndex + " front: " + front.id
						+ " rd: " + front.getRemainingDist() + " behind: "
						+ behind.id + " rd: " + behind.getRemainingDist()
						+ " car before behind: " + behind.getNextCar().id);
				throw new RuntimeException("Improperly added node!");
			}

			front = behind;
		}
	}

	/**
	 * This function returns the speed limit (meters/second) of a road based on
	 * its class as specified in the CFCC field of TIGER files.
	 */
	public float getSpeedLimit() {
		// Primary Road with limited access/ Interstate Highway - unseparated
		if (roadClass >= 11 && roadClass <= 14)
			return 31.2928f;

		// Primary Road with limited access/ Interstate Highway - separated
		else if (roadClass >= 15 && roadClass <= 18)
			return 35.7632f;

		// Primary Road without limited access/ US Highway - unseparated
		else if (roadClass >= 21 && roadClass <= 24)
			return 20.1168f;

		// Primary Road without limited access / US Highway - separated
		else if (roadClass >= 25 && roadClass <= 28)
			return 22.352f;

		// Secondary and Connecting Roads / State Highways - unseparated
		else if (roadClass >= 31 && roadClass <= 34)
			return 20.1168f;

		// Secondary and Connecting Roads / State Highways - separated
		else if (roadClass >= 35 && roadClass <= 38)
			return 22.352f;

		// Local, Rural, Neighborhood / City Street - unseparated
		else if (roadClass >= 41 && roadClass <= 44)
			return 11.176f;

		// Local, Rural, Neighborhood / City Street - separated
		else if (roadClass >= 45 && roadClass <= 48)
			return 13.4112f;
		// access ramp
		else if (roadClass >= 62 && roadClass <= 63)
			return 13.4112f;
		else
			System.err.println("Unknown road class " + (int) roadClass
					+ " encountered\n");
		return 11.0f;
	}

	/**
	 * Returns the distance along a road segment
	 * 
	 * @param point
	 * @return
	 */
	public float getDistance(Shape sh) {
		int numPoints;
		float d = 0;

		// handle straight line
		if (sh == null)
			return endPoint.distance(startPoint);

		// handle segment with shape points
		else {

			numPoints = sh.points.length;

			d = startPoint.distance(sh.points[0]);
			for (int i = 0; i < numPoints - 1; i++) {
				d += sh.points[i].distance(sh.points[i + 1]);
			}
			d += sh.points[numPoints - 1].distance(endPoint);
			return d;
		}

	}

	/**
	 * @return Returns the endAddressLeft.
	 */
	public int getEndAddressLeft() {
		return endAddressLeft;
	}

	/**
	 * @param endAddressLeft
	 *            The endAddressLeft to set.
	 */
	public void setEndAddressLeft(int endAddressLeft) {
		this.endAddressLeft = endAddressLeft;
	}

	/**
	 * @return Returns the endAddressRight.
	 */
	public int getEndAddressRight() {
		return endAddressRight;
	}

	/**
	 * @param endAddressRight
	 *            The endAddressRight to set.
	 */
	public void setEndAddressRight(int endAddressRight) {
		this.endAddressRight = endAddressRight;
	}

	/**
	 * @return Returns the endPoint.
	 */
	public Location getEndPoint() {
		return endPoint;
	}

	/**
	 * @param endPoint
	 *            The endPoint to set.
	 */
	public void setEndPoint(Location endPoint) {
		this.endPoint = endPoint;
	}

	/**
	 * @return Returns the roadClass.
	 */
	public char getRoadClass() {
		return roadClass;
	}

	/**
	 * @param roadClass
	 *            The roadClass to set.
	 */
	public void setRoadClass(char roadClass) {
		this.roadClass = roadClass;
	}

	/**
	 * @return Returns the shapeIndex.
	 */
	public int getShapeIndex() {
		return shapeIndex;
	}

	/**
	 * @param shapeIndex
	 *            The shapeIndex to set.
	 */
	public void setShapeIndex(int shapeIndex) {
		this.shapeIndex = shapeIndex;
	}

	/**
	 * @return Returns the startAddressLeft.
	 */
	public int getStartAddressLeft() {
		return startAddressLeft;
	}

	/**
	 * @param startAddressLeft
	 *            The startAddressLeft to set.
	 */
	public void setStartAddressLeft(int startAddressLeft) {
		this.startAddressLeft = startAddressLeft;
	}

	/**
	 * @return Returns the startAddressRight.
	 */
	public int getStartAddressRight() {
		return startAddressRight;
	}

	/**
	 * @param startAddressRight
	 *            The startAddressRight to set.
	 */
	public void setStartAddressRight(int startAddressRight) {
		this.startAddressRight = startAddressRight;
	}

	/**
	 * @return Returns the startPoint.
	 */
	public Location getStartPoint() {
		return startPoint;
	}

	/**
	 * @param startPoint
	 *            The startPoint to set.
	 */
	public void setStartPoint(Location startPoint) {
		this.startPoint = startPoint;
	}

	/**
	 * @return Returns the streetIndex.
	 */
	public int getStreetIndex() {
		return streetIndex;
	}

	/**
	 * @param streetIndex
	 *            The streetIndex to set.
	 */
	public void setStreetIndex(int streetIndex) {
		this.streetIndex = streetIndex;
	}

	/**
	 * @return Returns the selfIndex.
	 */
	public int getSelfIndex() {
		return selfIndex;
	}

	/**
	 * @param selfIndex
	 *            The selfIndex to set.
	 */
	public void setSelfIndex(int selfIndex) {
		this.selfIndex = selfIndex;
	}

	/**
	 * @return Returns the length of the road segment.
	 */
	public float getLength() {
		return length;
	}

	/**
	 * Sets segment length and maximum number of cars in segment
	 * 
	 * @param sh
	 *            The shape describing the segment.
	 */
	public void setLength(Shape sh) {
		this.length = getDistance(sh);
		if (StreetMobility.ENABLE_INTERSECTION_TRAFFIC) {
			intersectionDistance = (float) StrictMath.min(numberOfLanes * 2.0f
					* StreetMobility.LANE_WIDTH, length / 3.0);
		}
		length -= 2.0f * intersectionDistance;
		this.maxCars = (int) StrictMath.max(
				(StrictMath.floor(length / (CAR_LENGTH + SPACE))), 1);
	}

	/**
	 * @return Returns the numberOfCars.
	 */
	public int getNumberOfCars() {
		return numberOfCars;
	}

	/**
	 * @param numberOfCars
	 *            The numberOfCars to set.
	 */
	public void setNumberOfCars(int numberOfCars) {
		this.numberOfCars = numberOfCars;
	}

	/**
	 * @return Returns the maxCars for a direction.
	 */
	public int getMaxCars() {
		return maxCars;
	}

	/**
	 * @param maxCars
	 *            The maxCars to set.
	 */
	public void setMaxCars(int maxCars) {
		this.maxCars = maxCars;
	}

	/**
	 * @return Returns the numberOfLanes.
	 */
	public int getNumberOfLanes() {
		return numberOfLanes;
	}

	/**
	 * @param numberOfLanes
	 *            The numberOfLanes to set.
	 */
	public void setNumberOfLanes(int numberOfLanes) {
		this.numberOfLanes = numberOfLanes;
	}

	/**
	 * @return Returns the lane with the fewest number of cars in this
	 *         direction. Prefer lanes on the right side (small indices).
	 */
	public LinkedList<StreetMobilityInfo> getCarsToEnd() {
		float space = -1.0f;
		LinkedList<StreetMobilityInfo> rlane = null;

		if (lanesToEnd.size() == 0) {
			throw new RuntimeException("Road " + selfIndex + " has no lanes");
		}
		Iterator<LinkedList<StreetMobilityInfo>> it_lanes = lanesToEnd
				.listIterator();
		while (it_lanes.hasNext()) {
			LinkedList<StreetMobilityInfo> lane = it_lanes.next();
			if (lane.size() == 0) {
				return lane;
			}
			if (lane.size() > 0 && lane.getLast().getRemainingDist() > space) {
				rlane = lane;
				space = lane.getLast().getRemainingDist();
			}
		}
		return rlane;
	}

	/**
	 * @return Returns the lane wit hthe fewest number of cars in this
	 *         direction. Prefer lanes on the right side (small indices).
	 */
	public LinkedList<StreetMobilityInfo> getCarsToStart() {
		float space = -1.0f;
		LinkedList<StreetMobilityInfo> rlane = null;

		if (lanesToStart.size() == 0) {
			throw new RuntimeException("Road " + selfIndex + " has no lanes");
		}
		Iterator<LinkedList<StreetMobilityInfo>> it_lanes = lanesToStart
				.listIterator();
		while (it_lanes.hasNext()) {
			LinkedList<StreetMobilityInfo> lane = it_lanes.next();
			if (lane.size() == 0) {
				return lane;
			}
			if (lane.size() > 0 && lane.getLast().getRemainingDist() > space) {
				rlane = lane;
				space = lane.getLast().getRemainingDist();
			}
		}
		return rlane;
	}

	/**
	 * @return Returns the space between the front of a car and the front of the
	 *         car behind it (when stopped).
	 */
	public float getCarSpacing(float speed, float beta, float gamma) {
		return (CAR_LENGTH + SPACE) + speed * beta + speed * speed * gamma;
	}

	/**
	 * 
	 * @param id
	 *            node number to remove... must use 1-based number
	 * @param rsEnd
	 * @param mobInfo
	 */
	public void removeNode(StreetMobilityInfo smi,
			LinkedList<StreetMobilityInfo> currentLane, Vector mobInfo) {

		if (currentLane.size() > 0) {
			// make sure it's at the front of the queue
			if (((StreetMobilityInfo) currentLane.getFirst()) == smi) {
				currentLane.removeFirst();
			} else {
				System.err.println("Tried to remove: " + mobInfo.indexOf(smi));

				System.err.println("CarList: "
						+ printCarList(currentLane, mobInfo));

				throw new RuntimeException("Removed node that wasn't"
						+ " at front of queue");
			}
			return;
		} else // empty list
		{
			System.err.println("Tried to remove: " + mobInfo.indexOf(smi)
					+ " from segment " + selfIndex);

			printCarList(currentLane, mobInfo);

			throw new RuntimeException("Tried to remove from " + "empty list!");
		}

	}

	public String printCarList(LinkedList<StreetMobilityInfo> currentLane,
			Vector mobInfo) {
		Iterator it = currentLane.iterator();
		String s = "";
		StreetMobilityInfo smri;
		while (it.hasNext()) {
			smri = (StreetMobilityInfo) it.next();

			s += mobInfo.indexOf(smri) + " - remaining: "
					+ smri.getRemainingDist() + " - next: " + smri.getNextCar()
					+ "\n";
		}
		return s;
	}

	/**
	 * @return
	 */
	public String printStreetName(HashMap streets) {

		return "("
				+ startAddressLeft
				+ "/"
				+ startAddressRight
				+ ") - ("
				+ "("
				+ endAddressLeft
				+ "/"
				+ endAddressRight
				+ ")"
				+ ((StreetName) streets.get(new Integer(streetIndex)))
						.toString() + " [" + selfIndex + "] " + startPoint
				+ " - " + endPoint;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return ((RoadSegment) obj).selfIndex == this.selfIndex;
	}

	public int getLane(LinkedList<StreetMobilityInfo> currentLane) {
		if (lanesToEnd.contains(currentLane)) {
			return lanesToEnd.indexOf(currentLane);
		}
		return lanesToStart.indexOf(currentLane);
	}

	/**
	 * Returns the number of directions for this road
	 * 
	 * @return
	 */
	public int getNumberOfDirections() {
		// TODO update when support for one-way streets is included in
		// map data.
		return 2;
	}

	/**
	 * Returns the stroke width for painting a this road segment.
	 */
	public char getStrokeWidth() {
		// Primary Road with limited access/ Interstate Highway - unseparated
		if (roadClass >= 11 && roadClass <= 14)
			return 7;

		// Primary Road with limited access/ Interstate Highway - separated
		else if (roadClass >= 15 && roadClass <= 18)
			return 7;

		// Primary Road without limited access/ US Highway - unseparated
		else if (roadClass >= 21 && roadClass <= 24)
			return 6;

		// Primary Road without limited access / US Highway - separated
		else if (roadClass >= 25 && roadClass <= 28)
			return 6;

		// Secondary and Connecting Roads / State Highways - unseparated
		else if (roadClass >= 31 && roadClass <= 34)
			return 5;

		// Secondary and Connecting Roads / State Highways - separated
		else if (roadClass >= 35 && roadClass <= 38)
			return 5;

		// Local, Rural, Neighborhood / City Street - unseparated
		else if (roadClass >= 41 && roadClass <= 44)
			return 3;

		// Local, Rural, Neighborhood / City Street - separated
		else if (roadClass >= 45 && roadClass <= 48)
			return 3;
		// access ramp
		else if (roadClass >= 62 && roadClass <= 63)
			return 3;
		else
			System.err.println("Unknown road class " + (int) roadClass
					+ " encountered\n");
		return 3;
	}

	/**
	 * Returns a curve, which connects
	 * 
	 * @param smi
	 * @param nextLane
	 * @param startShape
	 * @param endShape
	 * @return
	 */
	public LaneConnector getLaneConnector(StreetMobilityInfo smi,
			LinkedList<StreetMobilityInfo> nextLane, Shape startShape,
			Shape endShape) {
		// test in which direction the car drives
		if (startPoint.distance(smi.getRSEnd()) > endPoint.distance(smi
				.getRSEnd())) {
			int laneNumber = smi.getLane();
			if (!laneConnectionsToEnd.get(laneNumber).containsKey(nextLane)) {
				laneConnectionsToEnd.get(laneNumber).put(
						nextLane,
						new LaneConnector(smi, smi.getNextRS()
								.getLane(nextLane), startShape, endShape));
			}
			return laneConnectionsToEnd.get(laneNumber).get(nextLane);
		} else {
			int laneNumber = smi.getLane();
			if (!laneConnectionsToStart.get(laneNumber).containsKey(nextLane)) {
				laneConnectionsToStart.get(laneNumber).put(
						nextLane,
						new LaneConnector(smi, smi.getNextRS()
								.getLane(nextLane), startShape, endShape));
			}
			return laneConnectionsToStart.get(laneNumber).get(nextLane);
		}
	}
}
