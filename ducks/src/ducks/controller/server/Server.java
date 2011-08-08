/*
 * Ulm University DUCKS project
 * 
 * Author:		Elmar Schoch <elmar.schoch@uni-ulm.de>
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
package ducks.controller.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ducks.controller.DucksController;
import ducks.controller.simulation.SimulationInstance;
import ducks.misc.DucksException;

import jist.runtime.Main;
import jist.runtime.RemoteJist;
import jist.runtime.Node;
import jist.runtime.Main.CommandLineOptions;
import jist.swans.Constants;

/**
 * This class represents a server for job scheduling.
 * @author Elmar Schoch
 * 
 * Note: class comprises threading and accesses variables across threads
 *       => check thread safety !!! 
 */
public class Server implements Runnable {

    public static final int DISABLED = 1;
    public static final int INACTIVE = 2;
    public static final int PROCESSING = 3;

	// log4j Logger 
	private static Logger log = Logger.getLogger(Server.class.getName());
    
    /**
     * Hostname and port of remote server
     */
    private String host;
    private int port;
    
    /**
     * Literal identifier of server
     */
    private String identifier;

    private DucksController dc;

    /**
     * Flag indicating whether the server is enabled
     * to compute simulations
     */
    private boolean enabled = false;

    /**
     * Holds the Simulation object that is currently computed on
     * the JiST-server that is represented by this object in SimuGen
     */
    private SimulationInstance currentSimu = null;

    private Thread t;
    private int state = DISABLED;

    /**
     * 
     *
     */
    public Server() {

    }

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Retrieve server hostname
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets server hostname. Should be working only, when
     * server is not enabled.
     * @param host Server hostname
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Retrieve server port
     * @return Port of server
     */
    public int getPort() {
        return port;
    }

    /**
     * Set server port
     * @param port Port number 
     */
    public void setPort(int port) {
        this.port = port;
    }


    /**
     * Retrieve state of server 
     * @return Indicates whether the server is currently enabled for scheduling new simulations
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the server state (
     * @param enabled New server state
     */
    public synchronized void setEnabled(boolean enabled) {
    	
        if (this.enabled == enabled) return;

        this.enabled = enabled;

        // 1. if server is currently running a simulation, and should be disabled now
        // - remain running until simulation ends
        //   (-> set enabled flag is enough, because thread will check it before
        //       beginning the next simulation)

        // 2. if server is currently not enabled, and shall be enabled now
        // - create thread (that picks a pending simulation from the global queue)
        if (enabled) {
            createThread();
        }

    }

    public void setDucksController(DucksController ducksController) {
        this.dc = ducksController;
    }

    // ========================================================
    // Internal methods for creating and running simulations
    // ========================================================

    /**
     * Create thread that runs the jist client for the current simulations
     *
     */
    private void createThread() {

        this.t = new Thread(this);
        //t.setDaemon(true); // TODO: Discuss: does this make sense?

        // run thread now...
        synchronized(this) {
            t.start();
        }
    }


    /**
     * Implements method run() that is necessary for the Runnable interface.
     * The method contains the code for the thread running the client side
     * of the simulation.
     */
    public void run() {

    	log.info("Starting server thread for "+this.toString());

        while (true) {
        	state = INACTIVE;
        	
            // if enabled flag is not set, thread will terminate 
        	if (! isEnabled()) {
        		break;
            }
        	
        	// get the next simulation to execute (synchronized)
            currentSimu = dc.getFreeSimulation();
            
            // if no pending simulation is available, wait a little
            if (currentSimu == null) {
                try {
                	log.info("Server "+this.toString()+" is waiting for work...");
                    Thread.sleep(5000+Constants.random.nextInt(3000));
                } catch (Exception e) {
                	// do we have to catch the exception?
                }
                // next loop
                continue;
            }

            // ok, a simulation has been selected, go processing it 
            state = PROCESSING;
            
            log.info("Server "+this.toString()+" starts processing simu "+currentSimu.getIdentifier());
            
            try {
            	// run simulation on jist server
                runRemoteSimulation();
                
                // check for result section                
                try {
                	currentSimu.parseResults();
				} catch (DucksException e) {
					// Error during parsing indicates that no result section was found
					log.error("Simulation "+currentSimu.getIdentifier()+" terminated with errors: "+e.getMessage());
				}
				
				String s = "";
				if (currentSimu.getState() == SimulationInstance.DONE) s = "successfully";
				if (currentSimu.getState() == SimulationInstance.FAILED) s = "with errors";
				log.info("Simulation "+currentSimu.getIdentifier()+" has finished "+s);
                
            } catch (Exception e) {  
            	
                log.error("Running simulation failed: "+e.getMessage());
                log.error("Disabling server "+this.toString());
                //e.printStackTrace();
                
                // Cleanup: reset simulation to pending and disable server 
                currentSimu.recall();
                                	
                setEnabled(false);
                break;
            }

        }

        state = DISABLED;
        log.info(this.toString()+ " server thread terminating...");
    }

    /**
     * Helper method with the final code that runs the simulation
     * on the remote jist server
     *
     */
    private void runRemoteSimulation() throws  DucksException, MalformedURLException, NotBoundException, RemoteException {

        Properties properties = null;
        try {
            File f = new File(Main.JIST_PROPERTIES);
            FileInputStream fin = new FileInputStream(f);
            properties = new Properties();
            properties.load(fin);
            fin.close();
        } catch(IOException e) {
            properties = null;
        }

        Node server = null;
        try {
            server = new Node(this.host,this.port);
        } catch (Exception e) {
        	log.warn("Server "+this.identifier+" is not available");
            throw new RemoteException("Server "+this.identifier+" is not available");
        }

        // find remote server or queue server
        RemoteJist.JobQueueServerRemote jqs = RemoteJist.JobQueueServer.getRemote(server);
        // create jist client stub
        RemoteJist.JistClient client = new RemoteJist.JistClient(currentSimu.getOutputStream(),System.err);
        // enqueue job
        RemoteJist.Job job = new RemoteJist.Job();

        // Commandline options can be created here (since we don't use them in ducks)
        CommandLineOptions options = new CommandLineOptions();
        options.args = currentSimu.getArgs();
        options.sim = currentSimu.getSim();
        
        job.options = options;
        job.properties = properties;
        job.client = client;
        
        jqs.addJob(job, false);

        // wait for server to release client
        try {
            synchronized(client) {
                client.wait();
            }
        } catch(InterruptedException e) {
            e.printStackTrace();
            throw new DucksException("Simulation was interrupted");
        }
    }

    
    // ========================================================
    // Helper functions (e.g. getters and setters)
    // ========================================================
    
    public String toString() {
        return this.host+":"+this.port;
    }

    public int getState() {
        return state;
    }

    public Thread getThread() {
    	return t;
    }
    
    /** 
     * Retrieve currently processed simu instance
     * @return currently processed SimInstance, or null if inactive/disabled
     */
    public SimulationInstance getCurrentSimInstance() {
    	if (state == PROCESSING) {
    		return currentSimu;
    	} else {
    		return null;
    	}
    }
	
    /**
	 * @return Returns the identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @param identifier The identifier to set.
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
