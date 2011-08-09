/**
 * C3 - Car to Car Cooperation - Project
 *
 * File:         VisualizerInterface.java
 * RCS:          $Id: VisualizerInterface.java,v 1.23 2005/07/13 17:44:03 drc915 Exp $
 * Description:  VisualizerInterface interface (see below)
 * Author:       David Choffnes
 *               Aqualab (aqualab.cs.northwestern.edu)
 *               Northwestern Systems Research Group
 *               Northwestern University
 * Created:      Nov 17, 2004
 * Language:     Java
 * Package:      driver
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

import java.awt.Color;

import jist.swans.misc.Location;
import vans.straw.streets.RoadSegment;

/**
 * 
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 * 
 *         The VisualizerInterface interface allows code to compile, but doesn't
 *         give you any of our visualization capability. We will release the
 *         actual Visualizer class when the time is right.
 */
public interface VisualizerInterface {

	/**
	 * Adds a node to the (visual) field.
	 * 
	 * @param initX
	 *            initial x position
	 * @param initY
	 *            initial y position
	 * @param ip
	 *            the node inditifier
	 */
	public abstract void addNode(float initX, float initY, int ip);

	/**
	 * Updates a node's location.
	 * 
	 * @param newX
	 *            new x coordinate
	 * @param newY
	 *            new y coordinate
	 * @param ip
	 *            node identifier
	 */
	public abstract void updateNodeLocation(float newX, float newY, int ip);

	/**
	 * Draws a cirlce around a node.
	 * 
	 * @param ip
	 *            the node to draw a cirlce around
	 */
	public abstract void drawTransmitCircle(int ip);

	/**
	 * Hides the circle.
	 * 
	 * @param ip
	 *            the node to stop showing the cirlce for
	 */
	public abstract void hideTransmitCircle(int ip);

	/**
	 * Displaces the specified node by the specified amount
	 * 
	 * @param step
	 *            the amount to add to the current location
	 * @param ip
	 *            the ip address of the node to move
	 */
	public abstract void displaceNode(Location step, int ip);

	/**
	 * Sets the node color to something new.
	 * 
	 * @param i
	 *            the node to set
	 * @param c
	 *            the color to set
	 */
	public abstract void setNodeColor(int i, Color c);

	/**
	 * Sets the tooltip text for the specified node.
	 * 
	 * @param ip
	 * @param text
	 */
	public abstract void setToolTip(int ip, String text);

	/**
	 * Sets the editor text for the specified node.
	 * 
	 * @param ip
	 *            the node indentifer
	 * @param text
	 *            the text to set
	 */
	public abstract void setRoutingPaneText(int ip, String text);

	/**
	 * Sets the general text for the panel.
	 * 
	 * @param text
	 */
	public abstract void setGeneralPaneText(String text);

	/**
	 * Sets the max base distance that a message will propagate (i.e., within 1
	 * std dev).
	 * 
	 * @param maxDistance
	 */
	public abstract void setBaseTranmit(double maxDistance);

	/**
	 * Resets node colors to the default colors.
	 */
	public abstract void resetColors();

	/**
	 * Draws a circle of radius r at location loc.
	 * 
	 * @param r
	 *            radius of circle
	 * @param loc
	 *            center of circle
	 */
	public abstract void drawCircle(int r, Location loc);

	/**
	 * Removes circle.
	 */
	public abstract void removeCircle();

	/**
	 * Colors a RoadSegment.
	 * 
	 * @param rs
	 *            the road segment to color
	 * @param c
	 *            the color to use
	 */
	public abstract void colorSegment(RoadSegment rs, Color c);

	/**
	 * Colors an array of road segments.
	 * 
	 * @param objects
	 *            an array of RoadSegments
	 * @param colors
	 *            an array of corresponding colors
	 */
	public abstract void colorSegments(Object[] objects, Color colors[]);

	/**
	 * Sets the time for the visualization. Should not be called by only method
	 * on a call stack that has already called JistAPI.sleep().
	 * 
	 * @param time
	 *            the time, in nanoseconds.
	 */
	public abstract void updateTime(long time);

	/**
	 * Draws an animated circle around the specified node
	 * 
	 * @param ip
	 *            the ip address of the node at center
	 * @param color
	 *            the color of the cirlce drawn
	 */
	public abstract void drawAnimatedTransmitCircle(int ip, Color color);
}