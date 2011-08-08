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

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ducks.misc.DucksException;
import ext.util.ExtendedProperties;

/**
 * Variable DUCKS Simulation multiplexer<br>
 * 
 * Approach to be more flexible in specifying simulation configurations
 * using variables. In the config file, there needs to be a namespace
 * like "ducks.var", and only this namespace is allowed to have multi-value
 * parameters. In the configuration namespaces (like ducks.nodes etc.),
 * multi-value parameters are not allowed, however, single-values are ok.
 * In addition, references to variables are ok as well. References
 * should look like "${varname}". For example, if a config parameter
 * would be "ducks.nodes.count", and a variable was "ducks.var.numnodes", 
 * then the reference should be "ducks.nodes.count=${numnodes}".
 * 
 * CAUTION: THIS CLASS IS NOT READY FOR USE! (only partly implemented so far)
 * 
 * @author Elmar Schoch
 */
public class VariableSimulationMultiplexer implements SimulationMultiplexer {
	
	// log4j Logger 
	private static Logger log = Logger.getLogger(VariableSimulationMultiplexer.class.getName());
	
	public static final String CFG_VAR_NAMESPACE = "ducks.var";
	
	protected ExtendedProperties baseConfig;
	protected ExtendedProperties singleValues = new ExtendedProperties();
	protected ExtendedProperties multiValues = new ExtendedProperties();
	

	/**
	 * Multiplexing method called from "outside"
	 * How this multiplexer works:
	 * - it requires a namespace ducks.var in the config which can contain values and
	 *   multi-values
	 * - "multi-values" are usually a comma-separated list of values, or a range with a certain
	 *   granularity, e.g. ducks.var.xyz=10,20,30,40,50 or ducks.var.xyz=10-50/10
	 *   This can also be mixed, e.g. 10,20,50-100/10
	 * - all other parameters (not under ducks.var namespace) will not be multiplexed, but can
	 *   contain references to these variables in the format
	 *   ducks.x.y.z=${ducks.var.xyz}
	 * - at the end of the multiplexing, variable references will be replaced
	 * 
	 * Extensions (currently not implemented)
	 * - variables (params under ducks.var) can also contain references to other parameters
	 *   (out of the complete config) 
	 */
	public List<ExtendedProperties> getSimulations(ExtendedProperties config) throws DucksException {

		this.baseConfig = config;
		log.debug("Start processing config");
		
		// multiplex variables
		Vector<ExtendedProperties> variables = processVars();
		Vector<ExtendedProperties> configurations = new Vector<ExtendedProperties>();
		
		// create complete property sets and replace variables 
		for(ExtendedProperties varset: variables) {
			
			ExtendedProperties simucfg = config.cloneProperties(); 
			try {
				simucfg.replaceVariables(varset);
            } catch (Exception e) {
	            throw new DucksException(e.getMessage());
            }
			
			configurations.add(simucfg);
		}
		
		return configurations;
	}
	
	/**
	 * Process variables, multiplex values and thus create a list of unique variable sets.
	 * @return The vector of unique variable sets
	 * @throws DucksException
	 */
	private Vector<ExtendedProperties> processVars() throws DucksException {
		
		ExtendedProperties vars = baseConfig.getNamespace(CFG_VAR_NAMESPACE,true);
		Enumeration keys = vars.keys();
		
		// separate variables into single and multi-values, and explode multi-values
		while( keys.hasMoreElements() ) {
			String k = (String) keys.nextElement();
			String v = vars.getProperty(k);
			
			if (v.indexOf(",") > 0 || (v.indexOf("-") > 0 && v.indexOf("/") > 0)) {
				// Multi-Value
				String exval = explodeMultiValue(v);
				multiValues.setProperty(k, exval);    			
				log.debug("Found multi-value variable: "+k+"="+v+"  => exploded: "+exval);
			} else {
				// Single-value
				singleValues.setProperty(k,v);
				log.debug("Found single value variable: "+k+" = "+v);
			}
		}
		
		// do multiplexing of multi-value variables
		Vector<ExtendedProperties> configurations = multiplexMultiValues(multiValues, new ExtendedProperties());
		
		// add single values to every configuration
		for(ExtendedProperties config: configurations) {
			config.putAll(singleValues);
		}
		
		return configurations;
	}
	 /**
     * Recursive assembly of simulations with multiple value parameters
     * (all combinations of all values in each multiple-value parameter are created)  
     * @param mvals Properties object that contains all multiple-value parameters. 
     * The discrete values of a multiple-value parameter must be divided by a ","
     * @param curVals Respresents the currently active combination of values. As long as curVals
     * doesn't contain a parameter value of each parameter in mvals, the recursion continues.
     * @return Vector of ExtendedProperties objects with distinct parameter set (all multi-values
     * resolved to single values)  
     */
    private Vector<ExtendedProperties> multiplexMultiValues(ExtendedProperties mvals, ExtendedProperties curVals) throws DucksException {
    	Vector<ExtendedProperties> result = new Vector<ExtendedProperties>();
    	    	
    	Enumeration keys = mvals.propertyNames();
    	while (keys.hasMoreElements()) {
    		String key = (String) keys.nextElement();
    		
    		if (curVals.getProperty(key) == null) {
    			log.debug("Handling mval "+key);
    			// Iterate through elem's value list and call this method recursive
    			String[] values = mvals.getProperty(key).split(",");
    			for (int i=0; i < values.length; i++) {
    				// For debugging only
    				//Logging.debugDetail("  current multi-value: "+key+" = "+values[i]);
    				//cKeys_debug.add(key); cVals_debug.add(values[i]);
    				// --
    				
    				curVals.setProperty(key,values[i]);
    				Vector<ExtendedProperties> v = multiplexMultiValues(mvals, curVals);
    				result.addAll(v);
    				
    				// For debugging only
        			//cKeys_debug.removeLast(); cVals_debug.removeLast();
        			// --
    			}
    			
    			curVals.remove(key);
    			break;
    		}
    	}
    	
    	return result;
    }
	
	
	/**
	 * Explodes a multi-value to a distinct, comma separated list
	 * Example: 10-50/10,100,200 -> 10,20,30,40,50,100,200
	 * @param val
	 * @return
	 */
	private static String explodeMultiValue(String val) {
		
		String[] vals;		
		String explodedval = "";
		
		val = val.trim();
		vals = val.split(",");
		for (int i=0; i < vals.length; i++) {
			// does current value contain a range definition?
			if (vals[i].indexOf("/") > 0) {
				// TODO: for multi-values, we currently support only integer values
				int step, start, stop;
				String[] mvp = vals[i].split("/");    					
				step = Integer.parseInt(mvp[1]);
				
				String[] range = mvp[0].split("-");
				start = Integer.parseInt(range[0]);
				stop = Integer.parseInt(range[1]);
				
				// step through the value range and create discrete values
				while (start <= stop ) {
					explodedval += start;
					start += step;
					if (start <= stop) explodedval += ",";
				}    					
			} else {
				// no range definition -> take directly
				explodedval += vals[i];
			}
			if ( i < vals.length-1 ) explodedval += ",";
		}
		
		return explodedval;		
	}
	
}
