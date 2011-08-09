// ////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <AppHeartbeat.java Tue 2004/04/06 11:59:55 barr
// pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;

/**
 * Heartbeat application.
 * 
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: AppHeartbeat.java,v 1.13 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */
public class AppHeartbeat implements AppInterface, NetInterface.NetHandler
{
    // ////////////////////////////////////////////////
    // neighbour table entry
    //

    /**
     * Neighbour entry information.
     */
    private static class NeighbourEntry
    {
        /** mac address of neighbour. */
        public MacAddress mac;
        /** heartbeats until expiration. */
        public int        beats;
    }

    // ////////////////////////////////////////////////
    // constants
    //

    /** minimum heartbeat period. */
    public static final long  HEARTBEAT_MIN = 2 * Constants.SECOND;
    /** maximum heartbeat period. */
    public static final long  HEARTBEAT_MAX = 5 * Constants.SECOND;
    /** throw out information older than FRESHNESS beats. */
    public static final short FRESHNESS     = 5;

    // ////////////////////////////////////////////////
    // messages
    //

    /**
     * Heartbeat packet.
     */
    private static class MessageHeartbeat implements Message
    {
        /** {@inheritDoc} */
        public int getSize() {
            return 0;
        }

        /** {@inheritDoc} */
        public void getBytes(byte[] b, int offset) {
            throw new RuntimeException("not implemented");
        }
    } // class: MessageHeartbeat

    // ////////////////////////////////////////////////
    // locals
    //

    /** network entity. */
    private NetInterface netEntity;
    /** self-referencing proxy entity. */
    private Object       self;
    /** list of neighbours. */
    private HashMap      neighbours;
    /** node identifier. */
    private int          nodenum;
    /** whether to display application output. */
    private boolean      display;

    // ////////////////////////////////////////////////
    // initialize
    //

    /**
     * Create new heartbeat application instance.
     * 
     * @param nodenum
     *            node identifier
     * @param display
     *            whether to display application output
     */
    public AppHeartbeat(int nodenum, boolean display) {
        this.nodenum = nodenum;
        this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class, NetInterface.NetHandler.class });
        this.display = display;
        neighbours = new HashMap();
    }

    // ////////////////////////////////////////////////
    // entity
    //

    /**
     * Set network entity.
     * 
     * @param netEntity
     *            network entity
     */
    public void setNetEntity(NetInterface netEntity) {
        this.netEntity = netEntity;
    }

    /**
     * Return self-referencing NETWORK proxy entity.
     * 
     * @return self-referencing NETWORK proxy entity
     */
    public NetInterface.NetHandler getNetProxy() {
        return (NetInterface.NetHandler) self;
    }

    /**
     * Return self-referencing APPLICATION proxy entity.
     * 
     * @return self-referencing APPLICATION proxy entity
     */
    public AppInterface getAppProxy() {
        return (AppInterface) self;
    }

    // Elmar Schoch >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    // Statistics collection

    public static class HeartbeatStats
    {

        public static class NeighborEvent
        {
            public int event; // 1 = discovered, 0=lost
            long       time;
        }

        private HashMap<MacAddress, List<NeighborEvent>> nbEvents = new HashMap<MacAddress, List<NeighborEvent>>();

        public void neighborDiscovered(MacAddress mac) {
            List<NeighborEvent> evList = nbEvents.get(mac);
            if (evList == null) {
                evList = new ArrayList<NeighborEvent>();
                nbEvents.put(mac, evList);
            }
            NeighborEvent ne = new NeighborEvent();
            ne.event = 1;
            ne.time = JistAPI.getTime();
            evList.add(ne);
        }

        public void neighborLost(MacAddress mac) {
            List<NeighborEvent> evList = nbEvents.get(mac);
            if (evList == null) {
                System.out.println("Event List started with Neighbor loss. Should not happen like this.");
                evList = new ArrayList<NeighborEvent>();
                nbEvents.put(mac, evList);
            }
            NeighborEvent ne = new NeighborEvent();
            ne.event = 0;
            ne.time = JistAPI.getTime();
            evList.add(ne);
        }

        private boolean isNeighbor(MacAddress mac, long time) {
            List<NeighborEvent> evList = nbEvents.get(mac);
            if (evList.get(0).time < time) {
                for (NeighborEvent ne : evList) {
                    if (ne.time <= time) {
                        if (ne.event == 1) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
            // if no time entry smaller than the current time is found
            return false;
        }

        /**
         * Return a list of statistical values on number of neighbors, counted
         * between 'start' and 'end', with a granularity of 'step'
         * 
         * @param start
         * @param end
         * @param step
         * @return array of floats. Currently [0] = min, [1] = max, [2] =
         *         average (more imaginable)
         */
        public float[] getNeighborCountStats(long start, long end, long step) {

            int minNeighbors = Integer.MAX_VALUE;
            int maxNeighbors = Integer.MIN_VALUE;
            ArrayList<Integer> counts = new ArrayList<Integer>();
            long time = start;
            while (time < end) {
                int cnt = 0;
                for (MacAddress mac : nbEvents.keySet()) {
                    if (isNeighbor(mac, time))
                        cnt++;
                }
                if (cnt > maxNeighbors)
                    maxNeighbors = cnt;
                if (cnt < minNeighbors)
                    minNeighbors = cnt;
                counts.add(new Integer(cnt));

                time += step;
            }

            float[] result = new float[3];
            result[0] = minNeighbors;
            result[1] = maxNeighbors;
            int sum = 0;
            for (Integer cnt : counts) {
                sum += cnt;
            }
            if (counts.size() > 0) {
                result[2] = (float) sum / counts.size();
            } else {
                result[2] = 0;
            }

            return result;
        }

        public void displayNeighborTrace() {
            for (MacAddress mac : nbEvents.keySet()) {
                System.out.print("Node " + mac + " ");
                for (NeighborEvent ne : nbEvents.get(mac)) {
                    if (ne.event == 1) {
                        System.out.print("discovered at " + ne.time + ", ");
                    } else {
                        System.out.print("lost at " + ne.time + ", ");
                    }
                }
                System.out.println("");
            }
        }

        /**
         * Return statistics about the time that nodes stay in mutual
         * visibility.
         * 
         * @param end
         *            [0] min visibility of a neighbor (seconds) [1] max. [2]
         *            avg. [3] total number of reincounters
         * @return
         */
        public float[] getNeighborTimeStats(long end) {
            int reincounters = 0;
            long start;
            boolean reincounterFlag;

            ArrayList<Long> periods = new ArrayList<Long>();

            for (MacAddress mac : nbEvents.keySet()) {
                reincounterFlag = false;
                start = -1;
                for (NeighborEvent ne : nbEvents.get(mac)) {
                    if (ne.event == 1) {
                        start = ne.time;
                        if (reincounterFlag)
                            reincounters++;
                    } else {
                        if (start == -1) {
                            // a neighbor lost event following no neighbor
                            // discovered event
                            // --> Something is wrong. Don't count.
                            System.out.println("Something wrong.");
                        } else {
                            periods.add(new Long(ne.time - start));
                            start = -1;
                            reincounterFlag = true;
                        }
                    }
                }
                if (start != -1) {
                    // Neighbor was still there at the end
                    periods.add(new Long(end - start));
                }
            }

            long minPeriod = Long.MAX_VALUE;
            long maxPeriod = Long.MIN_VALUE;
            long avgPeriodSum = 0;
            for (Long period : periods) {
                if (period.longValue() < minPeriod)
                    minPeriod = period.longValue();
                if (period.longValue() > maxPeriod)
                    maxPeriod = period.longValue();
                avgPeriodSum += period.longValue();
            }

            float[] result = new float[4];
            result[0] = minPeriod / (float) Constants.SECOND;
            result[1] = maxPeriod / (float) Constants.SECOND;
            if (periods.size() > 0) {
                result[2] = (avgPeriodSum / (float) Constants.SECOND) / (float) periods.size();
            } else {
                result[2] = 0;
            }
            result[3] = reincounters;

            return result;
        }
    }

    public HeartbeatStats hbs = new HeartbeatStats();

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // ////////////////////////////////////////////////
    // neighbour events
    //

    /**
     * Neighbour lost.
     * 
     * @param mac
     *            mac adddress of neighbour lost
     */
    private void neighbourLost(MacAddress mac) {
        // Elmar Schoch added >>>>>>>>>>>>>>>>>>>>>
        hbs.neighborLost(mac);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
        if (display) {
            System.out.println("(" + nodenum + ") lost neighbour:  " + mac + ", t=" + Util.timeSeconds());
        }
    }

    /**
     * Neighbour discovered.
     * 
     * @param mac
     *            mac address of neighbour discovered
     */
    private void neighbourDiscovered(MacAddress mac) {
        // Elmar Schoch added >>>>>>>>>>>>>>>>>>>>>
        hbs.neighborDiscovered(mac);
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
        if (display) {
            System.out.println("(" + nodenum + ") found neighbour: " + mac + ", t=" + Util.timeSeconds());
        }
    }

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    // ////////////////////////////////////////////////
    // NetHandler methods
    //

    /** {@inheritDoc} */
    public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority,
            byte ttl) {
        // System.out.println("("+nodenum+") received packet from ip="+src+" mac="+lastHop+" at t="+Util.timeSeconds());
        NeighbourEntry n = (NeighbourEntry) neighbours.get(src);
        if (n == null) {
            neighbourDiscovered(lastHop);
            n = new NeighbourEntry();
            neighbours.put(src, n);
        }
        n.mac = lastHop;
        n.beats = FRESHNESS;
    }

    // ////////////////////////////////////////////////
    // AppInterface methods
    //

    /**
     * Compute random heartbeat delay.
     * 
     * @return delay to next heartbeat
     */
    private long calcDelay() {
        return HEARTBEAT_MIN + (long) ((HEARTBEAT_MAX - HEARTBEAT_MIN) * Constants.random.nextFloat());
    }

    /** {@inheritDoc} */
    public void run(String[] args) {
        // staggered beginning
        if (JistAPI.getTime() == 0) {
            JistAPI.sleep(calcDelay());
        }
        // send heartbeat
        Message msg = new MessageHeartbeat();
        netEntity.send(msg, NetAddress.ANY, Constants.NET_PROTOCOL_HEARTBEAT, Constants.NET_PRIORITY_NORMAL, (byte) 1);
        // process neighbour set
        Iterator it = neighbours.values().iterator();
        while (it.hasNext()) {
            NeighbourEntry n = (NeighbourEntry) it.next();
            n.beats--;
            if (n.beats == 0) {
                neighbourLost(n.mac);
                it.remove();
            }
        }
        // schedule next
        JistAPI.sleep(calcDelay());
        ((AppInterface) self).run();
    }

    /** {@inheritDoc} */
    public void run() {
        run(null);
    }

}
