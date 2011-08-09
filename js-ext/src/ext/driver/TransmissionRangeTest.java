/*
 * Ulm University JiST/SWANS project
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
package ext.driver;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.app.AppHeartbeat;
import jist.swans.field.Fading;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.PathLoss;
import jist.swans.field.Spatial;
import jist.swans.mac.Mac802_11;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIpBase;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioNoiseIndep;

public class TransmissionRangeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.print("Creating simulation nodes... ");
		Field f = createSim();
		System.out.println("done.");

		System.out.println("Average density = " + f.computeDensity() * 1000
				* 1000 + "/km^2");
		System.out.println("Average sensing = "
				+ f.computeAvgConnectivity(true));
		System.out.println("Average receive = "
				+ f.computeAvgConnectivity(false));

		JistAPI.endAt(20 * Constants.SECOND);
	}

	public static void createNode(int i, int nodes, Field field, Location loc,
			RadioInfo.RadioInfoShared radioInfoShared, Mapper protMap,
			PacketLoss plIn, PacketLoss plOut) {

		// create entities
		RadioNoiseIndep radio = new RadioNoiseIndep(i, radioInfoShared);
		Mac802_11 mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo());

		NetIpBase net = new NetIp(new NetAddress(i), protMap, plIn, plOut);

		// AppCbr app = new AppCbr(1,100,5,0,1,i,nodes,100);
		AppHeartbeat app = new AppHeartbeat(i, true);

		// hookup entities
		field.addRadio(radio.getRadioInfo(), radio.getProxy(), loc);
		// field.startMobility(radio.getRadioInfo().getUnique().getID());
		radio.setFieldEntity(field.getProxy());
		radio.setMacEntity(mac.getProxy());
		mac.setRadioEntity(radio.getProxy());
		byte intId = net.addInterface(mac.getProxy());
		mac.setNetEntity(net.getProxy(), intId);
		net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT,
				app.getNetProxy());
		app.setNetEntity(net.getProxy());
		app.getAppProxy().run(null);
	}

	public static Field createSim() {

		Location.Location2D bounds = new Location.Location2D(1000, 200);
		int nodes = 2;

		Mobility mobility = new Mobility.Static();

		Spatial spatial = new Spatial.HierGrid(bounds, 5);
		Fading fading = new Fading.None();
		// PathLoss pathloss = new PathLoss.FreeSpace();
		PathLoss pathloss = new PathLoss.TwoRay();

		Field field = new Field(spatial, fading, pathloss, mobility,
				Constants.PROPAGATION_LIMIT_DEFAULT);

		// adaption of transmission power to achieve different transmission
		// ranges
		// default: 15 dbm => transmission range ~ 627m (pathloss=freespace)
		// default: 15 dbm => transmission range ~ 376m (pathloss=tworay)
		double txPower = Constants.TRANSMIT_DEFAULT;
		// txPower = 7.1 // => transmission range ~ 250m (pathloss=freespace)
		// txPower = 7.9 // => transmission range ~ 250m (pathloss=tworay)
		txPower = 7.9;

		RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
				Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
				txPower, Constants.GAIN_DEFAULT,
				Util.fromDB(Constants.SENSITIVITY_DEFAULT),
				Util.fromDB(Constants.THRESHOLD_DEFAULT),
				Constants.TEMPERATURE_DEFAULT,
				Constants.TEMPERATURE_FACTOR_DEFAULT,
				Constants.AMBIENT_NOISE_DEFAULT);

		Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
		protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT);
		PacketLoss pl = new PacketLoss.Zero();

		// Create nodes
		Location.Location2D loc0 = new Location.Location2D(100, 100);
		Location.Location2D loc1 = new Location.Location2D(350, 100);

		createNode(0, nodes, field, loc0, radioInfoShared, protMap, pl, pl);
		createNode(1, nodes, field, loc1, radioInfoShared, protMap, pl, pl);

		return field;
	}

}
