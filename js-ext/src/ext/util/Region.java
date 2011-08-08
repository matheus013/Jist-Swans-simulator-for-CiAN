/*
 * Ulm University JiST/SWANS Extension Project
 * 
 * Author:		Michael Feiri <michael.feiri@uni-ulm.de>
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
package ext.util;

import jist.runtime.JistAPI;
import jist.swans.misc.Location;
import java.awt.geom.Line2D;

/** 
 * Immutable Regions based on jist.swans.misc.Location.
 *
 * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
 */

public abstract class Region implements JistAPI.Timeless {

  /**
   * Determine whether a given location is inside region.
   *
   * @param l location to test
   * @return whether location is within region
   */
  public abstract boolean contains(Location l);   

  /**
   * Compute distance from a given location to this region.
   * Implementations are free to base this on a centroid or
   * any other object insider the region. But do not use a
   * regions border to compute distance since a packet
   * might not reach the interior of a region if the distance
   * is measured against the border
   *
   * @param l compute distance to this location
   * @return distance between given location and this region
   */
  public abstract float distance(Location l);
  
  /**
   * The size of the dataset in bytes. Round up if necessary.
   * 
   * @return the size of the dataset in bytes
   */
  public abstract int getSize();
  
  
  public static class LineBuffer extends Region {
    private final Line2D spline;
    private final int SIZE;
    private final float h;
    
    public LineBuffer(Location.Location2D a, Location.Location2D b, float height) {
      this.h = height;
      this.SIZE = a.size()+b.size()+4; // two Location objects and a float
      spline = new Line2D.Float(a.getX(), a.getY(), b.getX(), b.getY());
    }
    
    /** {@inheritDoc} */
    public String toString() {
        return "("+spline+","+h+")";
    }

    /** {@inheritDoc} */
    public int getSize() {
        return SIZE;
    }

    /** {@inheritDoc} */
    public boolean contains(Location l) {
    	return (distance(l)<h);
    }

    /** {@inheritDoc} */
	public float distance(Location l) {
		return (float)spline.ptSegDist(l.getX(), l.getY());
	}

    /** {@inheritDoc} */
    public int hashCode() {
      int result = 17;
      result = result * 31 + spline.hashCode();
      result = result * 31 + Float.floatToIntBits(h);
      return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object o) {
      if (o == this) return true; 
      if (!(o instanceof Region.LineBuffer)) return false; 
      Region.LineBuffer lb = (Region.LineBuffer)o; 
      return (spline.equals(lb.spline)) && (h==lb.h);
    }

  }
  
  
  public static class Rectangle extends Region {
    private final Location.Location2D c;
    private final int SIZE;
    private final float h;
    private final boolean cw;
    private final Line2D.Float spline;
    
    public Rectangle(Location.Location2D a, Location.Location2D b, float height, boolean clockwise) {

       	// two Location objects a float (the boolean could be stuffed in the floats sign bit) 
        this.SIZE = a.size()+b.size()+4;
       	
        this.h = height;
        this.cw = clockwise;
        this.spline = new Line2D.Float(a.getX(), a.getY(), b.getX(), b.getY());
        
        // construct the centroid
        int Xorient = clockwise ? -1 : 1;
        int Yorient = clockwise ?  1 :-1;
        float scaleFactor = h/(a.distance(b));
        c = new Location.Location2D((float)(a.getX()+((b.getX()-a.getX())*0.5)+
        							(Yorient*((b.getY()-a.getY())*scaleFactor)*0.5)),
    	         	  				(float)(a.getY()+((b.getY()-a.getY())*0.5)+
    	         	  				(Xorient*((b.getX()-a.getX())*scaleFactor)*0.5)));
    }

    /** {@inheritDoc} */
    public String toString() {
        return "("+spline+"," + h + "," + cw + ")";
    }

    /** {@inheritDoc} */
    public int getSize() {
        return SIZE;
    }
    
    public boolean contains(Location l) {
    	
    	// idea based on comp.graphics.algorithms FAQ: "distance from a point to a line"
    	double r = ((l.getX()-spline.x1) * (spline.x2-spline.x1) +
				    (l.getY()-spline.y1) * (spline.y2-spline.y1))/
			      ((spline.x2-spline.x1) * (spline.x2-spline.x1) +
			       (spline.y2-spline.y1) * (spline.y2-spline.y1));
    	
    	if ( (r >= 0) && (r <= 1) ) { // perpendicular projection works

    		// now check orientation and see if point is within height
    		int orient = spline.relativeCCW(l.getX(), l.getY());
    		if ((orient==1) && (cw)) { //clockwise
        		return (spline.ptLineDist(l.getX(), l.getY()) <= h);
    		} else if ((orient==-1) && (!cw)) { //counterclockwise
        		return (spline.ptLineDist(l.getX(), l.getY()) <= h);
    		} else {
    			return true;// orient==0 ---> l==a or l==b
    		}

    	}
    	return false;

    }

	public float distance(Location l) {
		return c.distance(l);
	}
		
    public int hashCode() {
      int result = 17;
      result = result * 31 + spline.hashCode();
      result = result * 31 + Float.floatToIntBits(h*(cw ? 1 : -1));
      return result;
    }

    public boolean equals(Object o) {
      if (o == this) return true; 
      if (!(o instanceof Region.Rectangle)) return false; 
      Region.Rectangle pn = (Region.Rectangle)o; 
      return (spline.equals(pn.spline)) && (h==pn.h) && (cw==pn.cw);
    }

  }
  
  
  public static class Circle extends Region {
    private final Location.Location2D a;
    private final float r;
    private final int SIZE;
    
    public Circle(Location.Location2D center, float radius) {
      this.a = center;
      this.r = radius;
      this.SIZE = a.size()+4; // one Location object and a float
    }
    
    public String toString() {
        return "("+a+","+r+")";
    }

    /** {@inheritDoc} */
    public int getSize() {
        return SIZE;
    }
    
    // Chek if this Region containes the give Location
    public boolean contains(Location l) {
    	return (this.distance(l)<r);
    }
      
    public int hashCode() {
      int result = 17;
      result = result * 31 + a.hashCode();
      result = result * 31 + Float.floatToIntBits(r);
      return result;
    }

    public boolean equals(Object o) {
      if (o == this) return true; 
      if (!(o instanceof Region.Circle)) return false; 
      Region.Circle pn = (Region.Circle)o; 
      return (a.equals(pn.a)) && (pn.r == r);
    }
    
	public float distance(Location l) {
		return a.distance(l);
	}

  }
  
}
