//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <heartbeat.java Tue 2004/04/06 11:57:52 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

// with extensions by Ulm University (Elmar Schoch)

package ext.driver;

import java.util.ArrayList;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Fading;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.PathLoss;
import jist.swans.field.Placement;
import jist.swans.field.Spatial;
import jist.swans.mac.Mac802_11;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacDumb;
import jist.swans.mac.MacInterface;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseAdditive;
import jist.swans.radio.RadioNoiseIndep;
import ext.jist.swans.app.AppHeartbeat;
import ext.jist.swans.mobility.MobilityReaderNs2;
import ext.jist.swans.mobility.MobilityReplay;
import ext.jist.swans.mobility.PlacementReplay;

/**
 * SWANS demo/test: heartbeat application.
 */

public class heartbeat {

	/** random waypoint pause time. */
	public static final int PAUSE_TIME = 30;
	/** random waypoint granularity. */
	public static final int GRANULARITY = 10;
	/** random waypoint minimum speed. */
	public static final int MIN_SPEED = 2;
	/** random waypoint maximum speed. */
	public static final int MAX_SPEED = 10;

	public static final int RADIO_TYPE = 2; // RadioNoiseAdditive
	public static final int MAC_TYPE = 1; // MacDumb
	public static final int PHY_TYPE = 2; // 802.11p radio

	public static final int MOBILITY = 2; // Replay
	// public static final String mobilityFile =
	// "ducks/resources/mobility/simfiles/2lpd_2-2npkm_scen1/combined-108_nodes-120_tsteps-2_2_npkm-2_lpd-0.tcl";
	// public static final String mobilityFile =
	// "ducks/resources/mobility/simfiles/2lpd_2-2npkm_scen2/combined-105_nodes-120_tsteps-2_2_npkm-2_lpd-0.tcl";
	// public static final String mobilityFile =
	// "ducks/resources/mobility/simfiles/2lpd_6-6npkm_scen1/combined-340_nodes-120_tsteps-6_6_npkm-2_lpd-0.tcl";
	// public static final String mobilityFile =
	// "ducks/resources/mobility/simfiles/2lpd_15-15npkm_scen1/combined-843_nodes-120_tsteps-15_15_npkm-2_lpd-0.tcl";
	public static final String mobilityFile = "ducks/resources/mobility/simfiles/2lpd_2-6npkm_scen1/combined-236_nodes-120_tsteps-2_6_npkm-2_lpd-0.tcl";

	// Elmar Schoch added >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	private static ArrayList<AppHeartbeat> appList = new ArrayList<AppHeartbeat>();

	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

	/**
	 * Initialize simulation node.
	 * 
	 * @param i
	 *            node number
	 * @param field
	 *            simulation field
	 * @param placement
	 *            node placement model
	 * @param radioInfoShared
	 *            shared radio information
	 * @param protMap
	 *            shared protocol map
	 * @param plIn
	 *            incoming packet loss model
	 * @param plOut
	 *            outgoing packet loss model
	 * @param radioNoiseType
	 *            2=RadioNoiseAdditive, 1=RadioNoiseIndep
	 * @param macType
	 *            2=Mac802.11 1=MacDumb
	 */
	public static void createNode(int i, Field field, Placement placement,
			RadioInfo.RadioInfoShared radioInfoShared, Mapper protMap,
			PacketLoss plIn, PacketLoss plOut, int radioNoiseType, int macType) {
		// create entities
		RadioNoise radio;
		switch (radioNoiseType) {
		case 2:
			radio = new RadioNoiseAdditive(i, radioInfoShared);
			break;
		case 1:
		default:
			radio = new RadioNoiseIndep(i, radioInfoShared);
		}

		MacInterface mac, macProxy;
		switch (macType) {
		case 2:
			mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo());
			macProxy = ((Mac802_11) mac).getProxy();
			break;
		case 1:
		default:
			mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());
			macProxy = ((MacDumb) mac).getProxy();
		}

		NetIp net = new NetIp(new NetAddress(i), protMap, plIn, plOut);
		AppHeartbeat app = new AppHeartbeat(i, true);

		// Elmar Schoch added >>>>>>>>>>>>>>>>>>>>>>>>>>>
		heartbeat.appList.add(app);
		// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

		// hookup entities
		field.addRadio(radio.getRadioInfo(), radio.getProxy(),
				placement.getNextLocation());
		field.startMobility(radio.getRadioInfo().getUnique().getID());
		radio.setFieldEntity(field.getProxy());
		radio.setMacEntity(macProxy);
		byte intId = net.addInterface(macProxy);

		if (mac instanceof Mac802_11) {
			((Mac802_11) mac).setRadioEntity(radio.getProxy());
			((Mac802_11) mac).setNetEntity(net.getProxy(), intId);
		}
		if (mac instanceof MacDumb) {
			((MacDumb) mac).setRadioEntity(radio.getProxy());
			((MacDumb) mac).setNetEntity(net.getProxy(), intId);
		}

		net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT,
				app.getNetProxy());
		app.setNetEntity(net.getProxy());
		app.getAppProxy().run(null);
	}

	/**
	 * Initialize simulation field.
	 * 
	 * @param nodes
	 *            number of nodes
	 * @param length
	 *            length of field
	 * @return simulation field
	 */
	public static Field createSim(int nodes, int length, int time,
			int maxspeed, int pausetime) {
		Location.Location2D bounds = new Location.Location2D(length, length);
		Placement placement = new Placement.Random(bounds);
		Mobility mobility = new Mobility.RandomWaypoint(bounds, pausetime,
				GRANULARITY, maxspeed, MIN_SPEED);

		// overwrite with FARSI mobility
		if (MOBILITY == 2)
			try {
				System.out
						.println("Ignoring all mobility parameters -- using MobilityReplay");
				System.out.println("File: " + mobilityFile);

				mobility = new MobilityReplay(null, 10, mobilityFile,
						MobilityReaderNs2.class.getName());
				bounds = ((MobilityReplay) mobility).getCorners()[1];
				nodes = ((MobilityReplay) mobility).getNodeNumber();
				placement = new PlacementReplay(((MobilityReplay) mobility));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Mobility could not be loaded: "
						+ e.getMessage());
			}

		Spatial spatial = new Spatial.HierGrid(bounds, 5);
		// Fading fading = new Fading.None();
		Fading fading = new Fading.Rayleigh();
		PathLoss pathloss = new PathLoss.TwoRay();
		Field field = new Field(spatial, fading, pathloss, mobility,
				Constants.PROPAGATION_LIMIT_DEFAULT);

		RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
				Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
				Constants.TRANSMIT_DEFAULT, Constants.GAIN_DEFAULT,
				Util.fromDB(Constants.SENSITIVITY_DEFAULT),
				Util.fromDB(Constants.THRESHOLD_DEFAULT),
				Constants.TEMPERATURE_DEFAULT,
				Constants.TEMPERATURE_FACTOR_DEFAULT,
				Constants.AMBIENT_NOISE_DEFAULT);

		// Overwrite radio PHY parameters with 802.11p settings
		// Frequency: 5.89 GHz (802.11p control channel)
		// Bandwidth: 3 Mbps (IEEE 802.11p specification)
		// TxPower : 30 dbm (C2C-CC Handbook)
		// SensitivityThreshold: -85 dbm (IEEE 802.11p specification)
		// ReceiveThreshold: -75 dbm (-85 + 10 => doubled sensing range, seems
		// reasonable)
		if (PHY_TYPE == 2) {
			radioInfoShared = RadioInfo.createShared(5.89e9, (int) 3e6, 15.0,
					Constants.GAIN_DEFAULT, Util.fromDB(-85.0),
					Util.fromDB(-75.0), Constants.TEMPERATURE_DEFAULT,
					Constants.TEMPERATURE_FACTOR_DEFAULT,
					Constants.AMBIENT_NOISE_DEFAULT, false);
		}

		Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
		protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT);
		PacketLoss pl = new PacketLoss.Zero();

		AppHeartbeat.HeartbeatStats.duration = time;

		for (int i = 0; i < nodes; i++) {
			createNode(i, field, placement, radioInfoShared, protMap, pl, pl,
					RADIO_TYPE, MAC_TYPE);
		}

		return field;
	}

	/**
	 * Benchmark entry point: heartbeat test.
	 * 
	 * @param args
	 *            command-line parameters
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			System.out
					.println("syntax: swans driver.heartbeat <nodes> <length> <time> [<maxspeed> [<pausetime>]]");
			System.out.println("    eg: swans driver.heartbeat 5 100 5");
			return;
		}

		int nodes = Integer.parseInt(args[0]);
		int length = Integer.parseInt(args[1]);
		int time = Integer.parseInt(args[2]);
		float density = nodes / (float) (length / 1000.0 * length / 1000.0);

		int maxspeed = MAX_SPEED;
		if (args.length >= 4) {
			maxspeed = Integer.parseInt(args[3]);
		}
		int pausetime = PAUSE_TIME;
		if (args.length >= 5) {
			pausetime = Integer.parseInt(args[4]);
		}

		// initialize random without seed
		Constants.random = new java.util.Random();

		System.out.println("nodes   = " + nodes);
		System.out.println("size    = " + length + " x " + length);
		System.out.println("time    = " + time + " seconds");
		System.out.println("Creating simulation nodes... ");
		Field f = createSim(nodes, length, time, maxspeed, pausetime);
		System.out.println("done.");

		System.out.println("Average density = " + f.computeDensity() * 1000
				* 1000 + "/km^2");
		System.out.println("Average sensing = "
				+ f.computeAvgConnectivity(true));
		System.out.println("Average receive = "
				+ f.computeAvgConnectivity(false));
		JistAPI.endAt(time * Constants.SECOND);

		// Elmar Schoch added >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		JistAPI.runAt(new Runnable() {
			public void run() {
				System.out.println("Evaluation");
				int i = 0;
				float avgMinSum = 0;
				float avgMaxSum = 0;
				float avgSum = 0;

				int[] periodCount = new int[AppHeartbeat.HeartbeatStats.duration + 1];
				for (int k = 0; k < periodCount.length; k++) {
					periodCount[k] = 0;
				}

				for (AppHeartbeat app : appList) {
					float[] stats = app.hbs.getNeighborCountStats(
							5 * Constants.SECOND,
							AppHeartbeat.HeartbeatStats.duration
									* Constants.SECOND,
							500 * Constants.MILLI_SECOND);
					float[] statTime = app.hbs.getNeighborTimeStats(1);
					avgMinSum += stats[0];
					avgMaxSum += stats[1];
					avgSum += stats[2];
					System.out.println("Node " + i + ":");
					System.out.println(" - Neighbor Count:  Min: " + stats[0]
							+ "  Max: " + stats[1] + "  Avg: " + stats[2]);
					System.out.println(" - Neighbor Time:   Min: "
							+ statTime[0] + "  Max: " + statTime[1] + "  Avg: "
							+ statTime[2]);
					// app.hbs.displayNeighborTrace();
					// neighbor periods collection
					float[] perCounts = app.hbs.getNeighborTimeStats(2);
					if (perCounts != null) {
						assert (perCounts.length <= periodCount.length);
						for (int j = 0; j < perCounts.length; j++) {
							periodCount[j] += Math.round(perCounts[j]);
						}
					}

					i++;
				}
				System.out.println("Min average: "
						+ (avgMinSum / (float) appList.size()));
				System.out.println("Max average: "
						+ (avgMaxSum / (float) appList.size()));
				System.out.println("Total average: "
						+ (avgSum / (float) appList.size()));

				for (int j = 0; j < periodCount.length; j++) {
					System.out.println("PeriodLength: " + j + " Count: "
							+ periodCount[j]);
					// System.out.println(periodCount[j]);
				}
			}
		}, JistAPI.END);
		// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	}

}
