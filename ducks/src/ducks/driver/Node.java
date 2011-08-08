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

import jist.swans.field.Placement;
import jist.swans.misc.Mapper;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import vans.straw.VisualizerInterface;
import ext.util.ExtendedProperties;

/**
 * The Node interface is required for classes that implement a node in the
 * ad hoc network, at least if they shall be used within the DUCKS generic
 * driver model.
 * Note that this is not necessarily required if the next higher level class 
 * (here: GenericNodes) is implemented differently
 *  
 * @author Elmar Schoch
 *
 */
public interface Node extends DucksDriverModule {

	public void setIdentifier(int id);
	
	public void setScene(Scene scene);
	public void setGlobalConfig(ExtendedProperties config);
	
	public void setCommonObjects(Placement place, RadioInfo.RadioInfoShared radioInfo, 
			Mapper protMap, PacketLoss inLoss, PacketLoss outLoss, VisualizerInterface v);
	
}
