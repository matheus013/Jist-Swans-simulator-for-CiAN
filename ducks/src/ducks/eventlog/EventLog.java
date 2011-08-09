/*
 * Ulm University DUCKS project
 * 
 * Author:		Stefan Schlott <stefan.schlott@uni-ulm.de>
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
package ducks.eventlog;

import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import ducks.driver.SimParams;

import jist.runtime.JistAPI.DoNotRewrite;
import jist.swans.field.Field;
import jist.swans.misc.Location;

/**
 * 
 * Framework for logging runtime data (like node movements) to arbitrary
 * destinations (e.g. console, files, databases).
 * <p/>
 * To enable debug output, put in your log4j.properties:
 * 
 * <pre>
 * log4j.logger.ducks.eventlog = DEBUG
 * </pre>
 * <p/>
 * To enable the eventlog framework, put in your simulation property file:
 * 
 * <pre>
 * ducks.eventlog.dest=<i>destination classname</i>
 * ducks.eventlog.modules=<i>module classname[,module classname...]</i>
 * </pre>
 * <p/>
 * eventlog will first look for a full qualified classname, then for a class in
 * ducks.eventlog.[destinations|modules].
 * <p/>
 * Additional parameters for the log destination may use the config key
 * 
 * <pre>
 * ducks.eventlog.dest.<i>classname</i>[.*]
 * </pre>
 * 
 * Log modules can use config keys accordingly.
 * 
 * @author Stefan Schlott
 * 
 */

public abstract class EventLog implements DoNotRewrite {
	protected static Vector<EventLog> eventlogs = new Vector<EventLog>();
	protected static final Logger logger = Logger.getLogger(EventLog.class
			.getName());
	protected static Vector<EventLogModule> modules = new Vector<EventLogModule>();

	/**
	 * Get event log destinations
	 * 
	 * @return EventLog instance, or null if none set
	 */
	public static Iterator<EventLog> getEventLogs() {
		return eventlogs.iterator();
	}

	public static boolean hasEventLogs() {
		return (eventlogs.size() > 0);
	}

	/**
	 * Replace placeholders in pattern string with values from config properties
	 * list. Placeholders are written in curly braces, e.g.
	 * {ducks.general.nodes}.
	 * 
	 * @param config
	 *            Property list with ducks instance configuration
	 * @param pattern
	 *            String containing placeholders {property.name}
	 * @return Result of placeholder expansion
	 */
	public static String configStringReplacer(Properties config, String pattern) {
		StringBuffer result = new StringBuffer(pattern);

		while (result.indexOf("{") >= 0) {
			int start = result.indexOf("{");
			int end = result.indexOf("}");

			if (end < start) {
				result.deleteCharAt(end);
			} else {
				String propName = result.substring(start + 1, end);
				if (config.containsKey(propName))
					result.replace(start, end + 1, config.getProperty(propName));
				else
					result.replace(start, end + 1, "0");
			}
		}

		return result.toString();
	}

	/**
	 * Add event log target.
	 * 
	 * @param logger
	 */
	public static void addEventLog(EventLog logger) {
		if (logger != null)
			eventlogs.add(logger);
	}

	/**
	 * Set event log target (and remove all existing ones)
	 * 
	 * @param logger
	 */
	public static void setEventLog(EventLog logger) {
		finalizeLoggers();
		eventlogs.clear();
		addEventLog(logger);
	}

	/**
	 * 
	 * Find a destination class according to the naming rules given above. The
	 * class will be instantiated using the default constructor (no parameters).
	 * After that, the configure method is called.
	 * 
	 * @param classname
	 *            Name of the class
	 * @param config
	 *            Simulation property file
	 */
	public static EventLog findEventLog(String classname, Properties config) {
		EventLog el = null;
		String prefix = null;
		logger.debug("Trying to instantiate EventLog class " + classname);
		try {
			el = (EventLog) Class.forName(classname).newInstance();
			prefix = classname;
		} catch (Exception e1) {
			try {
				el = (EventLog) Class.forName(
						"ducks.eventlog.destinations." + classname)
						.newInstance();
				prefix = SimParams.EVENTLOG_DEST + "." + classname;
			} catch (Exception e2) {
				logger.error("Unable to instantiate EventLog destination "
						+ classname);
			} catch (Error err) {
				logger.error("Unable to instantiate EventLog destination "
						+ classname);
			}
		}
		if (el != null) {
			el.configure(config, prefix);
			addEventLog(el);
			logger.debug("Successfully instantiated EventLog destination "
					+ classname);
		}
		return el;
	}

	/**
	 * Safe event logging. Writes log data if a logging instance is set.
	 */
	public static void log(int node, long time, Location loc, String type,
			String comment) {
		Iterator<EventLog> it = eventlogs.iterator();

		while (it.hasNext())
			it.next().logEvent(node, time, loc, type, comment);
	}

	public static void finalizeLoggers() {
		Iterator<EventLog> it = eventlogs.iterator();

		while (it.hasNext())
			it.next().finalize();
	}

	/**
	 * Configure the logging instance (e.g. get database settings and open db
	 * connection, etc)
	 * 
	 * @param config
	 *            Simulation configuration
	 * @param configprefix
	 *            Prefix for configuration parameters
	 */
	public void configure(Properties config, String configprefix) {
	}

	public void finalize() {
	}

	/**
	 * Do actual logging. Mind that parameters may be null or <0, which means
	 * that they are irrelevant for the log entry.
	 * 
	 * @param node
	 *            Node number
	 * @param time
	 *            Simulation time
	 * @param loc
	 *            Position
	 * @param type
	 *            String describing the kind of log entry, e.g. "move",
	 *            "transmit", etc.
	 * @param comment
	 *            For arbitrary comments
	 */
	public abstract void logEvent(int node, long time, Location loc,
			String type, String comment);

	/**
	 * Load all log module classes according to the naming scheme given above.
	 */
	public static void loadModules(String[] modulename, Field field,
			Properties config) {
		int i;

		for (i = 0; i < modulename.length; i++) {
			EventLogModule m = null;
			try {
				m = (EventLogModule) Class.forName(modulename[i]).newInstance();
			} catch (Exception e1) {
				try {
					m = (EventLogModule) Class.forName(
							"ducks.eventlog.modules." + modulename[i])
							.newInstance();
				} catch (Exception e2) {
					logger.error("Unable to instantiate EventLog module "
							+ modulename[i]);
				}
			}
			if (m != null) {
				String prefix = SimParams.EVENTLOG_MODULEPREFIX + modulename[i];
				m.configure(field, config, prefix);
				modules.add(m);
				m.enable();
				logger.debug("Successfully instantiated EventLog module "
						+ modulename[i]);
			}
		}
	}
}
