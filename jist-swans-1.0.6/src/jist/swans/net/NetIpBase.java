// ////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <NetIpBase.java Tue 2004/04/20 10:12:52 barr
// pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

// Includes extensions by Ulm University (Michael Feiri, Elmar Schoch)
// - Notification of packets dropped on MAC layer
// - More flexible protocol handler
// - Broadcast reception flag

package jist.swans.net;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacInterface;
import jist.swans.mac.MacLoop;
import jist.swans.misc.Mapper;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetInterface.NetHandler;
import jist.swans.route.RouteInterface;

import org.apache.log4j.Logger;

/**
 * IPv4 implementation based on RFC 791. Performs protocol multiplexing, and
 * prioritized packet queuing, but no RED or packet fragmentation.
 * 
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: NetIpBase.java,v 1.45 2004/11/03 23:53:22 barr Exp $
 * @since SWANS1.0
 */

// public class NetIpBase implements NetInterface
public abstract class NetIpBase
{
    /**
     * IP logger.
     */
    public static final Logger log = Logger.getLogger(NetIpBase.class.getName());

    /**
     * Information about each network interface.
     */
    public static class NicInfo
    {
        /** mac entity. */
        public MacInterface mac;
        /** outgoing packet queue. */
        public MessageQueue q;
        /** whether link layer is busy. */
        public boolean      busy;
    }

    // ////////////////////////////////////////////////
    // constants
    //

    /**
     * Packet fragmentation threshold.
     */
    public static final int  THRESHOLD_FRAGMENT = 2048;

    /**
     * Maximum packet queue length.
     */
    public static final byte MAX_QUEUE_LENGTH   = 50;

    // ////////////////////////////////////////////////
    // locals
    //

    // entity hookup
    /** self-referencing proxy entity. */
    protected NetInterface   self;

    /** local network address. */
    protected NetAddress     localAddr;

    /** routing protocol. */
    protected RouteInterface routing;

    /** protocol number mapping. */
    protected Mapper         protocolMap;

    /** protocol handlers. */
    protected NetHandler[]   protocolHandlers;

    /** network interfaces. */
    protected NicInfo[]      nics;

    /** packet loss models. */
    protected PacketLoss     incomingLoss, outgoingLoss;

    // ////////////////////////////////////////////////
    // initialization
    //

    /**
     * Initialize IP implementation with given address and protocol mapping.
     * 
     * @param addr
     *            local network address
     * @param protocolMap
     *            protocol number mapping
     * @param in
     *            incoming packet loss model
     * @param out
     *            outgoing packet loss model
     */
    public NetIpBase(NetAddress addr, Mapper protocolMap, PacketLoss in, PacketLoss out) {
        // proxy entity
        this.self = (NetInterface) JistAPI.proxy(this, NetInterface.class);
        // local address
        setAddress(addr);
        // protocol number mapping
        this.protocolMap = protocolMap;
        // protocol handlers
        // @author Elmar Schoch >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        // Remark: since protocolMap.getLimit() always returns the CURRENT
        // maximum index in the map, later assignment of net handlers
        // than the NetIP constructor is not possible. This side effect
        // makes development rather tricky and thus should be avoided
        // I left this in here and decided to implement a sustainable
        // solution - now, the array grows dynamically when new handlers
        // are set (see setNetHandler method)
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
        this.protocolHandlers = new NetHandler[protocolMap.getLimit()];
        // network interfaces
        this.nics = new NicInfo[0];
        // packet loss
        this.incomingLoss = in;
        this.outgoingLoss = out;
        // add loopback mac:
        // therefore, loopback = 0, Constants.NET_INTERFACE_LOOPBACK
        // next = 1, Constants.NET_INTERFACE_DEFAULT
        MacLoop loopback = new MacLoop();
        byte netid = addInterface(loopback.getProxy());
        if (Main.ASSERT)
            Util.assertion(netid == Constants.NET_INTERFACE_LOOPBACK);
        loopback.setNetEntity(getProxy(), netid);
    }

    // ////////////////////////////////////////////////
    // entity hookup
    //

    /**
     * Return self-referencing proxy entity.
     * 
     * @return self-referencing proxy entity
     */
    public NetInterface getProxy() {
        return this.self;
    }

    // ////////////////////////////////////////////////
    // address
    //

    /**
     * Set local network address.
     * 
     * @param addr
     *            local network address
     */
    public void setAddress(NetAddress addr) {
        if (Main.ASSERT)
            Util.assertion(addr != null);
        this.localAddr = addr;
    }

    /**
     * Whether packet is for local consumption.
     * 
     * @param msg
     *            packet to inspect
     * @return whether packet is for local consumption
     */
    protected boolean isForMe(NetMessage.Ip msg) {
        NetAddress addr = msg.getDst();
        return NetAddress.ANY.equals(addr) || NetAddress.LOCAL.equals(addr) || localAddr.equals(addr);
    }

    protected boolean isDup(NetMessage.Ip msg) {
        return false;
    }

    // ////////////////////////////////////////////////
    // routing, protocols, interfaces
    //

    /**
     * Set routing implementation.
     * 
     * @param routingEntity
     *            routing entity
     */
    public void setRouting(RouteInterface routingEntity) {
        if (!JistAPI.isEntity(routingEntity))
            throw new IllegalArgumentException("expected entity");
        this.routing = routingEntity;
    }

    /**
     * Add network interface with default queue.
     * 
     * @param macEntity
     *            link layer entity
     * @return network interface identifier
     */
    public byte addInterface(MacInterface macEntity) {
        return addInterface(macEntity,
                new MessageQueue.NoDropMessageQueue(Constants.NET_PRIORITY_NUM, MAX_QUEUE_LENGTH));
    }

    /**
     * Add network interface.
     * 
     * @param macEntity
     *            link layer entity
     * @return network interface identifier
     */
    public byte addInterface(MacInterface macEntity, MessageQueue q) {
        if (!JistAPI.isEntity(macEntity))
            throw new IllegalArgumentException("expected entity");
        // create new nicinfo
        NicInfo ni = new NicInfo();
        ni.mac = macEntity;
        ni.q = q;
        ni.busy = false;
        // store
        NicInfo[] nics2 = new NicInfo[nics.length + 1];
        System.arraycopy(nics, 0, nics2, 0, nics.length);
        nics2[nics.length] = ni;
        nics = nics2;
        // return interface id
        return (byte) (nics.length - 1);
    }

    /**
     * Set network protocol handler.
     * 
     * @param protocolId
     *            protocol identifier
     * @param handler
     *            protocol handler
     */
    public void setProtocolHandler(int protocolId, NetHandler handler) {

        // @author Elmar Schoch >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        // grow protocol handler array, if necessary
        if (protocolMap.getLimit() > protocolHandlers.length) {
            log.info("Growing protocol handler array at " + this.localAddr + " from " + protocolHandlers.length
                    + " to " + protocolMap.getLimit());
            NetHandler[] protocolHandlersNew = new NetHandler[protocolMap.getLimit()];
            for (int i = 0; i < protocolHandlers.length; i++) {
                protocolHandlersNew[i] = protocolHandlers[i];
            }
            protocolHandlers = protocolHandlersNew;
        }
        // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        protocolHandlers[protocolMap.getMap(protocolId)] = handler;
    }

    /**
     * Return network protocol handler.
     * 
     * @param protocolId
     *            protocol identifier
     * @return procotol handler
     */
    private NetHandler getProtocolHandler(int protocolId) {
        return protocolHandlers[protocolMap.getMap(protocolId)];
    }

    // ////////////////////////////////////////////////
    // NetInterface implementation
    //

    /** {@inheritDoc} */
    public NetAddress getAddress() throws JistAPI.Continuation {
        return localAddr;
    }

    /** {@inheritDoc} */
    public void receive(Message msg, MacAddress lastHop, byte macId, boolean promisc, boolean bcast) {
        if (msg == null)
            throw new NullPointerException();
        NetMessage.Ip ipmsg = (NetMessage.Ip) msg;
        if (incomingLoss.shouldDrop(ipmsg))
            return;
        if (log.isInfoEnabled()) {
            log.info("receive t=" + JistAPI.getTime() + " from=" + lastHop + " on=" + macId + " data=" + msg);
        }
        if (routing != null)
            routing.peek(ipmsg, macId, lastHop);
        if (promisc)
            return;
        if (bcast)
            if (isDup(ipmsg))
                return;

        if (isForMe(ipmsg) || (macId == Constants.NET_INTERFACE_LOOPBACK)) {
            JistAPI.sleep(Constants.NET_DELAY);
            getProtocolHandler(ipmsg.getProtocol()).receive(ipmsg.getPayload(), ipmsg.getSrc(), lastHop, macId,
                    ipmsg.getDst(), ipmsg.getPriority(), ipmsg.getTTL());
        } else {
            if (ipmsg.getTTL() > 0) {
                if (ipmsg.isFrozen())
                    ipmsg = ipmsg.copy();
                else
                    log.info("NOT FRIZZLED!");
                ipmsg.decTTL();
                sendIp(ipmsg, bcast);
            } else {

                log.info("in node " + localAddr + " a packets ttl expired " + ipmsg);
                return;
            }
        }
    }

    public void dropNotify(Message packet, MacAddress packetNextHop) {
        routing.dropNotify(packet, packetNextHop);
    }

    /** {@inheritDoc} */
    public void send(Message msg, NetAddress dst, short protocol, byte priority, byte ttl) {
        if (msg == null)
            throw new NullPointerException();
        sendIp(new NetMessage.Ip(msg, localAddr, dst, protocol, priority, ttl), false);
    }

    /** {@inheritDoc} */
    public void send(NetMessage.Ip msg, int interfaceId, MacAddress nextHop) {
        if (msg == null)
            throw new NullPointerException();
        if (outgoingLoss.shouldDrop(msg))
            return;
        /*
         * if(msg.getSize()>THRESHOLD_FRAGMENT) { throw new
         * RuntimeException("ip fragmentation not implemented"); }
         */
        if (log.isDebugEnabled()) {
            log.debug("queue t=" + JistAPI.getTime() + " to=" + nextHop + " on=" + interfaceId + " data=" + msg);
        }
        NicInfo ni = nics[interfaceId];
        ni.q.insert(new QueuedMessage(msg, nextHop), msg.getPriority());
        if (!ni.busy)
            pump(interfaceId);
    }

    // ////////////////////////////////////////////////
    // send/receive
    //

    /**
     * Send an IP packet. Knows how to broadcast, to deal with loopback. Will
     * call routing for all other destinations.
     * 
     * @param msg
     *            ip packet
     * @param bast
     *            whether the packet was received as a broadcast
     */
    protected void sendIp(NetMessage.Ip msg, boolean bcast) {
        if (NetAddress.ANY.equals(msg.getDst())) {
            // broadcast
            send(msg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
        } else if (NetAddress.LOCAL.equals(msg.getDst()) || localAddr.equals(msg.getDst())) {
            // loopback
            send(msg, Constants.NET_INTERFACE_LOOPBACK, MacAddress.LOOP);
        } else {
            // route and send
            routing.send(msg);
        }
    }

    // ////////////////////////////////////////////////
    // send pump
    //

    /** {@inheritDoc} */
    public void pump(int interfaceId) {
        NicInfo ni = nics[interfaceId];
        if (ni.q.isEmpty()) {
            ni.busy = false;
        } else {
            ni.busy = true;
            QueuedMessage qmsg = ni.q.remove();
            NetMessage.Ip ip = (NetMessage.Ip) qmsg.getPayload();
            ip = ip.freeze(); // immutable once packet leaves node
            if (log.isInfoEnabled()) {
                log.info("send t=" + JistAPI.getTime() + " to=" + qmsg.getNextHop() + " data=" + ip);
            }
            JistAPI.sleep(Constants.NET_DELAY);
            ni.mac.send(ip, qmsg.getNextHop());
        }
    }

    // ////////////////////////////////////////////////
    // display
    //

    /** {@inheritDoc} */
    public String toString() {
        return "ip:" + localAddr;
    }

}
