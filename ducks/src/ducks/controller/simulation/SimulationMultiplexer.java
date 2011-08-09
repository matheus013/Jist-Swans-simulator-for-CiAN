/*
 * Ulm University DUCKS project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
 * 
 * (C) Copyright 2007, Ulm University, all rights reserved.
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
package ducks.controller.simulation;

import java.util.List;

import ducks.misc.DucksException;
import ext.util.ExtendedProperties;

public interface SimulationMultiplexer
{

    // namespaces in the config file that are excluded during multiplexing of
    // parameters
    // Note that this may be different from the parameters excluded from
    // saving. In particular, do not exclude any parameters that are required by
    // the
    // driver !
    public static final String[] CFG_EXCLUDE_PARAMS = new String[] { "ducks.config", "ducks.servers", "ducks.stats" };

    public List<ExtendedProperties> getSimulations(ExtendedProperties config) throws DucksException;

}
