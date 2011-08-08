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
package ducks.controller.simulation;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ext.util.Configurable;
import ext.util.ExtendedProperties;

/**
 * SimulationStudy represents a set of simulation configurations. The set of simulations
 * can be loaded by a properties-file containing all steering parameters for them.
 * Multi-value parameters in the properties file are exploded combinatorically
 * to simulations with a unique parameter set.
 * 
 * @author Elmar Schoch
 *
 */

public class SimulationStudy implements Configurable {
	
	// log4j Logger 
	private static Logger log = Logger.getLogger(SimulationStudy.class.getName());
	
	public static final String CFG_SIM_RUNS     = "ducks.config.runs";
	public static final String CFG_SIM_DESC     = "ducks.config.desc";
	public static final String CFG_SIM_MUX      = "ducks.config.multiplexer.class";
	public static final String CFG_SIM_WRAPPER  = "ducks.config.wrapper.class";
	
	public static final String DEFAULT_MUX_CLASS     = "ducks.controller.simulation.DefaultSimulationMultiplexer";
	public static final String DEFAULT_WRAPPER_CLASS = "ducks.driver.Main";
	
	public static int idCounter = 1;
	
	// User-defined description of this study (for identification purposes)
	private String description   = "";
	
	// Wrapper to use. The wrapper is the DUCKS driver, i.e. the main class for a simulation 
	// Usually always "ducks.driver.Main", because only this class "knows" how to handle
	// parameters coming from DUCKS
	private String wrapper = DEFAULT_WRAPPER_CLASS;
	
	// Number of instances to process for each simulation configuration
	private int    simuInstances = 1;	
	
	// current ID in the result database
	private long   simRunID = -1; 
	
	// list of unique simulations
	private Vector<Simulation> simus = new Vector<Simulation>();
	
	// Loading ....................................................................
	
	public SimulationStudy() {
		simRunID = idCounter++;
	}
	
	
	public void configure(ExtendedProperties config) throws Exception {
		
		simuInstances = config.getIntProperty(CFG_SIM_RUNS);
		description   = config.getStringProperty(CFG_SIM_DESC);
		wrapper       = config.getStringProperty(CFG_SIM_WRAPPER, DEFAULT_WRAPPER_CLASS);
	    
		// instantiate simulation multiplexer which creates single simulations out of the
		// config. DUCKS does not care how this is done
		String muxname = config.getProperty(CFG_SIM_MUX);
		if (muxname == null) muxname = DEFAULT_MUX_CLASS;
		
		SimulationMultiplexer mux = null;
		try {
	        Class muxclass = Class.forName(muxname);
	        mux = (SimulationMultiplexer) muxclass.newInstance();
        } catch (Exception e) {
        	e.printStackTrace();
	        throw new Exception("Simu config multiplexer class could not be loaded: "+e.getMessage());
        }
        
       	log.debug("Trying to multiplex simulations");
        List<ExtendedProperties> configSet = mux.getSimulations(config);
       	
       	
       	// create simus
    	for (int i=0; i < configSet.size(); i++) {
			Simulation s = new Simulation(this);
			s.setIdentifier(i);
			ExtendedProperties simuconfig = configSet.get(i);
			s.setSimuConfig(simuconfig);
			simus.add(s);
			log.debug("Simu "+s.getIdentifier()+": "+s.getSimuConfig().saveToString());
		}
		
    	log.info("Loaded "+configSet.size()+" simulations");
    }

	
    
   
    
    /**
     * Retrieve a simulation that still has pending instances to execute 
     * @return
     */
    public synchronized SimulationInstance getPendingSimInstance() {
    	SimulationInstance theSimu = null;
    	for (int i=0; i < simus.size(); i++) {
    		Simulation simu = (Simulation) simus.get(i);
    		SimulationInstance inst = simu.getFreeInstance();
    		if (inst != null) {
    			theSimu = inst;
    			break;
    		}
    	}
        return theSimu;
    }

    /** 
     * check if this simulation run has finished all simulations yet.
     * Note: Method returns immediately after encountering an unfinished simulation 
     * @return 
     */
    public boolean hasFinished() {
    	
    	boolean done = true;
		for (int i=0; i < simus.size(); i++) {
			Simulation s = (Simulation) simus.get(i);
			if ( s.getState() < Simulation.FINISHED) {
				done = false;
				break;
			}
		}
		return done;
    }

    
    /**
     * Save all results to database (including all single simulations)
     * @param db
     * @throws DucksException
     * @throws SQLException
     */
    /*
    public void saveAll(Connection con) throws DucksException, SQLException {
    	
    	long simRunID = this.save(con);
    	
    	for (int i=0; i < simus.size(); i++) {
    		Simulation s = (Simulation) simus.get(i);
    		s.saveResults(con, simRunID);
    	}
    }
    */
    
    
    /**
     * Retrieve all simulations that have finished, but are not saved yet.
     * The method does not need to be synchronized because only one thread
     * accesses it (i.e. the background saving thread)
     * @return
     */
    public synchronized Vector<Simulation> getUnsavedFinishedSimulations() {
    	Vector<Simulation> ufsims = new Vector<Simulation>();
    	for (int i=0; i < simus.size(); i++) {
    		Simulation s = (Simulation) simus.get(i);
    		if (s.getState() == Simulation.FINISHED ) {
    			ufsims.add(s);
    		}
    	}
    	return ufsims;
    }

    public long getIdentifier() {
    	return simRunID;
    }
    
    public String getDescription() {
    	return description;
    }

    public String getWrapper() {
    	return wrapper;
    }
    
    public int getInstanceCount() {
    	return simuInstances;
    }
    
    public Vector<Simulation> getSimulations() {
    	return simus;
    }
    
    /**
     * Retrieve an array containing the number of simulations for
     * each simulation state (i.e. PENDING, RUNNING, FINISHED, FLUSHED)
     * @return
     */
    public int[] getStateCount() {
    	
    	int[] counter = new int[5];
    	for (int i=0; i < 5; i++) {
    		counter[i] = 0;
    	}
    	for (int i=0; i < simus.size(); i++) {
    		Simulation s = (Simulation) simus.get(i);
    		counter[s.getState()]++;
    	}
    	
    	return counter;
    }

	
}
