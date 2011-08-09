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

import ext.util.ExtendedProperties;

/**
 * The Nodes interface is required to be implemented by a class that shall
 * represent the nodes collection of the simulation, at least if the higher
 * level class (GenericDriver) is to be used. If the GenericDriver is not used
 * as provided by Ducks, the own implementation may of course process nodes
 * totally different.
 * 
 * @author Elmar Schoch
 * 
 */
public interface Nodes extends DucksDriverModule {

	public void setScene(Scene scene);

	public void setGlobalConfig(ExtendedProperties config);
}
