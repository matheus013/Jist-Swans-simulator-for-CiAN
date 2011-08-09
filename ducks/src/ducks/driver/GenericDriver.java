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

import org.apache.log4j.Logger;

import ducks.eventlog.EventLog;
import ext.util.ExtendedProperties;
import ext.util.stats.MultipleStatsCollector;

/**
 * GenericDriver is a driver for DUCKS, which wraps most of the functionality
 * available from SWANS and from several UULM extensions. Given a suitable
 * config file, it can build and run various ad hoc network scenarios.
 * 
 * Regarding the architecture, it follows roughly the "configure"-pattern. This
 * means, that components implement the DucksDriverModule interface, which in
 * turn extends the Configurable interface. Therefore, components provide a
 * configure method and use this to initialize their sub-components.
 * 
 * Because GenericDriver, GenericNodes, GenericNode and GenericScene are all
 * based on particular interfaces, it is easily possible to exchange parts of
 * the driver, e.g. only by implementing an own node class while keeping
 * everything else. The class only needs to be given in the config file.
 * 
 * GenericDriver |- GenericScene |- Field (incl. mobility, spatial, pathloss,
 * fading, ...) |- GenericNodes |- List<GenericNode> |- Placement, PacketLoss,
 * Radio, MAC, Routing, IP, App, ...
 * 
 * @author Elmar Schoch
 * 
 */
public class GenericDriver implements DucksDriverModule {

	// log4j Logger
	private static Logger log = Logger.getLogger(GenericNode.class.getName());

	protected ExtendedProperties config;
	protected Scene scene;
	protected Nodes nodes;

	protected MultipleStatsCollector collector = new MultipleStatsCollector();

	public void configure(ExtendedProperties config) throws Exception {

		this.config = config;

		// create scene
		String sceneClass;
		try {
			sceneClass = config.getStringProperty(SimParams.SCENE_CLASS);
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}

		try {
			scene = (Scene) Class.forName(sceneClass).newInstance();
			collector.registerCollector(scene);
			ExtendedProperties scenecfg = config.getNamespace(
					SimParams.SCENE_NAMEPSPACE, false);
			scene.configure(scenecfg);
			scene.setGlobalConfig(config);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Scene class could not be loaded: "
					+ sceneClass + " Problem: " + e.getMessage());
		}

		// create nodes handler
		String nodesClass;
		try {
			nodesClass = config.getStringProperty(SimParams.NODES_CLASS);
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		try {
			nodes = (Nodes) Class.forName(nodesClass).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Nodes class could not be loaded: "
					+ nodesClass + ": " + e.getMessage());
		}
		// configure nodes
		try {
			nodes.setScene(scene);
			nodes.setGlobalConfig(config);
			collector.registerCollector(nodes);
			nodes.configure(config);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Nodes could not be configured: "
					+ e.getMessage());
		}

		// initialize runtime logging
		String logDest = null;
		try {
			logDest = config.getStringProperty(SimParams.EVENTLOG_DEST);
		} catch (Exception e) {
			log.info("Event logging destinations not defined. Skipping eventlogging ...");
		}

		if (logDest != null) {
			String[] dests = logDest.split(",");
			for (int i = 0; i < dests.length; i++) {
				while (dests[i].endsWith(" "))
					dests[i] = dests[i].substring(0, dests[i].length() - 1);
				EventLog.findEventLog(dests[i], config);
			}
			if (EventLog.hasEventLogs()) {
				String[] logModule = config.getProperty(
						SimParams.EVENTLOG_MODULES, "").split(",");
				EventLog.loadModules(logModule, scene.getField(), config);
			}
		}

	}

	public ExtendedProperties getConfig() {
		return this.config;
	}

	public String[] getStatParams() {
		return collector.getStatParams();
	}

	public ExtendedProperties getStats() {
		return collector.getStats();
	}

}
