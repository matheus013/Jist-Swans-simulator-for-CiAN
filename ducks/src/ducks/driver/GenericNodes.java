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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;

import vans.straw.Visualizer;
import vans.straw.VisualizerInterface;

import jist.swans.Constants;
import jist.swans.field.Mobility;
import jist.swans.field.Placement;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.misc.Location.Location2D;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;

import ducks.misc.DucksException;
import ext.jist.swans.mobility.MobilityReplay;
import ext.util.ExtendedProperties;
import ext.util.ReflectionUtils;
import ext.util.stats.MultipleStatsCollector;

/**
 * The GenericNodes interface represents the nodes collection of a simulation.
 * It also allows for different node types on the field.
 * 
 * @author Elmar Schoch
 * 
 */
public class GenericNodes implements Nodes {

	// log4j Logger
	private static Logger log = Logger.getLogger(GenericNodes.class.getName());

	protected Scene scene;
	protected HashMap<String, ArrayList<Node>> nodes;
	protected ExtendedProperties config;
	protected ExtendedProperties globalConfig;

	protected MultipleStatsCollector collector = new MultipleStatsCollector();

	public void configure(ExtendedProperties config) throws DucksException {
		this.config = config;

		float x, y;
		try {
			x = globalConfig.getFloatProperty(SimParams.SCENE_NAMEPSPACE + "."
					+ SimParams.SCENE_FIELD_SIZE_X);
			y = globalConfig.getFloatProperty(SimParams.SCENE_NAMEPSPACE + "."
					+ SimParams.SCENE_FIELD_SIZE_Y);
		} catch (Exception e) {
			throw new DucksException(e.getMessage());
		}

		Location2D dimensions = new Location2D(x, y);

		// Get the namespaces for different node types
		HashMap<String, ExtendedProperties> nodeTypes = config
				.getSubNamespaces(SimParams.NODES_NAMESPACE, false);
		// workaround: we have an entry "ducks.nodes.class" which is actually no
		// subnamespace under ducks.nodes
		nodeTypes.remove("class");

		Set<String> keys = nodeTypes.keySet();
		nodes = new HashMap<String, ArrayList<Node>>();

		// Iterate through different node types and instantiate them
		// int nodeNum = 0;
		/*
		 * TODO: Christin: It is crucial that there is no nodeID=0 as this node
		 * will have a MacAddress.NULL=0, which marks an invalid address. This
		 * creates problems e.g., in AODV a rreq for destIp=0 is rebroadcasted
		 * although there is an entry for 0 in the routing table. See
		 * RouteAodv.RREQtimeout().
		 */
		int nodeNum = 1;

		for (String key : keys) {
			ExtendedProperties nodeProps = nodeTypes.get(key);

			// how may nodes do we need?
			int count;
			try {
				String nc = nodeProps.getStringProperty(SimParams.NODE_COUNT);

				// Special handling for MobilityReplay
				if (nc.equals(SimParams.NODE_COUNT_FROM_SCENE)) {
					// if the count parameter is set to "scene", then the number
					// of nodes
					// to create will be requested from MobilityReplay
					if (scene.getField().getMobility() instanceof MobilityReplay) {
						MobilityReplay mr = (MobilityReplay) scene.getField()
								.getMobility();
						count = mr.getNodeNumber();
					} else {
						throw new Exception(
								"Node count can not be retrieved from scene (no mobility replay)");
					}
				} else {
					// if it's not, we can assume to have a regular integer
					// number
					count = nodeProps.getIntProperty(SimParams.NODE_COUNT);
				}

			} catch (Exception e) {
				throw new DucksException(e.getMessage());
			}

			try {
				ArrayList<Node> thesenodes = createNodes(nodeProps, key, count,
						nodeNum, dimensions);
				nodes.put(key, thesenodes);
			} catch (Exception e) {
				e.printStackTrace();
				throw new DucksException(e.getMessage());
			}
			nodeNum += count;
		}

	}

	public ExtendedProperties getConfig() {
		return this.config;
	}

	protected ArrayList<Node> createNodes(ExtendedProperties nodeProps,
			String nodeType, int count, int startNum, Location2D fieldsize)
			throws Exception {

		log.debug("Creating " + count + " node(s) of class " + nodeType
				+ ", starting with id=" + startNum);

		ArrayList<Node> list = new ArrayList<Node>();

		// Instantiate common objects .........................................
		// custom transmission power
		double txPower = nodeProps.getDoubleProperty(SimParams.RADIO_TX_POWER,
				Constants.TRANSMIT_DEFAULT);

		// initialize shared radio information
		RadioInfo.RadioInfoShared radioInfo = RadioInfo.createShared(
				Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
				txPower, Constants.GAIN_DEFAULT,
				Util.fromDB(Constants.SENSITIVITY_DEFAULT),
				Util.fromDB(Constants.THRESHOLD_DEFAULT),
				Constants.TEMPERATURE_DEFAULT,
				Constants.TEMPERATURE_FACTOR_DEFAULT,
				Constants.AMBIENT_NOISE_DEFAULT);

		// initialize shared protocol mapper
		Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);

		// initialize packet loss
		PacketLoss outLoss, inLoss;
		String outLossProp = nodeProps.getStringProperty(
				SimParams.PACKETLOSS_OUT, SimParams.PACKETLOSS_ZERO);

		if (outLossProp.equals(SimParams.PACKETLOSS_ZERO)) {
			outLoss = new PacketLoss.Zero();
		} else if (outLossProp.equals(SimParams.PACKETLOSS_UNIFORM)) {
			double prob = nodeProps.getDoubleProperty(
					SimParams.PACKETLOSS_OUT_UNIFORM, 0);
			outLoss = new PacketLoss.Uniform(prob);
		} else {
			throw new Exception("Unknown model for outgoing packet loss: "
					+ outLossProp);
		}

		String inLossProp = nodeProps.getStringProperty(
				SimParams.PACKETLOSS_IN, SimParams.PACKETLOSS_ZERO);

		if (inLossProp.equals(SimParams.PACKETLOSS_ZERO)) {
			inLoss = new PacketLoss.Zero();
		} else if (outLossProp.equals(SimParams.PACKETLOSS_UNIFORM)) {
			double prob = nodeProps.getDoubleProperty(
					SimParams.PACKETLOSS_IN_UNIFORM, 0);
			inLoss = new PacketLoss.Uniform(prob);
		} else {
			throw new Exception("Unknown model for incoming packet loss: "
					+ inLossProp);
		}

		// initialize node placement model
		Placement place = null;

		String placement = nodeProps
				.getStringProperty(SimParams.PLACEMENT_MODEL);
		if (placement.equals(SimParams.PLACEMENT_MODEL_RANDOM)) {
			place = new Placement.Random(fieldsize);
		} else if (placement.equals(SimParams.PLACEMENT_MODEL_GRID)) {

			int nodex, nodey;
			nodex = nodeProps.getIntProperty(SimParams.PLACEMENT_GRID_I);
			nodey = nodeProps.getIntProperty(SimParams.PLACEMENT_GRID_J);
			place = new Placement.Grid(fieldsize, nodex, nodey);

		} else if (placement.equals(SimParams.PLACEMENT_MODEL_STREETRANDOM)) {

			// StreetMobility smr = (StreetMobility) field.getMobility();
			// Location2D bounds[] = (Location2D[]) smr.getBounds();
			// place = new StreetPlacementRandom(bounds[0], bounds[3], smr,
			// 4.0);

			// invoke method via reflection ("soft" binding)
			Mobility mob = scene.getField().getMobility();

			Location2D bounds[] = (Location2D[]) ReflectionUtils.invokeMethod(
					"vans.straw.StreetMobility", "getBounds", null, mob, null);
			Class smobClass = ReflectionUtils
					.getClass("vans.straw.StreetMobility");
			Class[] paramTypes = new Class[] { Location.class, Location.class,
					smobClass, double.class };
			Object[] params = new Object[] { bounds[0], bounds[3], mob, 4.0 };
			place = (Placement) ReflectionUtils.createObject(
					"vans.straw.StreetPlacementRandom", paramTypes, params);

		} else {
			throw new Exception("unknown node placement model");
		}

		// TODO Does not work like that. Will create more than one Visualizer
		// with > 1 node types
		VisualizerInterface v = null;
		if (globalConfig.getBooleanProperty(SimParams.SCENE_NAMEPSPACE + "."
				+ SimParams.SCENE_STRAWVIZ)) {

			String mobilityModel = globalConfig
					.getStringProperty(SimParams.SCENE_NAMEPSPACE + "."
							+ SimParams.MOBILITY_MODEL);
			if (!(mobilityModel.equals(SimParams.MOBILITY_MODEL_STRAW_SIMPLE) || mobilityModel
					.equals(SimParams.MOBILITY_MODEL_STRAW_OD))) {

				v = new Visualizer(scene.getField(), (int) fieldsize.getX(),
						(int) fieldsize.getY());
			} else {
				v = new Visualizer(scene.getField(), 0, 0);
			}
		}

		// Instantiate nodes
		for (int i = 0; i < count; i++) {
			String nodeClass = nodeProps
					.getStringProperty(SimParams.NODE_CLASS);
			Node n;
			n = (Node) Class.forName(nodeClass).newInstance();
			n.setIdentifier(startNum++);
			n.setScene(scene);
			n.setCommonObjects(place, radioInfo, protMap, inLoss, outLoss, v);
			n.setGlobalConfig(globalConfig);
			// configure node
			try {
				n.configure(nodeProps);
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception("Error while configuring node: "
						+ e.getMessage());
			}

			collector.registerStaticCollector(n, "node." + nodeType);

			list.add(n);
		}

		return list;
	}

	public String[] getStatParams() {
		return collector.getStatParams();
	}

	public ExtendedProperties getStats() {
		return collector.getStats();
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

	public void setGlobalConfig(ExtendedProperties config) {
		this.globalConfig = config;
	}

}
