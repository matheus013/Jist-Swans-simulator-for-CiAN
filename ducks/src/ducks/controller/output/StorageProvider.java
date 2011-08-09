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
package ducks.controller.output;

import ducks.controller.DucksControllerModule;
import ducks.controller.simulation.Simulation;
import ducks.controller.simulation.SimulationStudy;
import ducks.misc.DucksException;

public interface StorageProvider extends DucksControllerModule
{

    // namespaces to be excluded by default from being saved.
    // Note that this may be different from the parameters excluded from
    // multiplexing in ducks.controller.simulation.SimulationMultiplexer

    public static final String[] CFG_EXCLUDE_PARAMS = new String[] { "ducks.config", "ducks.servers", "ducks.stats",
            "ducks.eventlog"                       };

    public void saveSimulationStudy(SimulationStudy study) throws DucksException;

    public void saveSimulation(Simulation simu) throws DucksException;
}
