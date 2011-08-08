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
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ducks.misc.DucksException;
import ext.util.ExtendedProperties;

/**
 * Default DUCKS Simulation multiplexer<br>
 * Function: 
 * Create unique set of simulations out of the config file
 * DUCKS is not interested, how this is done, as long as the multiplexer works like
 * the user wants it to.<br>
 * 
 * The syntax of this multiplexer is as follows:
 * - the parameter value may be a single value, i.e. a.b.c=5
 * - parameter value may be a multi-value, which in turn may have the syntax
 *   - a.b.c=5,10,15,20
 *   - a.b.c=5-20;5
 *   In both cases 4 simulations with these values will be done; in the second
 *   value, a notation indicating regular steps is used. This can also be combined,
 *   e.g. a.b.c=5-20;5,30,40. However, such ranges currently only work with integers,
 *   whereas the comma-separated list can contain strings of any kind (except commas ;)
 * - advanced values refer to other parameters (see documentation vor AdvancedValue classes)
 * 
 * Note: Some reserved characters like "," and ";" must not occur in values
 * 
 * Note: the multiplexer is only used within the SimulationStudy object, which means that
 *       it can not interfere with parameters drawn from the config before that point. 
 * 
 * @author Elmar Schoch
 */
public class DefaultSimulationMultiplexer implements SimulationMultiplexer {
	
	// log4j Logger 
	private static Logger log = Logger.getLogger(DefaultSimulationMultiplexer.class.getName());
	
	
	protected ExtendedProperties baseConfig;
	protected ExtendedProperties singleValues = new ExtendedProperties();
	protected ExtendedProperties multiValues = new ExtendedProperties();
	protected ExtendedProperties advancedValues = new ExtendedProperties();
	
	private LinkedList<String> cKeys_debug = new LinkedList<String>();  // for debugging only
	private LinkedList<String> cVals_debug = new LinkedList<String>();  // -"-
	

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
		Vector<ExtendedProperties> configurations = processConfig();
		
		return configurations;
	}
	
	/**
	 * Process config to demultiplex multi-values and thus create a list of unique variable sets.
	 * @return The vector of unique variable sets
	 * @throws DucksException
	 */
	private Vector<ExtendedProperties> processConfig() throws DucksException {
		
    	// Exclude all DUCKS config related params
    	ExtendedProperties simuConfig = baseConfig.getFilteredSet(SimulationMultiplexer.CFG_EXCLUDE_PARAMS);
    	
		Enumeration keys = simuConfig.keys();
		
		// separate variables into single and multi-values, and explode multi-values
		while( keys.hasMoreElements() ) {
			String k = (String) keys.nextElement();
			String v = simuConfig.getProperty(k).trim();
			
			
			// Distinguish between advanced, multi and single values
    		if (v.startsWith("(") ) {
    			log.debug("ConfigParser: Found advanced value: "+v);
    			// Advanced value
    			AdvancedValue av;
    			if (v.indexOf("?") > 0) {
    				av = new ConditionalValue();
    			} else if (v.indexOf("<->") > 0) {
    				av = new ParallelValue();
    				((ParallelValue) av).setDescriptor(simuConfig);
    			} else if (v.indexOf("->") > 0) {
    				av = new ReferenceValue();
    			} else {
    				// unknown advanced value 
    				continue; 
    			}
    			av.setParamName(k);
    			av.init(v);
    			advancedValues.put(k, av);
    			
    		} else if (v.indexOf(",") > 0 || (v.indexOf("-") > 0 && v.indexOf("/") > 0)) {
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
		
		// assemble final configurations
		Vector<ExtendedProperties> configurations;
		if (multiValues.size() == 0 && advancedValues.size() == 0) {
    		// exactly 1 simple simulation
			configurations = new Vector<ExtendedProperties>(1,50);
			configurations.add(simuConfig);
    	} else {
    		// do multiplexing of multi-values
    		configurations = assembleSimus(singleValues, multiValues, advancedValues, new ExtendedProperties());
    	}
		
		return configurations;
	}
	
	
    /**
     * recursive assembly of simulations with multiple value parameters
     * (all combinations of all values in each multiple-value parameter are created)  
     * @param mvals Properties object that contains all multiple-value parameters. 
     * The discrete values of a multiple-value parameter must be divided by a ","
     * @param curVals Respresents the currently active combination of values. As long as curVals
     * doesn't contain a parameter value of each parameter in mvals, the recursion continues.
     * @param avals Properties object containing all advanced value parameters. Note that for
     * a (key,value) pair in avals, value is an object of type AdvancedValue !!!
     * @return Vector of simulation descriptors with distinct parameter set (all multi-values
     * and advanced values resolved to single values)  
     */
    private Vector<ExtendedProperties> assembleSimus(ExtendedProperties svals, ExtendedProperties mvals, 
    		ExtendedProperties avals, ExtendedProperties curVals) throws DucksException {
    	
    	Vector<ExtendedProperties> result = new Vector<ExtendedProperties>();
    	boolean foundMVal = false;
    	
    	Enumeration keys = mvals.propertyNames();
    	while (keys.hasMoreElements()) {
    		String key = (String) keys.nextElement();
    		
    		if (curVals.getProperty(key) == null) {
    			foundMVal = true;
    			// Iterate through elem's value list and call this method recursive
    			String[] values = mvals.getProperty(key).split(",");
    			for (int i=0; i < values.length; i++) {
    				// For debugging only
    				//Logging.debugDetail("  current multi-value: "+key+" = "+values[i]);
    				cKeys_debug.add(key); cVals_debug.add(values[i]);
    				// --
    				
    				curVals.setProperty(key,values[i]);
    				Vector<ExtendedProperties> v = assembleSimus(svals,mvals,avals,curVals);
    				result.addAll(v);
    				
    				// For debugging only
        			cKeys_debug.removeLast(); cVals_debug.removeLast();
        			// --
    			}
    			
    			
    			curVals.remove(key);
    			break;
    		}
    	}
    	
    	if (foundMVal) return result;
    	
    	curVals.putAll(svals);
    	
    	boolean foundAVal = false;
    	keys = avals.propertyNames();
    	
    	if (foundMVal == false) {
    		while (keys.hasMoreElements()) {
    			String key = (String) keys.nextElement();
    			if (curVals.getProperty(key) == null) {
    				foundAVal = true;
        			try {
        				AdvancedValue av = (AdvancedValue) avals.get(key);
            			String val = av.getValue(curVals);
            			
            			String[] values = val.split(",");
            			
            			for (int i=0; i < values.length; i++) {
            				// For debugging only
            				// Logging.debugDetail("  current advanced value: "+key+" = "+values[i]);
            				cKeys_debug.add(key); cVals_debug.add(values[i]);
            				// --
            				
            				curVals.setProperty(key,values[i]);
            				Vector<ExtendedProperties> v = assembleSimus(svals,mvals,avals,curVals);
            				result.addAll(v);
            				
        	    			// For debugging only
        	    			cKeys_debug.removeLast(); cVals_debug.removeLast();
        	    			// --
            			}
            			
					} catch (Exception e) {
						log.error("Error during simu assembly (advanced values): "+e.getMessage());
						throw new DucksException("Error in simu assembly - "+e.getMessage());
					}
					
					curVals.remove(key);
					break;
    			}
    		}
    	}
    	
    	if (foundAVal) return result;
    	
    	
    	ExtendedProperties sd = new ExtendedProperties();
    	sd.putAll(curVals);
    	result.add(sd);
    	
		// For debugging only
    	
//    	if (log.isDebugEnabled()) {
//    		System.out.print("  ");
//    		for (int i=0; i < cKeys_debug.size(); i++) {
//    			System.out.print((String)cKeys_debug.get(i) + " = " + (String) cVals_debug.get(i) + "  ");
//    		}
//    		System.out.print("\n");
//    	}
    	
		// --
    	
    	return result;
    }
	
	
	/**
	 * Explodes a multi-value to a distinct, comma separated list
	 * Example: 10-50;10,100,200 -> 10,20,30,40,50,100,200
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
			if (vals[i].indexOf(";") > 0) {
				// TODO: for multi-values, we currently support only integer values
				int step, start, stop;
				String[] mvp = vals[i].split(";");    					
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
	
	// Advanced value classes ......................................................
	
	/**
	 * Abstract base class for AdvancedValue classes
	 */
	public abstract class AdvancedValue {
		
		protected String thisParam;
		protected String correspondingParam;
		
		public void setParamName(String thisParam) {
			this.thisParam = thisParam;
		}
		
		public abstract void init(String valueString) throws DucksException;
		
		/**
		 * Returns single value (ParallelValue and ReferenceValue) or 
		 * (eventually) multi value (ConditionalValue)
		 *  
		 * @param currentDescriptor
		 * @return
		 * @throws DucksException
		 */
		public abstract String getValue(ExtendedProperties currentDescriptor) throws DucksException;
	}
	
	/**
	 * Conditional value: Define param value(s) depending on other param value:
	 * Example: ducks.general.fieldsize.y=(ducks.general.fieldsize.x == 500 ? 500 : 1000)
	 * Syntax : '(' <corresponding param> <comparator> <single-value> '?' <multi-value> ':' <multi-value> ')'
	 * - comparator may be '==', '>', '>=', '<', '<='
	 * - spaces are optional
	 * 
	 * @author Elmar
	 *
	 */
	public class ConditionalValue extends AdvancedValue {
		
		private String comparator;
		private long compValue;
		private String ifTrue;
		private String ifFalse;

		public void init(String valueString) throws DucksException {
			
			valueString = valueString.substring(valueString.indexOf("(")+1);
			valueString = valueString.substring(0,valueString.lastIndexOf(")"));
			valueString.trim();
			
			String[] parts = valueString.split("\\?");
			String cond, vals;
			try {
				cond = parts[0].trim();
				vals = parts[1].trim();
				
				String[] sarr = vals.split(":");
				ifTrue = explodeMultiValue(sarr[0].trim());
				ifFalse = explodeMultiValue(sarr[1].trim());
				
				if (cond.indexOf("==") > 0) {
					comparator = "==";
				} else if (cond.indexOf(">=") > 0) {
					comparator = ">=";
				} else if (cond.indexOf(">") > 0) {
					comparator = ">";
				} else if (cond.indexOf("<=") > 0) {
					comparator = "<=";
				} else if (cond.indexOf("<") > 0) {
					comparator = "<";
				} else throw new DucksException("Unknown comparator");
				
				sarr = cond.split(comparator);
				correspondingParam = sarr[0].trim();
				compValue = Integer.parseInt(sarr[1].trim());
				
				log.debug("  Conditional param: "+thisParam+" = ("+correspondingParam+" "+comparator+" "+compValue+" ? "+ifTrue+" : "+ifFalse+")");
				
			} catch( Exception e) {
				throw new DucksException("Error parsing advanced value: "+valueString+" : "+e.getMessage());
			}
		}
		
		public String getValue(ExtendedProperties currentDescriptor) throws DucksException {
			
			int v = Integer.parseInt(currentDescriptor.getProperty(correspondingParam));
			boolean result = true;
			if (comparator.equals("==")) {
				result = v == compValue;
			} else if (comparator.equals(">=")) {
				result = v >= compValue;
			} else if (comparator.equals(">")) {
				result = v > compValue;
			} else if (comparator.equals("<=")) {
				result = v <= compValue;
			} else if (comparator.equals("<")) {
				result = v < compValue;
			}
			
			if (result) return ifTrue;
			else return ifFalse;
		}
		
	}
	 
	/**
	 * Reference value: Define value identical to other param value:
	 * Example: ducks.general.fieldsize.y=(-> ducks.general.fieldsize.x)
	 * Syntax : '(->' <corresponding param> ')'
	 * - spaces are optional
	 * 
	 * @author Elmar
	 *
	 */
	public class ReferenceValue extends AdvancedValue {

		public void init(String valueString) throws DucksException {
			valueString = valueString.substring(valueString.indexOf("(")+1);
			valueString = valueString.substring(0,valueString.lastIndexOf(")"));
			valueString.trim();
			
			correspondingParam = valueString.substring(valueString.indexOf("->")+2).trim();
			log.debug("  Reference param: "+thisParam+" points to "+correspondingParam);
		}

		public String getValue(ExtendedProperties currentDescriptor) throws DucksException {
			
			//Logging.debugDetail("ReferenceValue.getValue: corresponding param: "+correspondingParam+" = "+currentDescriptor.getProperty(correspondingParam));
			
			return currentDescriptor.getProperty(correspondingParam);
		}
		
	}
	
	/**
	 * Parallel value: Define values in parallel to other param values 
	 * -> only makes sense with multiple value params
	 * Example: ducks.general.fieldsize.x=1000,1500,2000
	 *          ducks.general.fieldsize.y=(ducks.general.fieldsize.x <-> 500,1000,1500)
	 *          -> field sizes finally are 1000x500, 1500x1000, 2000x1500
	 * Syntax : '(' <corresponding param> '<->' <multi-value> ')'
	 * Important: 
	 *  - corresponding param and this param always must have the same number of values !!!!
	 *  - corresponding param must not be an advanced value
	 * 
	 * 
	 * @author Elmar
	 *
	 */
	public class ParallelValue extends AdvancedValue {
		
		private String myValues;
		private String cValues;
		// temporarily needed, to fetch values from corresponding param in order
		// to be able to correlate a value later in getValue.
		private ExtendedProperties params; 
		
		public void setDescriptor(ExtendedProperties params) {
			this.params = params;
		}

		public void init(String valueString) throws DucksException {
			valueString = valueString.substring(valueString.indexOf("(")+1);
			valueString = valueString.substring(0,valueString.lastIndexOf(")"));
			valueString.trim();
			
			String[] parts = valueString.split("<->");
			correspondingParam = parts[0].trim();
			
			cValues = explodeMultiValue(params.getProperty(correspondingParam));
			myValues = explodeMultiValue(parts[1].trim());
			
			// release params pointer (no longer needed)
			params = null; 
			
			log.debug("  Parallel param: "+thisParam+" = ("+correspondingParam+" ["+cValues+"] <-> ["+myValues+"])");
		}

		public String getValue(ExtendedProperties currentDescriptor) throws DucksException {
			
			String curVal = currentDescriptor.getProperty(correspondingParam);
			String[] cVals = cValues.split(",");
			String[] mVals = myValues.split(",");
			
			for (int i=0; i < cVals.length; i++) {
				if (curVal.equals(cVals[i])) {
					return mVals[i];
				}
			}
			// if nothing is found (i.e. because number of values is not equal), throw exception
			throw new DucksException("Didn't find parallel value: this: "+thisParam+"="+myValues+
					                 ", corresponding param: "+correspondingParam+"="+cValues);
		}
		
		
		
	}
}
