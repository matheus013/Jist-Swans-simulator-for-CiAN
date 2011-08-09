/*
 * Ulm University DUCKS project
 * 
 * Author: Stefan Schlott <stefan.schlott@uni-ulm.de>
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

package ducks.eventlog;

import java.util.Properties;

import jist.runtime.JistAPI.DoNotRewrite;
import jist.swans.field.Field;

/**
 * Interface for all event log modules.
 * 
 * @author Stefan Schlott
 * 
 */
public interface EventLogModule extends DoNotRewrite
{
    public void configure(Field field, Properties config, String configPrefix);

    public void enable();

    public void disable();
}
