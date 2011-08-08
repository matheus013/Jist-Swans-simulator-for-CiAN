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

import java.io.ByteArrayOutputStream;

import org.apache.log4j.Logger;

import ducks.misc.DucksException;
import ext.util.ExtendedProperties;

/**
 * Simulation instance represents one atomic simulation execution. Since
 * every unique simulation normally needs to be executed several times to produce
 * statistically meaningful results, it consists of a user-defined number of
 * simulation instances.
 *   
 * @author Elmar Schoch
 *
 */
public class SimulationInstance {
	
	// log4j Logger 
	private static Logger log = Logger.getLogger(SimulationInstance.class.getName());
	
	private Simulation simu;
	
	public static final int RUNNING = 1; 
	public static final int DONE = 2;
	public static final int FAILED = 3;
	
	// SimInstance state 
	private int state;   	
	
	// Simulation instance number
	private int instanceNum;
	
	// the output produced by the simulation
	// Caution: for productive simulations, take care that the debug output
	// of the driver is deactivated. Otherwise, with a high number of compute
	// servers, this may consume really much memory or even overload the
	// network interface queues.
	private ByteArrayOutputStream simOutput;
	
	private ExtendedProperties result;
		
	public SimulationInstance(Simulation parentSimu, int instanceNumber) {
		simu = parentSimu;
		simOutput = new ByteArrayOutputStream();
		state = Simulation.RUNNING;
		instanceNum = instanceNumber;
	}
	
	/**
	 * Parse simulation output and extract result section
	 * @throws DucksException
	 */
	public void parseResults() throws DucksException {
		
		String output = simOutput.toString();
		
		log.debug("\n8< ->>---------------------------------------------------\n"+
			      output +
				  "8< -<<---------------------------------------------------\n");
		
		int begin = output.indexOf(Simulation.BEGIN_RESULTS);
		int end = output.indexOf(Simulation.END_RESULTS, begin);
		
		if (begin == -1 || end == -1 ) {
			finalize(FAILED);
			log.debug("Simulation failed. See output for details");
			throw new DucksException("Simulation results not valid (no result section)");
		}
		
		String results;
		try {
			results = output.substring(begin+Simulation.BEGIN_RESULTS.length(),end-1);
		} catch (Exception e) {
			log.warn("Failed to parse result properties:\n"+output);
			finalize(FAILED);
			throw new DucksException("Simulation results could not be parsed (substring)");
		}
		
		
		ExtendedProperties rd = new ExtendedProperties();
		try {
			rd.loadFromString(results);
		} catch (Exception e) {
			log.warn("Failed to load result properties:\n"+results);
			finalize(FAILED);
			throw new DucksException("Loading results into ResultDescriptor failed");
		}
		
		finalize(DONE);
		result = rd;		
	}

	/**
	 * Finalize simulation: simulation instance has succeeded or failed
	 * definitely. This won't change any more.
	 * @param newState DONE or FAILED
	 */
	private void finalize(int newState) {
		state = newState;
		simu.finalizeInstance(this);
	}
	
	/**
	 * Call, if sim instance could not be executed as intended, e.g. because
	 * RMI link to server was broken. This means of course, that the instance
	 * has to be executed again, whereas it must not be repeated when a
	 * simulation internal failure occured that is not due to DUCKS
	 * (in this case, finalize(FAILED) would be used)
	 *
	 */
	public void recall() {
		simu.recallInstance(this);
	}
	
	// Getters .............................................	
	
	public ExtendedProperties getResults() {
		return result;
	}
	
	public String getOutput() {
		return simOutput.toString();
	}
	
	public ByteArrayOutputStream getOutputStream() {
		return simOutput;
	}

	public int getState() {
		return state;
	}

	// Pass-through methods for Simulation .................
	
	public ExtendedProperties getSimuConfig() {
		return simu.getSimuConfig();
	}
	
	public int getIdentifier() {
		return simu.getIdentifier();
	}

	public String getSim() {
		return simu.getSim();
	}
	
	public int getNumber() {
		return instanceNum;
	}
	
	public String[] getArgs() {
		String[] args = simu.getArgs();
		// TODO: Little hack to add the current instance number to the properties sent to the JiST server
		//args[1] = args[1].concat(ExtendedProperties.DEFAULT_SEPARATOR+"ducks.simulation.instance.num="+instanceNum);
		return args;
	}
	
	public Simulation getSimulation() {
		return simu;
	}
	
	// DB ....................................................
	
	/*
	public void saveResults(Connection con, long config, long run) throws SQLException, DucksException {
		
		if (con == null) throw new DucksException("No connection to db");
		
		// Add entry to simu list
		String q = "INSERT INTO "+DBManager.DB_SIMU_LIST+" (RunID,ConfigID) VALUES ("+run+","+config+")";
		Statement s = con.createStatement();
		s.execute(q,Statement.RETURN_GENERATED_KEYS);
		
		ResultSet rs = s.getGeneratedKeys();
		rs.next();
		long simuID = rs.getLong(1);
		
		if (result != null) {
			result.saveToDB(con, simuID);
		}
	}
	*/
	
	/**
	 * Allow Java to garbage collect the memory consumed by the result
	 * variables by setting their pointers to null.
	 * This is done after saving a result successfully to the result database
	 * in the Simulation class.
	 */
	public void releaseMemory() {
		simOutput = null;
		result = null;
	}
	
}
