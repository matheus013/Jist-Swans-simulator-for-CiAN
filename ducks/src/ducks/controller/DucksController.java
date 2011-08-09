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
package ducks.controller;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import jargs.gnu.CmdLineParser;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ducks.controller.output.StorageManager;
import ducks.controller.server.ServerManager;
import ducks.controller.simulation.Simulation;
import ducks.controller.simulation.SimulationInstance;
import ducks.controller.simulation.SimulationStudy;
import ducks.misc.DucksException;

import ext.util.ExtendedProperties;

/**
 * DucksController is the central class of the DUCKS framework. It controls <br>
 * <li>the currently available list of jist servers <li>the currently prepared,
 * running and completed simulation configurations <li>the connection to the
 * database that stores simulation results
 * 
 * DUCKS Version 2
 * 
 * @author Elmar Schoch
 */
public class DucksController {

	// log4j Logger
	private static Logger log = Logger.getLogger(DucksController.class
			.getName());

	// (optional) remote management interface to the controller
	private ControllerProxy proxy;
	public static final String CGF_PROXY_ENABLED = "ducks.config.monitor.enabled";

	// list of available servers
	private ServerManager servers;
	// current result manager
	private StorageManager storage;

	// simulation studies
	private Vector<SimulationStudy> studies;
	// currently simulation study beeing processed
	private SimulationStudy currentStudy = null;

	public DucksController() {
		servers = new ServerManager(this);
		storage = new StorageManager();

		studies = new Vector<SimulationStudy>();
	}

	/**
	 * Retrieve the server manager object
	 * 
	 * @return server manager
	 */
	public ServerManager getServers() {
		return servers;
	}

	/**
	 * Retrieve the simulation study list
	 * 
	 * @return simulation studies
	 */
	public Vector getSimulationStudies() {
		return studies;
	}

	/**
	 * Get first unscheduled simulation
	 * 
	 * @return Simulation first unscheduled simulation
	 * @see ducks.SimGeneratorInt
	 */
	public synchronized SimulationInstance getFreeSimulation() {
		if (currentStudy != null) {
			return currentStudy.getPendingSimInstance();
		} else {
			return null;
		}
	}

	/**
	 * Find a simulation study that has not been processed yet
	 * 
	 * @return Unprocessed simulation study object, or null if none is available
	 */
	private SimulationStudy getNextSimulationStudy() {
		for (int i = 0; i < studies.size(); i++) {
			SimulationStudy sst = (SimulationStudy) studies.get(i);
			if (!sst.hasFinished() && sst != currentStudy) {
				return sst;
			}
		}
		// no unprocessed study found
		return null;
	}

	/**
	 * Main method
	 * 
	 * @param args
	 *            Command line arguments for main. <li>-s / --simuconfig
	 *            filename : specify a simulation study config file to use <li>
	 *            -d / --ducksconfig filename : specify a ducks controller
	 *            config file to use (default ducks/resources/ducks.properties)
	 *            <li>-n / --nodb : run simulations without saving results to
	 *            database <li>-l / --debuglevel : set basic debug level (log4j:
	 *            INFO,WARN,DEBUG) <li>-p / --persist : do not exit controller,
	 *            if all studies are processed (i.e. wait for new ones via
	 *            remote proxy)
	 * 
	 */
	public static void main(String[] args) {

		// Handling command line options & config properties
		// ............................

		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option optSimuConfig = parser.addStringOption('s',
				"simuconfig");
		CmdLineParser.Option optDebugConfig = parser.addStringOption('d',
				"debugconfig");
		CmdLineParser.Option optDebugLevel = parser.addStringOption('l',
				"debuglevel");
		CmdLineParser.Option optPersist = parser.addIntegerOption('p',
				"persist");
		CmdLineParser.Option optNoSave = parser.addBooleanOption('n', "nosave");

		try {
			parser.parse(args);
		} catch (Exception e) {
			System.out.println("Error parsing commandline options: " + args);
			System.out.println("Usage: ");
			System.out.println("  -s  --simuconfig <simulation config file>");
			System.out.println("  -p  --persist (optional)");
			System.out.println("  -n  --nosave  (optional)");
			System.out
					.println("  -d  --debugconfig <log4j config file> (optional)");
			System.out
					.println("  -l  --debuglevel <root log4j level> (optional)");
			System.exit(99);
		}

		// Init log4j logging .................................................
		// 1. Control root level (can be set as well)
		// 2. Use detailed log4j properties, if given
		BasicConfigurator.configure();

		Level l = Level.ERROR;
		if (parser.getOptionValue(optDebugLevel) != null) {
			String loglevel = parser.getOptionValue(optDebugLevel).toString();
			l = Level.toLevel(loglevel, l);
		}
		Logger.getRootLogger().setLevel(l);

		if (parser.getOptionValue(optDebugConfig) != null) {
			File f = new File(parser.getOptionValue(optDebugConfig).toString());

			if (f.exists()) {
				System.out.println("Starting logging with file "
						+ f.getAbsoluteFile());
				PropertyConfigurator.configure((parser
						.getOptionValue(optDebugConfig)).toString());
			}
		}

		System.out.println("RootLogger is on level "
				+ Logger.getRootLogger().getLevel());

		// Loading configuration of simulation
		// ...........................................
		ExtendedProperties config = new ExtendedProperties();
		String configFilename = (String) parser.getOptionValue(optSimuConfig);
		if (configFilename == null) {
			// if nothing given at the command line, try "config.properties"
			configFilename = "config.properties";
		}

		try {
			config.loadFromFile(configFilename);
		} catch (IOException e) {
			log.error("Loading DUCKS config file failed: given filename: "
					+ configFilename + " working dir: "
					+ System.getProperty("user.dir") + " Exception: "
					+ e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		// Setup servers and storage
		// .........................................................

		DucksController dc = new DucksController();

		try {
			dc.servers.configure(config);
		} catch (Exception e) {
			log.error("Loading compute servers failed: " + e.getMessage());
			e.printStackTrace();
			System.exit(2);
		}

		// if no remote server is enabled, create a local server instead
		if (dc.servers.count() == 0) {
			log.warn("Could not find any enabled servers. Creating local server");
			if (!dc.servers.createLocalServer(5550)) {
				log.error("Could not create local server! Exiting ...");
				System.exit(2);
			}
		}

		boolean nosave = parser.getOptionValue(optNoSave) != null;
		if (nosave) {
			log.warn("Saving of results is disabled!");
		} else {

			try {
				dc.storage.configure(config);
				dc.storage.enable();
				log.info("Result storage providers are initialized");

			} catch (Exception e) {
				log.error("Opening result storage failed: " + e.getMessage());
				e.printStackTrace();
				System.exit(3);
			}
		}

		// Load simulation study ...............................................

		try {
			SimulationStudy simStudy = new SimulationStudy();
			simStudy.configure(config);
			dc.studies.add(simStudy);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Loading simulation study config failed: "
					+ e.getMessage());
		}

		// Run simulations
		// ............................................................

		// set simulation study
		if (dc.studies.size() > 0) {
			dc.currentStudy = (SimulationStudy) dc.studies.get(0);

			if ((!nosave) && dc.storage.backgroundSavingEnabled()) {
				dc.storage.startBackgroundSaving(dc.currentStudy);
			}
		}

		// enable the simulation server threads
		dc.servers.enable();

		// enable remote monitoring and control of the DucksController
		// (currently only partly implemented)
		if (config.getBooleanProperty(DucksController.CGF_PROXY_ENABLED, false)) {
			dc.proxy = new ControllerProxy(dc);
		}

		// main loop
		while (true) {

			// Sleep for 10 seconds
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
			}

			if (dc.currentStudy != null) {
				// we are currently processing a study
				if (dc.currentStudy.hasFinished()) {
					// current study has finished

					if ((!nosave) && dc.storage.backgroundSavingEnabled()) {
						dc.storage.stopBackgroundSaving();
					}

					// Save data to db (if no already done in while running)
					if ((!nosave) && (!dc.storage.backgroundSavingEnabled())) {
						try {
							dc.storage.saveAll(dc.currentStudy);
						} catch (Exception e) {
							log.error("Failure with simulation study "
									+ dc.currentStudy.getIdentifier());
							log.error("  Saving results to database failed: "
									+ e.getMessage());
							log.error("  Waiting for user intervention ...");
							e.printStackTrace();
							// wait for user intervention ... (e.g. reparing the
							// database)
							continue;
						}
					}

					// set currentStudy to null
					// -> will select next available in next iteration of the
					// main loop
					// or quit, or idle
					dc.currentStudy = null;
				} else {
					// current study still processing
					// -> give some stats
					int[] states = dc.currentStudy.getStateCount();
					String st = "State of study "
							+ dc.currentStudy.getIdentifier() + ": ";
					st += "Pending: " + states[Simulation.PENDING] + ", ";
					st += "Running: " + states[Simulation.RUNNING] + ", ";
					st += "Finished: " + states[Simulation.FINISHED] + ", ";
					st += "Flushed: " + states[Simulation.FLUSHED];
					log.info(st);
				}

			} else {
				// do we have unfinished studies in the queue?
				SimulationStudy study = dc.getNextSimulationStudy();
				if (study != null) {
					// yeah, we have got one! -> set to current study and
					// continue!
					dc.currentStudy = study;
				} else {
					// not study any more? So, idle or exit
					if (parser.getOptionValue(optPersist) == null) {
						// ok, we do not want to persist, so exit loop here
						break;
					} else {
						// well, we shall persist, so wait ...
						log.info("Waiting for new simulation studies ...");
					}
				}
			}

			// Check if there are still processing servers available
			if (dc.currentStudy != null
					&& dc.servers.numberOfEnabledServers() == 0) {
				log.warn("Number of enabled servers is zero, though simulations are pending!");

				if (dc.proxy != null) {
					// if we have a remote proxy, trust on user to re-activate
					// servers again
					log.warn("Waiting for manual server re-activation via remote controller");
				} else {
					// if not remote proxy exists, try to re-enable servers
					int currentMin = 5;
					while (currentMin > 0) {
						log.debug("Trying to re-enable all servers in "
								+ currentMin + " minutes");
						currentMin--;
						try {
							Thread.sleep(60000);
						} catch (Exception e) {
						}
					}
					log.info("Re-enabling servers now ...");
					dc.servers.enable();
				}
			}

		}

		// Clean up
		// ...................................................................
		dc.servers.disable();
		dc.servers.stopLocalServer();

		try {
			if (dc.storage.backgroundSavingEnabled()) {
				dc.storage.stopBackgroundSaving();
			}
			dc.storage.saveReadyResults();
			dc.storage.disable();
		} catch (DucksException e) {
			log.warn("Storage provider disable problem: " + e.getMessage());
		}

		log.info("Terminating controller ...");
		System.exit(0);
	}

}
