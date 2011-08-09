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
package ducks.controller.output;

import java.util.ArrayList;
import java.util.Vector;

import org.apache.log4j.Logger;

import ducks.controller.DucksControllerModule;
import ducks.controller.simulation.Simulation;
import ducks.controller.simulation.SimulationStudy;
import ducks.misc.DucksException;

import ext.util.ExtendedProperties;

/**
 * StorageManager the storage of simulation results It also features background
 * saving, i.e. can run a thread that periodically checks for finished
 * simulations and flushes these results to the storage while the overall
 * simulation study has not finished yet.
 * 
 * @author Elmar Schoch
 * 
 */
public class StorageManager implements Runnable, DucksControllerModule {

	// log4j Logger
	private static Logger log = Logger
			.getLogger(StorageManager.class.getName());

	private static final String CFG_NAMESPACE = "ducks.stats";

	/**
	 * flow two modes are available - complete saving: Storage Manager saves all
	 * results only after all results are available. - background saving:
	 * Storage Manager starts flushing finished simulations periodically, i.e.
	 * while other simus have not finished yet. This should be memory efficient.
	 */

	// config file option
	public static final String CFG_BACKGROUND_SAVING = CFG_NAMESPACE
			+ ".backgroundsaving";
	// Period to wait between attempts to save results (in milliseconds)
	public static final int BACKGROUND_SAVING_WAIT = 5000;

	private boolean doBackgroundSaving = true;
	private Thread checkingThread;

	private SimulationStudy simStudy;

	/**
	 * Storage providers - can be configured in the config file with, e.g.
	 * <code>ducks.stats.destinations=db,file</code> - then, the manager
	 * searches for entries like <code>ducks.stats.db.class</code> and
	 * <code>ducks.stats.file.class</code> and tries to instantiate these
	 * classes, which must implement the interface StorageProvider - all
	 * configuration can be done within the namespace of the corresponding
	 * provider, e.g. ducks.stats.db.hostname or ducks.stats.db.port, because
	 * the provider gets all "lower" options, in the example, ducks.stats.db.*
	 */
	public static final String CFG_STORAGE_DESTINATIONS = CFG_NAMESPACE
			+ ".modules";
	private ArrayList<StorageProvider> storageModules = new ArrayList<StorageProvider>();

	public static final String CFG_INCLUDE_PARAMS = CFG_NAMESPACE
			+ ".params.include";
	public static final String CFG_EXCLUDE_PARAMS = CFG_NAMESPACE
			+ ".params.exclude";
	public static final String CFG_INCLUDE_RESULTS = CFG_NAMESPACE
			+ ".results.include";
	public static final String CFG_EXCLUDE_RESULTS = CFG_NAMESPACE
			+ ".results.exclude";

	/**
	 * Init StorageManager
	 * 
	 * @param config
	 *            DUCKS controller properties
	 * @throws DucksException
	 */
	public void configure(ExtendedProperties config) throws DucksException {

		String provList = config.getProperty(CFG_STORAGE_DESTINATIONS).trim();
		if (provList == null || provList.equals("")) {
			log.warn("No storage providers are configured!");
			return;
		}
		String[] providers = provList.split("\\,");

		// create storage providers
		try {

			for (String provider : providers) {
				String providerClass = config.getProperty(
						CFG_NAMESPACE + "." + provider + ".class").trim();
				StorageProvider sp = (StorageProvider) Class.forName(
						providerClass).newInstance();
				sp.configure(config.getNamespace(
						CFG_NAMESPACE + "." + provider, false));
				storageModules.add(sp);
			}
		} catch (ClassNotFoundException cnfe) {
			throw new DucksException("Class for storage provider not found: "
					+ cnfe.getMessage());
		} catch (InstantiationException ie) {
			throw new DucksException(
					"Class for storage provider could not be instantiated: "
							+ ie.getMessage());
		} catch (Exception e) {
			throw new DucksException("Problem loading storage providers: "
					+ e.getMessage());
		}

		doBackgroundSaving = config.getBooleanProperty(CFG_BACKGROUND_SAVING,
				true);
	}

	// Connection handling ...................................................

	public void enable() throws DucksException {
		for (StorageProvider provider : storageModules) {
			provider.enable();
		}
	}

	public void disable() throws DucksException {
		for (StorageProvider provider : storageModules) {
			provider.disable();
		}
	}

	// Runtime-flushing to storage ...........................................
	// - checks periodically for completed simulations in a separate thread
	// and inserts them into the database instantly (even while other
	// simulations
	// are still running or pending)

	public boolean backgroundSavingEnabled() {
		return doBackgroundSaving;
	}

	public void startBackgroundSaving(SimulationStudy study) {

		log.info("Starting background saving of simulations");

		// Save simulation study as base first
		simStudy = study;
		try {
			this.saveSimulationStudy(study);
		} catch (Exception e) {
			log.error("Error during saving the simulation run! Skipping background saving ("
					+ e.getMessage() + ")");
			return;
		}

		checkingThread = new Thread(this);
		checkingThread.setName("StorageManager background saver");

		synchronized (this) {
			checkingThread.start();
		}
	}

	public void stopBackgroundSaving() {
		checkingThread.interrupt();
		try {
			checkingThread.join();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public synchronized void saveReadyResults() {
		Vector<Simulation> simus = simStudy.getUnsavedFinishedSimulations();
		for (Simulation s : simus) {
			try {
				saveSimulation(s);
			} catch (DucksException de) {
				log.error("Error while flushing simulation "
						+ s.getIdentifier() + ": " + de.getMessage());
				de.printStackTrace();
			}
		}
	}

	/**
	 * Thread main method, checks regularly for finished simulations, whose
	 * results can be flushed
	 */
	public void run() {

		while (!checkingThread.isInterrupted()) {

			// Check for new results and save them to db
			saveReadyResults();

			// Exit condition: if study has finished, we can break
			if (simStudy.hasFinished()) {
				break;
			}

			// Sleep for certain period of time
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				checkingThread.interrupt();
			}
		}

		log.debug("Stopped background saving of simulations");
	}

	public void saveAll(SimulationStudy study) throws DucksException {
		saveSimulationStudy(study);
		for (Simulation simu : study.getSimulations()) {
			saveSimulation(simu);
		}
	}

	public void saveSimulationStudy(SimulationStudy study)
			throws DucksException {
		for (StorageProvider provider : storageModules) {
			provider.saveSimulationStudy(study);
		}
	}

	public void saveSimulation(Simulation simu) throws DucksException {
		for (StorageProvider provider : storageModules) {
			provider.saveSimulation(simu);
		}
		simu.setFlushed();
	}

}
