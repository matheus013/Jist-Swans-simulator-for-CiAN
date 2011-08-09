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

package ducks.misc;

/**
 * Exception type used in the Ducks system.
 * 
 * @author Elmar Schoch
 */
public class DucksException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DucksException() {
		super();
	}

	public DucksException(String reason) {
		super(reason);
	}

}
