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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;

import ducks.controller.simulation.Simulation;
import ducks.controller.simulation.SimulationInstance;
import ducks.controller.simulation.SimulationStudy;
import ducks.misc.DucksException;
import ext.util.ExtendedProperties;
import ext.util.StringUtils;

/**
 * Storage provider LogFile
 * Writes all results to a file
 * 
 * @author Elmar Schoch
 *
 */
public class LogFile implements StorageProvider {
	
	// log4j Logger 
	private static Logger log = Logger.getLogger(LogFile.class.getName());
	
	// Configuration options relative to namespace
	public static final String CFG_FILENAME = "name";
	public static final String CFG_OPEN_APPEND = "append";

	
	private String filename;
	private boolean append;
	
	private PrintStream fileOutput;
	
	// Initialization, opening & closing ..........................................
	
	/**
	 * Configure module
	 * Whereas the name of the file is a mandatory parameter, the
	 * open mode is optional. If append is not configured, data is appended
	 * to an existing file by default.
	 */
	public void configure(ExtendedProperties config) throws DucksException {
		filename = config.getProperty(CFG_FILENAME);
		append   = config.getBooleanProperty(CFG_OPEN_APPEND, true);
		log.debug("Saving to file "+filename+", append="+append);
	}

	
	/**
	 * Disable module. Flushes and closes the file.
	 */
	public void disable() throws DucksException {
		if (fileOutput == null) return;
		
		try {
	       fileOutput.flush();
	       fileOutput.close();
        } catch (Exception e) {
	       
        }

	}

	/**
	 * Enable module. Opens the file for writing
	 */
	public void enable() throws DucksException {
		
		 try {
			 // Create file outputstream to filename, in append mode
			 FileOutputStream fileOut = new FileOutputStream(filename, append);
			 fileOutput = new PrintStream(fileOut, true);
		 } catch (Exception e) {
			 throw new DucksException("Opening result log file failed: "+e.getMessage());
         }
	}

	
	
	/**
	 * Save simulation to the file.
	 */
	public void saveSimulation(Simulation simu) throws DucksException {
		
		// Save configuration first
		// what do we need to save
		ExtendedProperties config = simu.getSimuConfig();
		config = config.getFilteredSet(StorageProvider.CFG_EXCLUDE_PARAMS);
		String excParams = config.getProperty(StorageManager.CFG_EXCLUDE_PARAMS) ;
		String incParams = config.getProperty(StorageManager.CFG_INCLUDE_PARAMS);
		// NOTE: incParams and excParams may be null intentionally!! 
		ExtendedProperties fields = config.getFilteredSet(incParams, excParams);
		
		fileOutput.println("Simulation ........................................");
		fileOutput.println("Study: "+simu.getSimStudy().getIdentifier());
		fileOutput.println("Simu: "+simu.getIdentifier());
		fileOutput.println("Configuration .....................................");
		
		fileOutput.println(fields.saveToSortedString("\n"));
		fileOutput.flush();
		
        // save results
        String excResults = config.getProperty(StorageManager.CFG_EXCLUDE_RESULTS);
        String incResults = config.getProperty(StorageManager.CFG_INCLUDE_RESULTS);
        Vector<SimulationInstance> instances = simu.getFinishedInstances();
        
        fileOutput.println("Results ...........................................");
        for (int i=0; i < instances.size(); i++) {
        	SimulationInstance si = instances.get(i);
        	ExtendedProperties results = si.getResults();
        	results = results.getFilteredSet(incResults, excResults);
        	// save result values
        	fileOutput.println("Instance: "+i);
        	fileOutput.println(results.saveToSortedString("\n"));
        }
        fileOutput.flush();
	}


	/**
	 * Save study information to file
	 */
	public void saveSimulationStudy(SimulationStudy study)
	        throws DucksException {
		
		fileOutput.println("Simulation Study ..........................................");
		fileOutput.println("Date: "+ StringUtils.formatDateTime(new Date()));
		fileOutput.println("Description: "+study.getDescription());
		fileOutput.println("Identifier : "+study.getIdentifier());
		
		fileOutput.flush();
	}
	
}
