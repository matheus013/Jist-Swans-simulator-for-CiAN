/*
 * Ulm University JiST/SWANS project
 * 
 * Author:		Michael Feiri <michael.feiri@uni-ulm.de>
 * 
 * (C) Copyright 2006, Ulm University, all rights reserved.
 * 
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

package vans.net;


import org.apache.log4j.Logger;

import ext.util.CacheMap;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Field.RadioData;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetIpBase;
import jist.swans.net.NetMessage;
import jist.swans.net.PacketLoss;
import jist.swans.route.RouteInterface;

/**
 * Extension of NetIP that considers geographic addressing modes in NetAddress
 * 
 * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
 */

// implement NetInterface here because JiST can rewrite classes only once
public class NetIpGeo extends NetIpBase implements NetInterface {
	
	private static Logger log = Logger.getLogger(NetIpGeo.class.getName());

	private static final int DUP_DATABASE_SIZE = 20;
	private static final long DUP_DATABASE_TIME = 10 * Constants.MINUTE;
	public static final long FORWARDING_JITTER = 200 * Constants.MICRO_SECOND;
	
	private final boolean geoOption;
	private RadioData localRadio;
	private CacheMap database;
	private RouteInterface routeNonEntity;
	
	public NetIpGeo(NetAddress addr, Mapper protocolMap, PacketLoss in,
			PacketLoss out, RadioData rd, boolean piggy) {
		super(addr, protocolMap, in, out);
		localRadio = rd;
		geoOption = piggy;
		database = new CacheMap(DUP_DATABASE_SIZE);
	}

	/**
	 * Return true if the message is *exlcusively* for this node. Every thing else
	 * will be handled by SendIp. Since we reuse regular Ip adresses we can simply
	 * call super.isForMe() in area mode.
	 * 
	 * @param msg
	 * @return true if this packet should reach upper layers in this node
	 */
	protected boolean isForMe(NetMessage.Ip msg) {
		NetAddress addr = msg.getDst();
		if (addr instanceof NetAddressGeo)
			if (!((NetAddressGeo) addr).getRegion().contains(localRadio.getLoc()))
				return false;
		return super.isForMe(msg);
	}
	

	protected boolean isDup(NetMessage.Ip msg) {
		
		// Beacons should not fill up the dup database
		//if (msg.getProtocol()==Constants.NET_PROTOCOL_CGGC) return false;
		// Loopback delivery is not suspect to dup production
		if (NetAddress.LOCAL.equals(msg.getDst())) return false;
		
		Long key = new Long(((long)msg.getId() << 32) + msg.getSrc().getIP().hashCode());
	    Long dupTimestamp = (Long)database.put(key, new Long(JistAPI.getTime()));
	    
	    log.debug("DUP check: here="+localAddr+" msg="+msg.getSrc()+"/"+msg.getId()+" key="+key+" dupTime="+dupTimestamp);
	    
	    if (dupTimestamp==null) {
	    	return false;  // fist-time delivery
	    } else if (dupTimestamp.longValue()+DUP_DATABASE_TIME < (JistAPI.getTime())) {
	    	return false;  // dup was stale
	    } else {
	    	log.debug("DUP found, here="+localAddr+" msg="+msg.getSrc()+"/"+msg.getId());
	    	return true; // dup detected
	    }
	    
	}
	
	/** {@inheritDoc} */
	public void send(NetMessage.Ip msg, int interfaceId, MacAddress nextHop) {
		
		log.debug("send here="+localAddr+" to="+nextHop+" msg="+msg.getSrc()+"/"+msg.getId()+" iid="+interfaceId);
		
		if (geoOption) {
			try {
				// TODO This throws UnsupportedOperationExceptions!!!
				// because getOptions() returns an unmodifiable map, if the packet is frozen.
				// Then, the operation "put" is of course not allowed in such a map.
				// -> WORKAROUND: check before, if packet is frozen
				if (! msg.isFrozen()) {
					msg.getOptions().put(Constants.IP_OPTION_HOPLOC,new NetMessage.IpOptionHopLoc(localRadio.getLoc()));
				}
			} catch (UnsupportedOperationException e) {
				System.out.println(e);
				// do nothing if options were frozen
			}
		}
		
		super.send(msg, interfaceId, nextHop);
	}

	
	/**
	 * Send an IP packet. Knows how to broadcast, to deal with loopback. Will
	 * call routing for all other destinations.
	 * 
	 * @param msg
	 *            ip packet
	 */
	protected void sendIp(NetMessage.Ip msg, boolean bcast) {

		if (msg.getDst() instanceof NetAddressGeo) {
			NetAddressGeo addr = (NetAddressGeo) msg.getDst();
			
			if (addr.getRegion().contains(localRadio.getLoc())) {
				// AREA FORWARDING MODE within destination zone: Flooding
				
				log.debug("sendIp "+" here="+localAddr+" msg="+msg.getSrc()+"/"+msg.getId());
				
				// First add packet to the local dup database
				Long key = new Long(((long)msg.getId() << 32) + msg.getSrc().getIP().hashCode());
			    database.put(key, new Long(JistAPI.getTime()));
				
			    // GEO[BROAD|MULTI]CAST can additionally send packets to upper layers via loop
			    if ((NetAddress.NULL.equals(addr)) || (addr.getIP().isMulticastAddress()))
					send(msg, Constants.NET_INTERFACE_LOOPBACK, MacAddress.LOOP);
			    
			    // Flood
			    // Before forwarding, sleep for a jitter time, otherwise packets may collide
			    long jitter = Util.randomTime(FORWARDING_JITTER);
			    JistAPI.sleep(jitter);
			    log.debug("   sleeping "+jitter+" ns before forwarding");
			    
			    send(msg.copy() , Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
			    
			} else {
				// dont return to line forwarding mode if area mode was already entered
				// if I could rely on geoOption always being active I could use that information
				// to tell if the last hop was in line or area mode. now I just use info from MAC
			    if (!bcast) routing.send(msg);
			}
			
		} else {
			super.sendIp(msg, bcast);
		}
	}
	
	public void setRoutingNonEntity(RouteInterface r) {
		routeNonEntity = r;
	}
	
	public String toString() {
		return "NetIpGeo " + super.toString() + "#" + localRadio.getLoc().toString();
	}
}
