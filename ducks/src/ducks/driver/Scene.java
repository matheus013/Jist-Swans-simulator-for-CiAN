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
package ducks.driver;

import jist.swans.field.Field;
import ext.util.ExtendedProperties;

/**
 * The Scene interface needs to be implemented by a class which wants to replace
 * the GenericScene implementation within the DUCKS driver model. If the
 * GenericDriver is not to be used, this is of course not required.
 * 
 * @author Elmar Schoch
 * 
 */
public interface Scene extends DucksDriverModule {

	public void setGlobalConfig(ExtendedProperties config);

	public Field getField();

}
