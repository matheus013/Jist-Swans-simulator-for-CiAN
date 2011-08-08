/*
 * Ulm University DUCKS project
 * 
 * Author:		Stefan Schlott <stefan.schlott@uni-ulm.de>
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
package ducks.eventlog.destinations;

import jist.swans.misc.Location;

import org.apache.log4j.Logger;

import ducks.eventlog.EventLog;

/**
 * Output event data to Log4J
 * 
 * @author Stefan Schlott
 *
 */
public class Log4J extends EventLog {
	protected static final Logger logger = Logger.getLogger(EventLog.class.getName());

	public void logEvent(int node, long time, Location loc, String type, String comment) {
		String result = "";
		if (node>=0)
			result = "Node " + node + " ";
		if (time>=0)
			result += "@" + time + " ";
		if (loc!=null)
			result += " " + loc.getX() + "/" + loc.getY() + " ";
		if (type!=null)
			result += "["+type+"] ";
		if (comment!=null)
			result += comment;
		logger.info(result);
	}
}
