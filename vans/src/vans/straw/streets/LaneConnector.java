package vans.straw.streets;

import java.util.ArrayList;
import java.util.Iterator;

import jist.swans.misc.Location;
import vans.straw.StreetMobility;
import vans.straw.StreetMobilityInfo;

public class LaneConnector {
	/* resolution of curve rendering */
	static final float resolution = 1.0f;
	private boolean debug = false;
	/* length of the connection */
	float length = 0.0f;
	/* list of points */
	ArrayList<curvePoint> curvePoints;

	/**
	 * creates a connection between two lanes on a intersection based on a cubic
	 * bezier curve the lanes with the smalles index is positioned on the right
	 * 
	 * @param smi
	 *            the vehicle
	 * @param endLane
	 *            the index of the lane the vehicle will take
	 * @param startShape
	 *            the shape of the current road segment
	 * @param endShape
	 *            the shape of the next road segment
	 */

	LaneConnector(StreetMobilityInfo smi, int endLane, Shape startShape,
			Shape endShape) {
		Location p1, p2, p3, p4, startPoint, endPoint;
		float length, xoffset, yoffset;

		/*
		 * calculation of the four points for the bezier curve: The first is the
		 * beginning of the intersection, the second is the end of the current
		 * road segment, the third is the beginning of the next road segment and
		 * the fourth the beginning of the next lane. Due to the width of the
		 * intersection depends on the numer of lanes of the road segment, the
		 * distance between the first and the second point is exactly the width
		 * of the intersection of the current road segment, the distance between
		 * the third and the fourth point is as well the width of the
		 * intersection of the next road segment. The distance between the
		 * second and the third point depends on the displacement of the lanes
		 * and the angle between the two road segments If the road segments have
		 * some additional points, which discribe their shape, they are
		 * considered. In the case of a right turn, the sections defined by the
		 * first two points and the second two points intersect. So the second
		 * and the third point of the curve are replaced by the
		 */
		if (debug)
			System.out.println("startLane: " + smi.getLane() + " endLane: "
					+ endLane);
		if (debug)
			System.out.println("startRS "
					+ smi.getCurrentRS().startPoint.toString() + " "
					+ smi.getCurrentRS().endPoint.toString() + " "
					+ smi.getCurrentRS().intersectionDistance + " endRS "
					+ smi.getNextRS().startPoint.toString() + " "
					+ smi.getNextRS().endPoint.toString() + " "
					+ smi.getNextRS().intersectionDistance + " rsEnd "
					+ smi.getRSEnd().toString());

		// find out start and endpoint of the current road segment
		if (smi.getCurrentRS().endPoint.distance(smi.getRSEnd()) < smi
				.getCurrentRS().startPoint.distance(smi.getRSEnd())) {
			if (smi.getCurrentRS().shapeIndex < 0) {
				startPoint = smi.getCurrentRS().startPoint;
			} else {
				// the shape points have to be considered
				startPoint = startShape.points[startShape.points.length - 1];
			}
			endPoint = smi.getCurrentRS().endPoint;
		} else {
			if (smi.getCurrentRS().shapeIndex < 0) {
				startPoint = smi.getCurrentRS().endPoint;
			} else {
				// the shape points have to be considered
				startPoint = startShape.points[0];
			}
			endPoint = smi.getCurrentRS().startPoint;
		}
		if (debug)
			System.out.println("startp " + startPoint.toString() + " "
					+ endPoint.toString() + " "
					+ smi.getCurrentRS().intersectionDistance + " endRS "
					+ smi.getNextRS().startPoint.toString() + " "
					+ startPoint.distance(endPoint));

		// calculate the lane displacement
		length = startPoint.distance(endPoint);
		xoffset = (startPoint.getY() - endPoint.getY())
				/ length
				* StreetMobility.LANE_WIDTH
				* (smi.getCurrentRS().getNumberOfLanes() - 1.0f - smi.getLane() + 0.5f);
		yoffset = -(startPoint.getX() - endPoint.getX())
				/ length
				* StreetMobility.LANE_WIDTH
				* (smi.getCurrentRS().getNumberOfLanes() - 1.0f - smi.getLane() + 0.5f);
		if (debug)
			System.out
					.println("startRS offset : "
							+ xoffset
							+ " "
							+ yoffset
							+ " lane "
							+ smi.getLane()
							+ " lanes "
							+ smi.getCurrentRS().numberOfLanes
							+ " lane width "
							+ StreetMobility.LANE_WIDTH
							+ " start: "
							+ startPoint.toString()
							+ " end: "
							+ endPoint.toString()
							+ " dist: "
							+ length
							+ " disp: "
							+ (StreetMobility.LANE_WIDTH * (smi.getCurrentRS()
									.getNumberOfLanes() - 1.0f - smi.getLane() + 0.5f)));

		// calculate the first two points
		p1 = new Location.Location2D(endPoint.getX()
				+ (startPoint.getX() - endPoint.getX())
				* smi.getCurrentRS().intersectionDistance / length + xoffset,
				endPoint.getY() + (startPoint.getY() - endPoint.getY())
						* smi.getCurrentRS().intersectionDistance / length
						+ yoffset);
		p2 = new Location.Location2D(endPoint.getX() + xoffset, endPoint.getY()
				+ yoffset);

		// find out start and endpoint of the next road segment
		if (smi.getNextRS().endPoint.distance(smi.getRSEnd()) < smi.getNextRS().startPoint
				.distance(smi.getRSEnd())) {
			startPoint = smi.getNextRS().endPoint;
			if (smi.getNextRS().shapeIndex < 0) {
				endPoint = smi.getNextRS().startPoint;
			} else {
				// the shape points have to be considered
				endPoint = endShape.points[endShape.points.length - 1];
			}
		} else {
			startPoint = smi.getNextRS().startPoint;
			if (smi.getNextRS().shapeIndex < 0) {
				endPoint = smi.getNextRS().endPoint;
			} else {
				// the shape points have to be considered
				endPoint = endShape.points[0];
			}
		}
		if (debug)
			System.out.println("startp " + startPoint.toString() + " "
					+ endPoint.toString() + " "
					+ smi.getCurrentRS().intersectionDistance + " endRS "
					+ smi.getNextRS().startPoint.toString() + " "
					+ startPoint.distance(endPoint));

		// calculate the lane displacement
		length = startPoint.distance(endPoint);
		xoffset = -(endPoint.getY() - startPoint.getY()) / length
				* StreetMobility.LANE_WIDTH
				* (smi.getNextRS().getNumberOfLanes() - 1.0f - endLane + 0.5f);
		yoffset = (endPoint.getX() - startPoint.getX()) / length
				* StreetMobility.LANE_WIDTH
				* (smi.getNextRS().getNumberOfLanes() - 1.0f - endLane + 0.5f);
		if (debug)
			System.out
					.println("endRS offset: "
							+ xoffset
							+ " "
							+ yoffset
							+ " lane "
							+ endLane
							+ " lanes "
							+ smi.getNextRS().numberOfLanes
							+ " lane width "
							+ StreetMobility.LANE_WIDTH
							+ " start: "
							+ startPoint.toString()
							+ " end: "
							+ endPoint.toString()
							+ " dist: "
							+ length
							+ " disp: "
							+ (StreetMobility.LANE_WIDTH * (smi.getNextRS()
									.getNumberOfLanes() - 1.0f - smi.getLane() + 0.5f)));

		// calculate the second two points
		p4 = new Location.Location2D(startPoint.getX()
				+ (endPoint.getX() - startPoint.getX())
				* smi.getNextRS().intersectionDistance / length + xoffset,
				startPoint.getY() + (endPoint.getY() - startPoint.getY())
						* smi.getNextRS().intersectionDistance / length
						+ yoffset);
		p3 = new Location.Location2D(startPoint.getX() + xoffset,
				startPoint.getY() + yoffset);

		if (debug)
			System.out.println("p1-p2 " + p1.distance(p2) + " p2-p3 "
					+ p2.distance(p3) + " p3-p4 " + p3.distance(p4));
		if (debug)
			System.out.println("new LaneConnector created " + p1.toString()
					+ " " + p2.toString() + " " + p3.toString() + " "
					+ p4.toString());
		if (RightTurn(p1, p2, p3, p4)) {
			// in case of a right turn, recalculate the second and third point
			// otherwise, vehicle would drive a loop
			if (debug)
				System.out.println("intersection: right turn");
			Location isp = calcIntersection(p1, p2, p3, p4);
			double fak = 2.0 - Math.sqrt(2.0);
			p2 = new Location.Location2D(
					(float) (p1.getX() + (isp.getX() - p1.getX()) * fak),
					(float) (p1.getY() + (isp.getY() - p1.getY()) * fak));
			p3 = new Location.Location2D(
					(float) (p4.getX() + (isp.getX() - p4.getX()) * fak),
					(float) (p4.getY() + (isp.getY() - p4.getY()) * fak));
			if (debug)
				System.out.println("p1-p2 " + p1.distance(p2) + " p2-p3 "
						+ p2.distance(p3) + " p3-p4 " + p3.distance(p4));
			if (debug)
				System.out.println("new LaneConnector created " + p1.toString()
						+ " " + p2.toString() + " " + p3.toString() + " "
						+ p4.toString());
		}
		if (debug
				&& (p1.distance(p2) > 100.0f || p2.distance(p3) > 100.0f || p3
						.distance(p4) > 100.0f)) {
			System.out.println("extreme wide intersection: " + p1.toString()
					+ " " + p2.toString() + " " + p3.toString() + " "
					+ p4.toString());
			System.out.println("startLane: " + smi.getLane() + " endLane: "
					+ endLane);
			System.out.println("startRS "
					+ smi.getCurrentRS().startPoint.toString() + " "
					+ smi.getCurrentRS().endPoint.toString() + " "
					+ smi.getCurrentRS().intersectionDistance + " endRS "
					+ smi.getNextRS().startPoint.toString() + " "
					+ smi.getNextRS().endPoint.toString() + " "
					+ smi.getNextRS().intersectionDistance + " rsEnd "
					+ smi.getRSEnd().toString());
		}

		// calculate the points in between
		curvePoints = new ArrayList<curvePoint>();
		createPoints(curvePoints, p1, p2, p3, p4);
		curvePoints.add(new curvePoint(p4, 0.0f));
	}

	/**
	 * @return the length
	 */
	public float getLength() {
		return length;
	}

	/**
	 * calculates a point on the bezier curve, if necessary calls itself
	 * recursevly
	 * 
	 * @param curvePoints
	 *            an array of curve points in which a new point will be added to
	 * @param p1
	 *            Bezier point
	 * @param p2
	 *            Bezier point
	 * @param p3
	 *            Bezier point
	 * @param p4
	 *            Bezier point
	 */

	private void createPoints(ArrayList<curvePoint> curvePoints, Location p1,
			Location p2, Location p3, Location p4) {
		if (p1.distance(p4) <= resolution) {
			if (debug)
				System.out.println("new point on intersection " + p1.toString()
						+ " distance to next point " + p1.distance(p4));
			curvePoints.add(new curvePoint(p1, p1.distance(p4)));
			length += p1.distance(p4);
			return;
		}
		Location np1 = new Location.Location2D((p1.getX() + p2.getX()) / 2.0f,
				(p1.getY() + p2.getY()) / 2.0f);
		Location np2 = new Location.Location2D((p2.getX() + p3.getX()) / 2.0f,
				(p2.getY() + p3.getY()) / 2.0f);
		Location np3 = new Location.Location2D((p3.getX() + p4.getX()) / 2.0f,
				(p3.getY() + p4.getY()) / 2.0f);
		Location np4 = new Location.Location2D(
				(np1.getX() + np2.getX()) / 2.0f,
				(np1.getY() + np2.getY()) / 2.0f);
		Location np5 = new Location.Location2D(
				(np2.getX() + np3.getX()) / 2.0f,
				(np2.getY() + np3.getY()) / 2.0f);
		Location np6 = new Location.Location2D(
				(np4.getX() + np5.getX()) / 2.0f,
				(np4.getY() + np5.getY()) / 2.0f);

		createPoints(curvePoints, p1, np1, np4, np6);
		createPoints(curvePoints, np6, np5, np3, p4);
	}

	/**
	 * calculates the intersection point of the lines described by the given
	 * four points
	 * 
	 * @param p1
	 *            Bezier point
	 * @param p2
	 *            Bezier point
	 * @param q1
	 *            Bezier point
	 * @param q2
	 *            Bezier point
	 * @return point of the intersection
	 */

	private Location calcIntersection(Location p1, Location p2, Location q1,
			Location q2) {
		double dpx = p2.getX() - p1.getX();
		double dpy = p2.getY() - p1.getY();
		double dqx = q2.getX() - q1.getX();
		double dqy = q2.getY() - q1.getY();
		double n = dqx * dpy - dqy * dpx;
		if (n < 0.1) {
			// given lines are (nearly) parallel, point in the middle of p2 and
			// q1 is returned
			if (debug)
				System.out.println("CalcIntersection: n=0 " + p1.toString()
						+ " " + p2.toString() + " " + q1.toString() + " "
						+ q2.toString());
			return new Location.Location2D(p2.getX() + (q1.getX() - p2.getX())
					/ 2.0f, p2.getY() + (q1.getY() - p2.getY()) / 2.0f);
		}
		double l = (q1.getY() * dpx - p1.getY() * dpx - q1.getX() * dpy + p1
				.getX() * dpy)
				/ n;
		if (debug)
			System.out.println("CalcIntersection: n: " + n + " l: " + l
					+ " dpx " + dpx + " dpy " + dpy + " dqx " + dqx + " dqy "
					+ dqy);
		return new Location.Location2D((float) (q1.getX() + l * dqx),
				(float) (q1.getY() + l * dqy));
	}

	/**
	 * determins if the turn goes to the right the movement is decribed by two
	 * lines, the result depends on the angle between these two lines
	 * 
	 * @param p1
	 *            first point on the first line
	 * @param p2
	 *            second point on the first line
	 * @param q1
	 *            first point on the second line
	 * @param q2
	 *            second point on the second line
	 * @return true for a right turn
	 */

	private boolean RightTurn(Location p1, Location p2, Location q1, Location q2) {
		double dpy = -p2.getX() + p1.getX();
		double dpx = p2.getY() - p1.getY();
		double dqx = q2.getX() - q1.getX();
		double dqy = q2.getY() - q1.getY();

		double s = dpx * dqx + dpy * dqy;
		return s < 0;
	}

	/**
	 * calculates a point on the connection the lines between the Bezier points
	 * are interpolated
	 * 
	 * @param distance
	 *            the distance from the beginning of the intersection to the
	 *            calculated point
	 * @return point on the bezier curve; null if distance > length of Bezier
	 *         curve
	 */

	public Location getPointOnConnection(float distance) {
		double d = 0;
		Iterator it_curvePoints = curvePoints.iterator();
		while (it_curvePoints.hasNext()) {
			curvePoint cp = (curvePoint) it_curvePoints.next();
			if (d + cp.distance >= distance) {
				double dd = distance - d;
				if (debug)
					System.out.println("getPointonConnection d: " + d
							+ " distance to next point " + cp.distance
							+ " distance on intersection " + distance + " dd: "
							+ dd);
				curvePoint cp2 = (curvePoint) it_curvePoints.next();
				return new Location.Location2D(
						(float) (cp.point.getX() + (cp2.point.getX() - cp.point.getX())
								* dd / cp.distance),
						(float) (cp.point.getY() + (cp2.point.getY() - cp.point
								.getY()) * dd / cp.distance));
			}
			d += cp.distance;
		}
		return null;
	}
}

class curvePoint {
	/* location of this point */
	Location point;
	/* distance to next curvePoint */
	double distance;

	curvePoint(Location point, double distance) {
		this.point = point;
		this.distance = distance;
	}
}