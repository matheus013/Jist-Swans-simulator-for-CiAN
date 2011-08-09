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
package ext.util.stats;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import ext.util.ExtendedProperties;

/**
 * MultipleStatsCollector is a encapsulating class that is able to hold objects
 * implementing the StatsCollector interface, and calls them in behalf.
 * Additionally, MultipleStatsCollector implements the possibility to register
 * static collectors. In this case, if a second stats collecting object with the
 * same class wants to be registered, it is not registered actually. This is
 * usefull, if stats are collected in static variables (e.g. total number of
 * sent messages throughout all network nodes)
 * 
 * @author Elmar Schoch
 * 
 */
public class MultipleStatsCollector implements StatsCollector {

	private static Logger log = Logger.getLogger(MultipleStatsCollector.class
			.getName());

	// private Vector statsCollectors = new Vector();
	private HashMap<StatsCollector, String> statsCollectors = new HashMap<StatsCollector, String>();

	private HashMap<String, String> additionalParams;

	/**
	 * Register a collector
	 * 
	 * @param sc
	 */
	public void registerCollector(StatsCollector sc) {
		registerCollector(sc, "");
	}

	/**
	 * Register a collector within a certain namespace
	 * 
	 * CAUTION! The namespace MUST be in valid Java property format, e.g.
	 * "node.a", otherwise parsing of statistics will fail!
	 * 
	 * @param sc
	 *            The collector object
	 * @param namespace
	 *            Namespace in valid Java property format
	 */
	public void registerCollector(StatsCollector sc, String namespace) {
		if (!(namespace.equals("") || ExtendedProperties
				.isValidPropertyName(namespace))) {
			log.warn("Registering collector failed: " + namespace
					+ " is not a valid namespace");
			return;
		}
		statsCollectors.put(sc, namespace);
	}

	/**
	 * Registers only one instance of the given object class. In other words, if
	 * a different StatsCollector implementing object of the same class is
	 * already registered, sc won't be registered.
	 * 
	 * @param sc
	 *            the collector to register
	 */
	public void registerStaticCollector(StatsCollector sc) {
		if (!hasStaticCollector(sc)) {
			registerCollector(sc);
		}
	}

	/**
	 * Register static collector, with namespace support
	 * 
	 * CAUTION! The namespace MUST be in valid Java property format, e.g.
	 * "node.a", otherwise parsing of statistics will fail!
	 * 
	 * @param sc
	 * @param namespace
	 */
	public void registerStaticCollector(StatsCollector sc, String namespace) {
		if (!hasStaticCollector(sc.getClass(), namespace)) {
			registerCollector(sc, namespace);
		}
	}

	/**
	 * Searches the registered collector objects and returns true if an object
	 * of the same class as the given one is registered already. Otherwise,
	 * false is returned.
	 * 
	 * @param sc
	 * @return true if object of same class as sc is registered
	 */
	public boolean hasStaticCollector(StatsCollector sc) {
		return hasStaticCollector(sc.getClass());
	}

	public boolean hasStaticCollector(Class c) {
		return hasStaticCollector(c, "");
	}

	public boolean hasStaticCollector(Class c, String namespace) {
		return getStaticCollector(c, namespace) != null;
	}

	public StatsCollector getStaticCollector(Class c, String namespace) {
		StatsCollector result = null;
		Set<Map.Entry<StatsCollector, String>> scs = statsCollectors.entrySet();
		for (Map.Entry<StatsCollector, String> entry : scs) {
			StatsCollector sc = entry.getKey();
			String ns = entry.getValue();
			if (sc.getClass().getName().equals(c.getName())
					&& ns.equals(namespace)) {
				result = sc;
				break;
			}
		}
		return result;
	}

	/**
	 * Return an object with class c, if one has been registered as collector
	 * 
	 * @param c
	 *            the searched class
	 * @return the corresponding StatsCollector object, or null if not
	 *         registered
	 * @throws Exception
	 */
	public StatsCollector getStaticCollector(Class c) throws Exception {
		return getStaticCollector(c, "");
	}

	public StatsCollector getStaticCollector(Class c, boolean createAndAdd)
			throws Exception {
		return getStaticCollector(c, "", createAndAdd);
	}

	public StatsCollector getStaticCollector(Class c, String namespace,
			boolean createAndAdd) throws Exception {
		StatsCollector sc = getStaticCollector(c, namespace);

		if (sc == null && createAndAdd == true) {
			sc = (StatsCollector) c.newInstance();
			registerCollector(sc, namespace);
		}
		// still null?
		if (sc == null) {
			throw new Exception("Static collector for class " + c.getName()
					+ " not found");
		}

		return sc;
	}

	// StatsCollector interface implementation
	// ..........................................

	/**
	 * Retrieve stats for all registered collectors
	 */
	public ExtendedProperties getStats() {

		ExtendedProperties stats = new ExtendedProperties();

		Set<Map.Entry<StatsCollector, String>> scs = statsCollectors.entrySet();
		for (Map.Entry<StatsCollector, String> entry : scs) {
			StatsCollector sc = entry.getKey();
			String ns = entry.getValue();
			ExtendedProperties theseStats = sc.getStats();
			if (theseStats != null) {
				if (!ns.equals("")) {
					theseStats.addPrefix(ns);
				}
				stats.putAll(theseStats);
			}
		}

		if (additionalParams != null) {
			for (String param : additionalParams.keySet()) {
				stats.put(param, additionalParams.get(param));
			}
		}
		return stats;
	}

	/**
	 * Retrieve stats parameter set for all registered collectors. May be used
	 * for consistency checks, because this lists the parameters that will be
	 * delivered as results by "getStats()" from the StatsCollector objects.
	 */
	public String[] getStatParams() {

		Vector v = new Vector();
		int paramCount = 0;

		Set<Map.Entry<StatsCollector, String>> scs = statsCollectors.entrySet();
		for (Map.Entry<StatsCollector, String> entry : scs) {
			StatsCollector sc = entry.getKey();
			String ns = entry.getValue();

			String[] scpm = sc.getStatParams();
			// prepend namespace if necessary
			if (!ns.equals("")) {
				for (int i = 0; i < scpm.length; i++) {
					scpm[i] = ns + scpm[i];
				}
			}
			paramCount += scpm.length;
			v.add(scpm);
		}

		paramCount += additionalParams.size();

		String[] params = new String[paramCount];
		Enumeration slist = v.elements();
		int paramIndex = 0;
		while (slist.hasMoreElements()) {
			String[] scpm = (String[]) slist.nextElement();
			for (int i = 0; i < scpm.length; i++) {
				params[paramIndex + i] = scpm[i];
			}
			paramIndex += scpm.length;
		}
		if (additionalParams != null) {
			for (String param : additionalParams.keySet()) {
				params[paramIndex] = param;
				paramIndex++;
			}
		}

		return params;
	}

	// Add single parameters
	// ......................................................
	// (useful for multiple stats collectors that also provide own result
	// values)

	public void addStatParam(String param) {
		if (additionalParams == null) {
			additionalParams = new HashMap<String, String>();
		}
		additionalParams.put(param, null);
	}

	public void putStats(String param, String value) {
		if (additionalParams == null) {
			additionalParams = new HashMap<String, String>();
		}
		additionalParams.put(param, value);
	}
}
