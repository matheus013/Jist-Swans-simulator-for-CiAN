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

import java.util.Vector;

import org.apache.log4j.Logger;

import ext.util.ExtendedProperties;


/**
 * This class represents a single simulation (with unique parameter set)
 * 
 * @author Elmar Schoch
 */
public class Simulation {
	
	// log4j Logger 
	private static Logger log = Logger.getLogger(Simulation.class.getName());

	// Simulation states
    public static final int PENDING  = 1;
    public static final int RUNNING  = 2;
    public static final int FINISHED = 3;
    public static final int FLUSHED  = 4;  // indicates that simu results have been saved
    
    public static final String BEGIN_RESULTS = "----BEGIN-RESULTS----";
    public static final String END_RESULTS   = "----END-RESULTS----";
    
    // reference to "parent" simulation study
    private SimulationStudy study;
    
    // unique data for this simulation config
    private int identifier; 
    private ExtendedProperties simConfig;
    
    // Since a simulation with a unique parameter set should be processed
    // several times to get statistically meaningful values, each simulation
    // consists of several simulation instances.
    
    // simulation instance related
    private Vector<SimulationInstance> finishedSimInstances;
    private Vector<SimulationInstance> runningSimInstances;
    private int maxSimInstances;
    private int remainingSimInstances;
    
    // result db related
    private boolean resultsAreFlushed = false;
    
    
    public Simulation(SimulationStudy study) {
    	
    	this.study = study;
    	
    	finishedSimInstances = new Vector<SimulationInstance>();
    	runningSimInstances = new Vector<SimulationInstance>();
    	remainingSimInstances = maxSimInstances = this.study.getInstanceCount();
    }

    public ExtendedProperties getSimuConfig() {
        return simConfig;
    }

    public void setSimuConfig(ExtendedProperties simConfig) {
        this.simConfig = simConfig;
    }
    
    
    public int getProcessedInstances() {
    	return maxSimInstances - remainingSimInstances;
    }
    
    /**
     * Get the simulation state. This may be
     * <li> pending: Simulation has not been processed at all
     * <li> running: At least one instance of the simulation is currently beeing processed
     * <li> finished: Simulation has been processed completely (i.e. all instances)
     * <li> flushed: Simulations results have been saved to database.
     * Note: the state "flushed" may never be reached when no saving is set.
     * @return state constant
     */
    public synchronized int getState() {    	
        int s = 0;
    	if (remainingSimInstances == maxSimInstances) s = PENDING;
        if (remainingSimInstances < maxSimInstances ) s = RUNNING;
        if (remainingSimInstances == 0 && runningSimInstances.size() == 0) s = FINISHED;
        if (resultsAreFlushed) s = FLUSHED;
    	
    	log.debug("State of simu "+getIdentifier()+": "+s+" (remaining: "+remainingSimInstances+
                  " running: "+runningSimInstances.size()+" flushed: "+resultsAreFlushed+")");
        
    	return s;
    }    
    
    /**
	 * Delivers an array containing numbers of instances, aggregated according
	 * to their state. E.g., index 0 = number of remaining instances, 1 = running, 2 = done, 3 = failed
	 * @return array containing instance states
	 */
	public int[] getStateInfo() {
		int[] states = new int[4];
		states[0] = remainingSimInstances;
		for (int i=1; i < 3; i++) {
			states[i] = 0;
		}
		states[1] = runningSimInstances.size();
		for (int i=0; i < finishedSimInstances.size(); i++) {
			SimulationInstance si = (SimulationInstance) finishedSimInstances.get(i);
			states[si.getState()]++;
		}
		
		return states;
	}
    
	public synchronized void setFlushed() {
		if (! resultsAreFlushed && remainingSimInstances == 0 && runningSimInstances.size() == 0) {
			resultsAreFlushed = true;
		}
	}
	
	/**
	 * Retrieve a simulation instance, if still one has to be processed
	 * @return
	 */
    public synchronized SimulationInstance getFreeInstance() {
    	
    	if (remainingSimInstances == 0) {
    		return null; 
    	} else {
    	
    		SimulationInstance newSim = new SimulationInstance(this,runningSimInstances.size()+1);
    		runningSimInstances.add(newSim);
    		remainingSimInstances--;
    		
    		return newSim;
    	}
    }
       
    public String[] getArgs() {
    	String[] args = new String[3];
    	args[0] = this.study.getWrapper();
    	args[1] = "-s";
    	args[2] = this.simConfig.saveToString();
    	return args;
    }
    
    public String getSim() {
    	return "jist.swans.Main";
    }


	/**
	 * Call, if an error occured with the link to the processing server
	 * (i.e. instance was set up, but not actually processed)
	 * @param si
	 */
	public void recallInstance(SimulationInstance si) {
		log.debug("Recalling instance of simulation "+getIdentifier());
		remainingSimInstances++;
		runningSimInstances.remove(si);
	}
	
	/**
	 * Call, if simu instance has completed (i.e. could be successfully
	 * executed on server, regardless whether simu produced errors
	 * or completed normally)
	 * @param si
	 */
	public void finalizeInstance(SimulationInstance si) {
		log.debug("Finalizing instance of simulation "+getIdentifier());
		runningSimInstances.remove(si);
		finishedSimInstances.add(si);		
	}
	
	
	/*
	public synchronized void saveResults(Connection con, long simRunID) throws SQLException, DucksException {
		
		// Assure that results aren't saved twice
		if (resultsAreFlushed) return;
		
		log.info("Flushing simu "+getIdentifier()+" to database ...");
		
		// Save simulation config (input params)
		long cfgID = saveConfig(con, simRunID);
		
		// Save computed simulation result values (for each SimInstance)
		for (int i=0; i < finishedSimInstances.size(); i++) {
			SimulationInstance si = (SimulationInstance) finishedSimInstances.get(i);
			if (si.getState() == SimulationInstance.DONE) {
				si.getResultDescriptor().setProperty("ducks.config.db.results.exclude",simDescriptor.getProperty("ducks.config.db.results.exclude"));
				si.getResultDescriptor().setProperty("ducks.config.db.results.include",simDescriptor.getProperty("ducks.config.db.results.include"));
				si.saveResults(con,cfgID, simRunID);
			} else {
				log.warn("Skipped saving failed simulation instance to database! (runID: "+cfgID+", cfgID: "+cfgID+")");
			}
		}
		resultsAreFlushed = true;
		
		// Release some memory (textual simu results, ...)
		for (int i=0; i < finishedSimInstances.size(); i++) {
			SimulationInstance si = (SimulationInstance) finishedSimInstances.get(i);
			si.releaseMemory();
		}
		
		System.gc();
	}
	
	private long saveConfig(Connection con, long simRunID) throws SQLException, DucksException {
		
		// Save general table 
		String q = "INSERT INTO "+DBManager.DB_SIMU_CONF+" SET RunID="+simRunID+";";
		
		Statement s = con.createStatement();
		s.execute(q,Statement.RETURN_GENERATED_KEYS);
		
		ResultSet rs = s.getGeneratedKeys();
		rs.next();
		long configID = rs.getLong(1);
		
		// Save config tables
		simDescriptor.saveToDB(con, configID);
		
		return configID;
	}
	
	*/

	public Vector<SimulationInstance> getFinishedInstances() {
		return (Vector<SimulationInstance>) finishedSimInstances.clone();
	}
	
    /**
	 * @return Returns the identifier.
	 */
	public int getIdentifier() {
		return identifier;
	}

	/**
	 * @param identifier The identifier to set.
	 */
	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * Returns the simulations "parent" run
	 * @return parent simulation run
	 */
	public SimulationStudy getSimStudy() {
		return this.study;
	}
	
}
