/*
 * Ulm University JiST/SWANS project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package ext.jist.runtime;

import jist.runtime.JistAPI;
import jist.swans.Constants;

/**
 * JistUtils collects a number of useful things regarding JiST.
 * 
 * @author eschoch
 * 
 */
public class JistUtils
{

    /**
     * Get simulation time in seconds formatting
     * 
     * @return time string
     */
    public static String getSimulationTime() {
        long t = JistAPI.getTime();
        return (t / Constants.SECOND) + "." + (t % Constants.SECOND);
    }
}
