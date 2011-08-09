/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         StartPoint.java
 * RCS:          $Id: StartPoint.java,v 1.1.1.1 2006/02/20 23:26:45 drchoffnes Exp $
 * Description:  StartPoint class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Mar 12, 2005
 * Language:     Java
 * Package:      jist.swans.misc
 * Status:       Release
 *
 * (C) Copyright 2005, Northwestern University, all rights reserved.
 *
 */
package vans.straw;

import jist.swans.misc.Location;

public final class StartPoint {
	Location start;

	/**
	 * @param start
	 * @param finish
	 */
	public StartPoint(Location start) {
		this.start = start;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		// this will resolve paths on chaining
		StartPoint other = (StartPoint) obj;
		return (other.start.distance(start) == 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		// this isn't great, but it should make the HashMap work decently
		return (new Float(start.getX() + start.getY())).hashCode();
	}
}