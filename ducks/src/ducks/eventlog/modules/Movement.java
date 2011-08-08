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
package ducks.eventlog.modules;

import java.util.Properties;

import jist.swans.field.Field;
import jist.swans.field.MovementListenerInterface;
import jist.swans.misc.Location;
import ducks.eventlog.EventLog;
import ducks.eventlog.EventLogModule;

/**
 * Register a movement listener and send movement data to logging system.
 * 
 * @author Stefan Schlott
 *
 */
public class Movement implements EventLogModule, MovementListenerInterface {
	Field field;

	public void configure(Field field, Properties config, String configPrefix) {
		this.field = field;
	}

	public void enable() {
		field.addMovementListener(this);
	}

	public void disable() {
		field.removeMovementListener(this);
	}

	public void move(long time, Location loc, int node) {
		EventLog.log(node,time,loc,"move",null);
	}
}
