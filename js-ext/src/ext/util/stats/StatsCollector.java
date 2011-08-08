/*
 * Ulm University DUCKS project
 * 
 * Author:		Elmar Schoch <elmar.schoch@uni-ulm.de>
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
package ext.util.stats;

import ext.util.ExtendedProperties;

/**
 * Defines an interface that all classes need to implement if they want to use
 * the database result handling delivered by the DUCKS framework.
 * 
 * Note that this mechanism is only useful for collecting aggregated values at
 * the end of a simulation. To trace events during runtime of a simulation,
 * use the Eventlog mechanisms. 
 * 
 * @author Elmar Schoch
 *
 */
public interface StatsCollector {
	
	/**
	 * Must return a properties object that contains result parameters
	 * in the format defined by DUCKS (i.e. properties format)
	 * Also note, that the result parameters reflect the database
	 * structure (see DBManager for details).
	 * 
	 * @return
	 */
	public ExtendedProperties getStats();
	
	/**
	 * Must return an array of result parameter strings that will be contained
	 * when "getStats()" is called.
	 * 
	 * @return
	 */
	public String[] getStatParams();

}
