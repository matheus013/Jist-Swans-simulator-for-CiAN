/*
 * Ulm University DUCKS project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */

package ducks.driver;

import jargs.gnu.CmdLineParser;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;

import jist.runtime.JistAPI;
import jist.swans.Constants;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ducks.eventlog.EventLog;
import ducks.misc.DucksException;
import ext.util.ExtendedProperties;
import ext.util.MersenneTwister;
import ext.util.StringUtils;

/**
 * This class represents the base for drivers in DUCKS. In the model, the driver
 * is capable of taking up configuration properties and giving it to a driver
 * class. Moreover, it expects the driver class to return a list of result
 * parameters as well as the corresponding values at the end of a simulation.
 * 
 * Caution: for productive simulations, take care that the debug output of the
 * driver is deactivated. Otherwise, with a high number of compute servers, this
 * may consume really much memory or even overload the network interface queues
 * on the DUCKS controller machine.
 * 
 * Note: The main method knows two types of being called: direct and via DUCKS
 * controller 1. When used directly, the driver can be called via SWANS, like
 * any other driver, except that it parses the properties file given at the
 * command line, which holds all (unique!!) configuration parameters. In this
 * case, the whole DUCKS controller engine with multiplexer, servers and storage
 * is not involved at all 2. When used by the DUCKS engine, all the required
 * configuration properties are
 * 
 * @author Elmar Schoch
 * 
 */
public class Main implements DucksDriverModule, Runnable
{

    /**
     * Main method.
     * 
     * @param args
     *            Contains complete set of simulation parameters
     */
    public static void main(String[] args) {

        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option optSimuCfgFile = parser.addStringOption('f', "configfile");
        CmdLineParser.Option optSimuCfgStream = parser.addStringOption('s', "configstream");
        CmdLineParser.Option optDebug = parser.addStringOption('d', "debugconfig");

        try {
            parser.parse(args);
        } catch (Exception e) {
            System.out.println(">>> Error parsing commandline options: " + e.getMessage());
        }

        if (parser.getOptionValue(optDebug) != null) {
            File f = new File(parser.getOptionValue(optDebug).toString());

            System.out.println(">>> Starting logging with file " + f.getAbsoluteFile());
            Logger.getRootLogger().setLevel(Level.OFF);
            PropertyConfigurator.configure((parser.getOptionValue(optDebug)).toString());
        } else {
            System.out.println(">>> Logging is deactivated");
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.OFF);
        }

        String configFile = null;
        String configStream = null;
        // Direct call (without DUCKS controller)
        if (parser.getOptionValue(optSimuCfgFile) != null) {
            configFile = (parser.getOptionValue(optSimuCfgFile)).toString();
        }
        // Call via DUCKS controller
        if (parser.getOptionValue(optSimuCfgStream) != null) {
            configStream = (parser.getOptionValue(optSimuCfgStream)).toString();
        }

        if (configFile == null && configStream == null) {
            System.out.println(">>> Neither config file nor config stream is available. Exiting ...");
            return;
        }

        // 0. Prepare simulation setup ..............................
        // -> options in Properties format

        ExtendedProperties options = new ExtendedProperties();
        try {
            if (configStream != null) {
                System.out.println(">>> Config stream: " + configStream);
                options.loadFromString(configStream, ExtendedProperties.DEFAULT_SEPARATOR);
            } else {
                options.loadFromFile(configFile);
            }
        } catch (Exception e) {
            System.out.println(">>> " + e.getMessage());
            System.out.println(">>> Loading options failed. Exiting ...");
            return;
        }

        // System.out.println(">>> Options: "+options);

        Enumeration os = options.propertyNames();
        if (!os.hasMoreElements()) {
            System.out.println(">>> No options available. Exiting ...");
            return;
        }

        // 1. Setup simulation ............................................

        Main simuMain = new Main();

        try {
            simuMain.configure(options);

        } catch (Exception e) {
            System.out.println(">>> Simulation setup failed: " + e.getMessage());
            simuMain.cancel();
            return;
        }

        // 2. Run simulation ..............................................

        System.out.println(">>> Start running simulation");
        try {
            simuMain.start();
        } catch (Exception e) {
            System.out.println("Starting simulation failed: " + e.getMessage());
        }
    }

    // Simu main object
    // ...........................................................

    private ExtendedProperties options;
    private Date               startTime;

    // The driver to use
    private DucksDriverModule  driver;

    public void configure(ExtendedProperties config) throws Exception {
        this.options = config;
        this.startTime = new Date();

        // Setup random number generator
        String seed = options.getProperty(SimParams.SIM_RANDOMSEED);
        if (seed == null) {
            Constants.random = new MersenneTwister(); /* Random(); */
        } else {
            try {
                Constants.random = new MersenneTwister(Long.parseLong(seed)); /*
                                                                               * Random
                                                                               * (
                                                                               * Long
                                                                               * .
                                                                               * parseLong
                                                                               * (
                                                                               * seed
                                                                               * )
                                                                               * )
                                                                               * ;
                                                                               */
            } catch (Exception e) {
                Constants.random = new MersenneTwister(0); /* Random(0); */
                System.out.println(">>> Random seed is not a valid long value (" + seed + "). Taking '0' instead.");
            }
        }

        // instantiate DucksDriver given in the config. Note that a driver must
        // implement the DucksDriver interface
        String driverClassname = config.getProperty(SimParams.SIM_CLASS);
        if (driverClassname == null) {
            throw new DucksException("DUCKS driver class is not specified in the configuration! (parameter "
                    + SimParams.SIM_CLASS);
        }
        try {
            Class driverClass = Class.forName(driverClassname);
            Object drv = driverClass.newInstance();
            if (!(drv instanceof DucksDriverModule)) {
                throw new DucksException("Driver " + driverClassname + " does not implement the DucksDriver interface");
            } else {
                driver = (DucksDriverModule) drv;
            }
        } catch (ClassNotFoundException cnfe) {
            throw new DucksException("DUCKS driver class not found: " + driverClassname);
        } catch (DucksException de) {
            throw de;
        } catch (Exception e) {
            throw new DucksException("DUCKS driver class could not be be instantiated: " + driverClassname);
        }

        driver.configure(config);
    }

    public ExtendedProperties getConfig() {
        return options;
    }

    /**
     * Run is scheduled to be executed at the end of the simulation, mainly to
     * read accumulated
     */
    public void run() {
        System.out.println(">>> Simu finished");
        System.out.println(this.getResults());
        System.out.flush();
        EventLog.finalizeLoggers();
    }

    public void start() throws Exception {
        // Schedule what will be executed at the end of the simulation
        long endTime = options.getLongProperty(SimParams.SIM_DURATION);
        if (endTime > 0) {
            JistAPI.endAt(endTime * Constants.SECOND);
        }
        JistAPI.runAt(this, JistAPI.END);
    }

    public void cancel() {
        JistAPI.end();
    }

    // Statistic collection routines
    // ..............................................
    /**
     * getResults reads, sums and formats all statistical information that is
     * collected during a simulation run.
     * 
     * 
     * @return String containing complete result value set in properties file
     *         format.
     */
    public String getResults() {

        ExtendedProperties jvmstats = this.getStats();
        ExtendedProperties simustats = this.driver.getStats();

        // ByteArrayOutputStream out = new ByteArrayOutputStream();
        // PrintStream ps = new PrintStream(out, true);

        String result = "----BEGIN-RESULTS----\n";
        // result += out.toString();
        result += jvmstats.saveToSortedString(ExtendedProperties.NEWLINE_SEPARATOR);
        result += "\n";
        result += simustats.saveToSortedString(ExtendedProperties.NEWLINE_SEPARATOR);
        result += "\n----END-RESULTS----\n";

        return result;
    }

    public ExtendedProperties getStats() {
        // general purpose information of environment (memory consumption,
        // elapsed time)
        System.gc();
        ExtendedProperties rd = new ExtendedProperties();
        rd.put("ducks.env.mem.free", Long.toString(Runtime.getRuntime().freeMemory()));
        rd.put("ducks.env.mem.max", Long.toString(Runtime.getRuntime().maxMemory()));
        rd.put("ducks.env.mem.total", Long.toString(Runtime.getRuntime().totalMemory()));
        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        rd.put("ducks.env.mem.used", Long.toString(usedMem));
        Date endTime = new Date();
        long elapsedTime = endTime.getTime() - startTime.getTime();
        rd.put("ducks.env.time.start", StringUtils.formatDateTime(startTime));
        rd.put("ducks.env.time.end", StringUtils.formatDateTime(endTime));
        rd.put("ducks.env.time.elapsed", Long.toString(elapsedTime));

        return rd;
    }

    /**
     * Return general stats for simulation (mem usage, ...)
     */
    public String[] getStatParams() {
        return getStatParameters();
    }

    private static String[] getStatParameters() {
        String[] stats = new String[7];
        stats[0] = "ducks.env.mem.free";
        stats[1] = "ducks.env.mem.max";
        stats[2] = "ducks.env.mem.total";
        stats[3] = "ducks.env.mem.used";
        stats[4] = "ducks.env.time.start";
        stats[5] = "ducks.env.time.end";
        stats[6] = "ducks.env.time.elapsed";
        return stats;
    }

}
