/*
 * Ulm University DUCKS project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package ducks.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.log4j.Logger;

import ducks.controller.server.Server;
import ducks.controller.simulation.Simulation;
import ducks.controller.simulation.SimulationInstance;
import ducks.controller.simulation.SimulationStudy;
import ducks.misc.DucksException;

/**
 * The ControllerProxy represents a socket-based interface to the simulation
 * manager. The communication protocol is intended to work like the HTTP
 * protocol. With growing functionality, this ControllerProxy may handle most of
 * the functions like monitor ongoing simulations, monitor, enabling and
 * disabling servers, introducing new simulation jobs etc.
 * 
 * TODO: add access control mechanism
 * 
 * @author Elmar Schoch
 * 
 */
public class ControllerProxy implements Runnable
{

    public static int       GET_INVALID     = -1;
    public static int       GET_SIMUSTATE   = 1;
    public static int       GET_SERVERSTATE = 2;

    /**
     * The port the controller is listening on
     */
    public static int       PORT            = 3330;

    // log4j Logger
    private static Logger   log             = Logger.getLogger(ControllerProxy.class.getName());

    private boolean         enabled         = false;
    private ServerSocket    srvsock         = null;

    private DucksController dc;

    public ControllerProxy(DucksController dc) {
        this.dc = dc;
    }

    /**
     * Run the TCP server to accept requests
     */
    public void run() {

        try {
            srvsock = new ServerSocket(PORT);
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("Controller proxy server socket could not be opened. " + e.getMessage());
            return;
        }

        Socket s = null;

        while (enabled) {
            try {

                s = srvsock.accept();
                s.setSoTimeout(15000);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                String l;
                String reqSt = "";
                while (true) {
                    l = in.readLine();
                    if (l.equals("")) {
                        break;
                    } else {
                        reqSt += l;
                    }
                }

                Request r = parseRequest(reqSt);

                PrintStream os = new PrintStream(s.getOutputStream());
                os.print(r.getResponse());
                os.print("\n");

                s.close();

            } catch (Exception e) {
                if (s != null) {
                    if (!s.isClosed()) {
                        try {
                            s.close();
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }
    }

    /**
     * Enable or disable external controlling ability
     * 
     * @param enabled
     */
    synchronized public void setEnabled(boolean enabled) {

        if (enabled && !this.enabled) {
            Thread t = new Thread(this);
            t.start();
        }

        this.enabled = enabled;
    }

    /**
     * Main method invoked on receiving a request
     * 
     * @param req
     *            complete request as string
     * @return Parsed request object (
     * @throws DucksException
     */
    private Request parseRequest(String req) throws DucksException {

        Request r = null;

        String[] lines = req.split("\n");

        log.debug("Got request: " + req + " (" + lines.length + " lines)");

        if (lines.length >= 1) {
            String[] parts = lines[0].split(" ");
            if (parts.length >= 2) {
                if (parts[0].trim().equals("GET")) {
                    GetRequest gr = new GetRequest();
                    gr.requestedItem = GET_INVALID;

                    if (parts[1].trim().equals("simustate")) {
                        gr.requestedItem = GET_SIMUSTATE;
                    }
                    if (parts[1].trim().equals("serverstate")) {
                        gr.requestedItem = GET_SERVERSTATE;
                    }

                    r = gr;
                }
            }
        }

        return r;
    }

    public interface Request
    {
        public String getResponse();
    }

    public class GetRequest implements Request
    {

        public int requestedItem = GET_INVALID;

        public String getResponse() {

            String result = "404 Not found\n\n";

            if (requestedItem == GET_INVALID) {
                result = "400 Bad request\n\n";
            }

            if (requestedItem == GET_SIMUSTATE) {
                result = "200 OK\n\n";
                Enumeration studies = dc.getSimulationStudies().elements();
                while (studies.hasMoreElements()) {

                    SimulationStudy study = (SimulationStudy) studies.nextElement();
                    Enumeration simus = study.getSimulations().elements();
                    while (simus.hasMoreElements()) {
                        Simulation s = (Simulation) simus.nextElement();
                        // format: run-id, simu-id, simu-state, remaining inst,
                        // running inst, done inst, failed inst
                        int[] sstate = s.getStateInfo();
                        result += "0," + s.getIdentifier() + "," + s.getState() + "," + sstate[0] + ","
                                + sstate[SimulationInstance.RUNNING] + "," + sstate[SimulationInstance.DONE] + ","
                                + sstate[SimulationInstance.FAILED] + "\n";
                    }
                }
            }

            if (requestedItem == GET_SERVERSTATE) {
                result = "200 OK\n\n";

                Iterator servers = dc.getServers().getAll().iterator();
                while (servers.hasNext()) {
                    Server srv = (Server) servers.next();
                    SimulationInstance si = srv.getCurrentSimInstance();
                    long runID = -1;
                    int simuID = -1;
                    if (si != null) {
                        Simulation s = si.getSimulation();
                        simuID = s.getIdentifier();
                        runID = s.getSimStudy().getIdentifier();
                    }

                    // format: srv-identifier, host, port, state, current
                    // SimRun, current Simu
                    result += srv.getIdentifier() + "," + srv.getHost() + "," + srv.getPort() + "," + srv.getState()
                            + "," + runID + "," + simuID + "\n";
                }
            }

            log.debug("Response on request: " + result);

            return result;
        }
    }

}
