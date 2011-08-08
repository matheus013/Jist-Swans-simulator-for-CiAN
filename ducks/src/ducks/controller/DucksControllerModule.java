/*
 * Ulm University DUCKS project
 * 
 * Author:		Elmar Schoch <elmar.schoch@uni-ulm.de>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 */
package ducks.controller;

import ducks.misc.DucksException;
import ext.util.Configurable;

public interface DucksControllerModule extends Configurable {

	/**
	 * Enable the module. This method should activate operation of the module. This may be
	 * opening a connection, a file, starting threads or whatever.
	 *  
	 * @throws DucksException
	 */
	public void enable() throws DucksException;
	
	/**
	 * Disable the module. This method should stop all operations of the module, e.g. by closing
	 * open handles like files, and clean up the state of the module
	 * 
	 * @throws DuckExeption
	 */
	public void disable() throws DucksException;
	
}
