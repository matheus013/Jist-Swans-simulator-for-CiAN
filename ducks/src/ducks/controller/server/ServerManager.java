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

import java.rmi.AlreadyBoundException;
import java.util.HashMap;
import java.util.Vector;

import jist.runtime.Main;

import org.apache.log4j.Logger;

import ducks.controller.DucksController;
import ducks.controller.DucksControllerModule;
import ext.util.ExtendedProperties;

/**
 * ServerManager contains the list of jist compute servers and allows to load,
 * add and remove them.
 * 
 * @author Elmar Schoch, Stefan Schlott
 * 
 */
public class ServerManager implements DucksControllerModule {

	// log4j Logger
	private static Logger log = Logger.getLogger(ServerManager.class.getName());

	// this module processes options in the config file with this prefix
	public static final String CFG_PREFIX = "ducks.servers";

	// list of Server objects
	private Vector<Server> servers = new Vector<Server>();

	// reference to the DUCKS controller
	private DucksController ducksController;

	// local server (may be used if no remote server is available)
	private Thread localserver = null;

	/**
	 * Server thread for local server
	 */
	private class ServerThread extends Thread {
		String servername;
		Main.CommandLineOptions options;

		public ServerThread(String servername, Main.CommandLineOptions options) {
			this.servername = servername;
			this.options = options;
		}

		public void run() {
			Main.showVersion();
			try {
				Main.runServer(servername, options);
			} catch (InterruptedException e) {
				// Do nothing
			} catch (AlreadyBoundException abe) {
				System.out
						.println("ERR: Could not create local server, port is already in use");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ServerManager constructor
	 * 
	 * @param dc
	 */
	public ServerManager(DucksController dc) {
		ducksController = dc;
	}

	/**
	 * Load the list of servers from a properties object (like the DUCKS
	 * controller config). Format of server parameters is:
	 * ducks.servers.{server-identifier-string}.hostname={server hostname or IP}
	 * ducks.servers.{server-identifier-string}.port={server port}
	 * ducks.servers.{server-identifier-string}.enabled={0|1}
	 */
	public void configure(ExtendedProperties config) {

		// load servers
		HashMap<String, ExtendedProperties> srvlist = config.getSubNamespaces(
				CFG_PREFIX, false);

		for (String srvname : srvlist.keySet()) {
			ExtendedProperties srvprops = srvlist.get(srvname);
			boolean enabled = srvprops.getBooleanProperty("enabled", true);
			// only currently enabled servers are added to the list
			// (if the property is missing, default is to enable)
			if (enabled) {
				String hostname = srvprops.getProperty("hostname");
				int port = Integer.parseInt(srvprops.getProperty("port"));
				String identifier = srvname;
				this.add(hostname, port, identifier);
			}
		}

		log.debug("Loaded " + servers.size() + " JiST compute servers");

	}

	public void enable() {

		// Enable all servers
		for (Server srv : servers) {
			srv.setEnabled(true);
		}
	}

	public void disable() {

		// Disable all servers
		for (Server srv : servers) {
			srv.setEnabled(false);
			try {
				srv.getThread().join();
			} catch (InterruptedException e) {
				// TODO: handle exception
			}
		}
	}

	// Local server functionality
	// ...................................................

	/**
	 * Create a local server<br>
	 * If no remote server is configured, ServerManager offers the capability to
	 * create a local server. This needs to be done externally; ServerManager
	 * will not create a local server automatically.
	 * 
	 * @param port
	 *            port to listen on
	 * @return true if everything worked fine
	 */
	public boolean createLocalServer(int port) {

		if (servers.size() < 1) {
			log.debug("No servers found, creating local server");

			// Create local JiST server in a thread
			String servername = "localhost";
			Main.CommandLineOptions options = new Main.CommandLineOptions();
			options.port = port;
			options.server = true;
			options.nocache = true;
			System.setProperty("java.rmi.server.hostname", servername);
			try {
				localserver = new ServerThread(servername, options);
				localserver.start();
			} catch (Exception e) {
				log.fatal("Creating local server failed " + e.getMessage());
				return false;
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// Do nothing
			}

			// add the local server to the DUCKS server list
			this.add(servername, port, "local_temp");

		}
		return true;
	}

	/**
	 * Stop a locally created JiST server (if running)
	 * 
	 */
	public void stopLocalServer() {
		if (localserver != null) {
			log.debug("Interrupting local server");
			localserver.interrupt();
		}
	}

	// Administrative functions .......................................

	/**
	 * Add a new server to the list
	 * 
	 * @param hostname
	 *            name of the host
	 * @param port
	 *            number of the port where JiST listens
	 * @param identifiere
	 *            literal identifier of the server (no further meaning)
	 */
	public void add(String hostname, int port, String identifier) {
		Server server = new Server(hostname, port);
		server.setIdentifier(identifier);
		server.setDucksController(ducksController);
		servers.add(server);

		log.info("Added jist server [" + server.getIdentifier() + "] at "
				+ server.getHost() + ":" + server.getPort());
	}

	public void remove(Server server) {

		if (server.isEnabled()) {
			server.setEnabled(false);
		}

		servers.remove(server);
	}

	/**
	 * Get number of servers in the list, regardless whether currently enabled
	 * or not. Note that servers set to enabled=false in the config file are not
	 * added to the server list at all.
	 * 
	 * @return number of initially configured servers
	 */
	public int count() {
		return servers.size();
	}

	public Vector getAll() {
		return servers;
	}

	/**
	 * Retrieve the number of CURRENTLY enabled servers. Enabled means that the
	 * server is active and trying to get work
	 * 
	 * @return Number of enabled servers
	 */
	public int numberOfEnabledServers() {
		int cnt = 0;
		for (Server srv : servers) {
			if (srv.isEnabled())
				cnt++;
		}
		return cnt;
	}

}
