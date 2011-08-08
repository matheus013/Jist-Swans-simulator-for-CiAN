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
package ducks.driver;

import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.Placement;
import jist.swans.mac.Mac802_11;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacDumb;
import jist.swans.mac.MacInterface;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location.Location2D;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetIp;
import jist.swans.net.NetIpBase;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioInterface;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseAdditive;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo.RadioInfoShared;
import jist.swans.route.RouteAodv;
import jist.swans.route.RouteDsr;
import jist.swans.route.RouteInterface;
import jist.swans.route.RouteZrp;

import org.apache.log4j.Logger;

import vans.apps.AppCbrGeo;
import vans.apps.AppGeocast;
import vans.net.NetIpGeo;
import vans.route.DucksCggcStats;
import vans.route.RouteCGGC;
import vans.straw.StreetMobility;
import vans.straw.VisualizerInterface;

import ext.jist.swans.app.AppCbrBase;
import ext.jist.swans.app.AppCbrIp;
import ext.jist.swans.mobility.MobilityReplay;
import ext.jist.swans.net.DropTailMessageQueue;
import ext.util.ExtendedProperties;
import ext.util.stats.DucksAodvStats;
import ext.util.stats.DucksDsrStats;
import ext.util.stats.DucksMac80211Stats;
import ext.util.stats.DucksRadioNoiseAdditiveStats;
import ext.util.stats.MultipleStatsCollector;

/**
 * GenericNode represents a node in the network, whose components can be configured
 * in the config file. The GenericNode implementation will then initialize all
 * required components according to this config (see SimParams.java for details)
 * 
 * @author Elmar Schoch
 *
 */
public class GenericNode implements Node {
	
	// log4j Logger 
	private static Logger log = Logger.getLogger(GenericNode.class.getName());
	
	protected Scene scene;

	protected Placement place; 
	protected RadioInfoShared radioInfo;
	protected Mapper protMap;
	protected PacketLoss inLoss; 
	protected PacketLoss outLoss;
	protected VisualizerInterface visualizer;
	protected ExtendedProperties globalConfig;
	
	public ExtendedProperties options;
	
	protected RadioNoise radio;
	protected RadioInterface radioEntity;
	protected MacInterface mac, macEntity;
	protected NetIpBase net;
	protected NetInterface netEntity;
	protected RouteInterface route, routeEntity;	
	protected AppInterface app, appEntity;
	
	protected Location location;
	
	protected MultipleStatsCollector stats = new MultipleStatsCollector();
	
	protected int id = -1;
	protected int nodeCount = 0;
	
	/**
	 * Set node id 
	 */
	public void setIdentifier(int id) {
	    this.id = id;
    }
	
	/**
	 * Set scene this node is part of
	 */
	public void setScene(Scene scene) {
		this.scene = scene;
    }
	
	public void setGlobalConfig(ExtendedProperties config) {
	    this.globalConfig = config;
    }

	/**
	 * SWANS uses several common objects for all nodes, which can be assigned using
	 * this method
	 */
	public void setCommonObjects(Placement place, RadioInfoShared radioInfo, Mapper protMap, 
			PacketLoss inLoss, PacketLoss outLoss, VisualizerInterface v) {
	    this.place = place;
	    this.radioInfo = radioInfo;
	    this.protMap = protMap;
	    this.inLoss = inLoss;
	    this.outLoss = outLoss;
	    this.visualizer = v;
    }


	public void configure(ExtendedProperties config) throws Exception {
		this.options = config;
		
		// assemble node (i.e. instantiate and tie together all parts
		// given in the config
		if (id == -1) {
			log.error("Node ID is not set properly upon node assembly!");
		}
		assembleNode();
	}

	public ExtendedProperties getConfig() {
		return options;
	}
	
	public String[] getStatParams() {
		return stats.getStatParams();
	}

	public ExtendedProperties getStats() {
		return stats.getStats();
	}

	
    // node assembly methods ..........................................
	
	protected void assembleNode() throws Exception {
		
		log.debug("Assembling node "+id);
		
		location = getLocation();
		
		// create layer modules
		addRadio(id, radioInfo);	
		addMac(id);
		
		// link field, radio and mac together (required by addNetwork)
		linkFieldAndRadio();
		linkRadioAndMac();

		addNetwork(id, protMap, inLoss, outLoss, scene.getField());
		addRouting(net.getAddress(), protMap, scene.getField());
		addApplication(protMap, scene.getField(), place);

		// link layers, where still necessary
		linkMacAndNet();
		linkNetAndRouting();
		
	    addNodeToVisualizer();
	}
	
	protected Location getLocation() {
		
		Location location = null;
		Mobility mob = scene.getField().getMobility();
		if (mob instanceof MobilityReplay) {
			location = ((MobilityReplay) mob).getInitialPosition(id);
		}
		if (location == null) {
			location = place.getNextLocation();
		}
		
		return location;
	}
	
	/**
	 * Link Field with Radio layer and vice versa.
	 * This involves invoking the start of the mobility model
	 */
	protected void linkFieldAndRadio() {
		radio.setFieldEntity(scene.getField().getProxy());
		
		scene.getField().addRadio(radio.getRadioInfo(), radioEntity, location);
	    scene.getField().startMobility(radio.getRadioInfo().getUnique().getID());
	}
	
	/**
	 * Link Radio layer with MAC layer and vice versa.
	 * @throws Exception
	 */
	protected void linkRadioAndMac() throws Exception {
		radio.setMacEntity(macEntity);
		
		// TODO improper solution in swans: Mac802_11 and MacDumb only have MacInterface in common,
		// whereas several procedures are literally identical => using an additional (perhaps derived) 
		// interface would have been nice!!!
		if (mac instanceof Mac802_11) {
			((Mac802_11) mac).setRadioEntity(radioEntity);
		} else 
		if (mac instanceof MacDumb) {
			((MacDumb) mac).setRadioEntity(radioEntity);
		} else {
			throw new Exception("MAC could not be linked with RADIO, because MAC type is not recognized");
		}
	}
	
	/**
	 * Link MAC and Network layer and vice versa.
	 * This involves creating an interface and a queue to the MAC layer
	 * @throws Exception
	 */
	protected void linkMacAndNet() throws Exception {
		// build net interface 
		DropTailMessageQueue msgQ = new DropTailMessageQueue(Constants.NET_PRIORITY_NUM, NetIp.MAX_QUEUE_LENGTH);
		stats.registerStaticCollector(msgQ);
		
		byte intId = net.addInterface(macEntity, msgQ);
		
		// TODO improper solution in swans: Mac802_11 and MacDumb only have MacInterface in common,
		// whereas several procedures are literally identical => using an additional (perhaps derived) 
		// interface would have been nice!!!
		if (mac instanceof Mac802_11) {
			((Mac802_11) mac).setNetEntity(netEntity, intId);
		} else
		if (mac instanceof MacDumb) {
			((MacDumb) mac).setNetEntity(netEntity, intId);
		} else {
			throw new Exception("MAC could not be linked with NET, because MAC type is not recognized!");
		}
	}

	protected void addNodeToVisualizer() {
		
		if (visualizer != null) {
        	visualizer.setBaseTranmit(250);
        	visualizer.addNode(location.getX(), location.getY(), id);
        	visualizer.drawTransmitCircle(id);
        }
	}
	
	/** 
	 * Link network object with routing object (vice versa, i.e. the ink from routing to net)
	 * is done by the addRouting method already.
	 */
	protected void linkNetAndRouting() {
		net.setRouting(routeEntity);
		if (net instanceof NetIpGeo) ((NetIpGeo) net).setRoutingNonEntity(route);
	}
	
	/**
	 * Create radio object according to config parameters. Does not set up links to other entities.
	 * @param id Node id
	 * @param radioInfo Shared radio info
	 * @throws Exception 
	 */
	protected void addRadio(int id, RadioInfo.RadioInfoShared radioInfo) throws Exception {
		String noiseOpt = options.getStringProperty(SimParams.NOISE_TYPE);
		log.debug("  Creating noise model: "+noiseOpt);

        if (noiseOpt.equals(SimParams.NOISE_TYPE_INDEPENDENT)) {
            radio = new RadioNoiseIndep(id, radioInfo);
			log.debug("  Added independent noise");
        } else if (noiseOpt.equals(SimParams.NOISE_TYPE_ADDITIVE)) {
            radio = new RadioNoiseAdditive(id, radioInfo);
            log.debug("  Added additive noise");
            
            DucksRadioNoiseAdditiveStats radioStats = null;
            try {
                // get aodv stats, and automatically create and register, if necessary
                radioStats = (DucksRadioNoiseAdditiveStats) stats.getStaticCollector(DucksRadioNoiseAdditiveStats.class, true);
            } catch (Exception e) {
                
            }
            ((RadioNoiseAdditive)radio).setStats(radioStats);            
		} else {
			throw new Exception("Unknown noise type: "+noiseOpt);
		}
	    radioEntity = radio.getProxy();
	}
	
	/**
	 * Create MAC object according to config parameters. Does not set up links to other entities.
	 * @param id Node ID
	 * @throws Exception
	 */
	protected void addMac(int id) throws Exception{
		String macOpt = options.getStringProperty(SimParams.MAC_PROTOCOL);
		boolean promisc = options.getBooleanProperty(SimParams.MAC_PROMISCUOUS);
		
		if (macOpt.equals(SimParams.MAC_PROTOCOL_802_11)) {
			Mac802_11 mac802 = new Mac802_11(new MacAddress(id), radio.getRadioInfo());
			
			DucksMac80211Stats macStats = null;
            try {
                // get aodv stats, and automatically create and register, if necessary
                macStats = (DucksMac80211Stats) stats.getStaticCollector(DucksMac80211Stats.class, true);
            } catch (Exception e) {
                
            }
            mac802.setStats(macStats);    
			
			this.mac = mac802;
			macEntity = mac802.getProxy();
			log.debug("  Added 802.11 MAC module");
			if (promisc) {
				mac802.setPromiscuous(true);
				log.debug("  Set 802.11 MAC module to promiscuous mode");
			}
		} else if (macOpt.equals(SimParams.MAC_PROTOCOL_DUMB)) {
			MacDumb macDumb = new MacDumb(new MacAddress(id), radio.getRadioInfo());
			this.mac = macDumb;
			macEntity = macDumb.getProxy();
			log.debug("  Added Dumb MAC module");
			if (promisc) {
				macDumb.setPromiscuous(true);
				log.debug("  Set Dumb MAC module to promiscuous mode");
			}
		} else {
			throw new Exception("Unknown MAC layer: "+macOpt);
		}
	}
	
	/**
	 * Add network layer implementation (not routing, but network
	 * interfaces with own IP address and interface queue as well as
	 * a protocol demultiplexer). Routing runs besides the network layer
	 * independently.
	 * Does not set up links to oher entities (Routing, MAC, or Protocols)
	 * @param id Node id
	 * @param protocolMap Protocol mapper to use
	 * @param inLoss
	 * @param outLoss
	 * @param field
	 */
	protected void addNetwork(int id, Mapper protocolMap, PacketLoss inLoss, PacketLoss outLoss, Field field) {
		
		final NetAddress address = new NetAddress(id);
	    //NetIpBase net = new NetIp(address, protocolMap, inLoss, outLoss);
		NetIpBase net = new NetIpGeo(address, protocolMap, inLoss, outLoss, field.getRadioData(new Integer(id)), true);	    
	    
		this.net = net;
		this.netEntity = net.getProxy();
		
		log.debug("  Added IP network module");
	}
	
	/**
	 * Create routing object. Automatically links to network entity, so this method MUST be
	 * called only after the network object is created.
	 * @param address
	 * @param protMap
	 * @param field
	 * @throws Exception
	 */
	protected void addRouting(NetAddress address, Mapper protMap, Field field) throws Exception {
		String routeOpt = options.getStringProperty(SimParams.ROUTING_PROTOCOL);
		
		if (routeOpt.equals(SimParams.ROUTING_PROTOCOL_AODV)) {
			
			RouteAodv aodv = new RouteAodv(address);			
			DucksAodvStats aodvstats = null;
			try {
				// get aodv stats, and automatically create and register, if necessary
				aodvstats = (DucksAodvStats) stats.getStaticCollector(DucksAodvStats.class, true);
			} catch (Exception e) {
				
			}
			
			aodv.setStats(aodvstats);
			aodv.setNetEntity(netEntity);
			protMap.testMapToNext(Constants.NET_PROTOCOL_AODV);
			net.setProtocolHandler(Constants.NET_PROTOCOL_AODV, aodv.getProxy());
			
			aodv.getProxy().start();
			
			route = aodv;
			routeEntity = aodv.getProxy();
			
			log.debug("  Added AODV routing module");
			
		} else if (routeOpt.equals(SimParams.ROUTING_PROTOCOL_DSR)) {
			RouteDsr dsr = new RouteDsr(address);
			dsr.setNetEntity(netEntity);
			
			DucksDsrStats dsrstats = null;
			try {
				// get aodv stats, and automatically create and register, if necessary
				dsrstats = (DucksDsrStats) stats.getStaticCollector(DucksDsrStats.class, true);
			} catch (Exception e) {
				
			}
			dsr.setStats(dsrstats);
			
			route = dsr;
			routeEntity = dsr.getProxy();
			
			protMap.testMapToNext(Constants.NET_PROTOCOL_DSR);
			net.setProtocolHandler(Constants.NET_PROTOCOL_DSR, routeEntity);
			
			log.debug("  Added DSR routing module");
        
             
		} else if (routeOpt.equals(SimParams.ROUTING_PROTOCOL_ZRP)) {
        
            RouteZrp zrp = new RouteZrp(address, 2);
            zrp.setSubProtocolsDefault();
            zrp.setNetEntity(net.getProxy());
            
			protMap.testMapToNext(Constants.NET_PROTOCOL_ZRP);
			net.setProtocolHandler(Constants.NET_PROTOCOL_ZRP, zrp.getProxy());
            
            zrp.getProxy().start();
            
            route = zrp;
            routeEntity = zrp.getProxy();
            //zrp.setStats(zrpStats);
            
			log.debug("  Added ZRP routing module ");

		} else if (routeOpt.equals(SimParams.ROUTING_PROTOCOL_CGGC)) {
			
			RouteCGGC cggc = new RouteCGGC(field.getRadioData(new Integer(id)));

			DucksCggcStats cggcstats = null;
			try {
				cggcstats = (DucksCggcStats) stats.getStaticCollector(DucksCggcStats.class, true);
			} catch (Exception e) {
				
			}
			
			cggc.setStats(cggcstats);
            cggc.setNetEntity(netEntity);
            cggc.setBeaconingActive(options.getBooleanProperty(SimParams.ROUTING_CGGC_DO_BEACONING, true));
            
			route = cggc;
			routeEntity = cggc.getProxy();
            
			protMap.testMapToNext(Constants.NET_PROTOCOL_CGGC);
			net.setProtocolHandler(Constants.NET_PROTOCOL_CGGC, routeEntity);
            
			cggc.getProxy().start();
			
			log.debug("  Added CGGC routing module");
			
		} else {
			throw new Exception("Unknown routing protocol: "+routeOpt);
		}
	    
	}
	
	/**
	 * Create application object. Automatically links to network entity, so this method MUST be
	 * called only after the network object is available.
	 * @param protMap
	 * @param field
	 * @param place
	 * @throws Exception
	 */
	protected void addApplication(Mapper protMap, Field field, Placement place) throws Exception {
		
		this.app = null;
		String appOpt = options.getStringProperty(SimParams.TRAFFIC_TYPE);
		
		if (appOpt.equals(SimParams.TRAFFIC_TYPE_CBR)) {
			int sendRate, waitTime, pPC, wTS, wTE, duration;
			sendRate = options.getIntProperty(SimParams.TRAFFIC_CBR_RATE);
			waitTime = options.getIntProperty(SimParams.TRAFFIC_CBR_WAITTIME);
			pPC      = options.getIntProperty(SimParams.TRAFFIC_CBR_PACKET_PER_CONNECTION);
			wTS      = options.getIntProperty(SimParams.TRAFFIC_WAITSTART);
			wTE      = options.getIntProperty(SimParams.TRAFFIC_WAITEND);
			duration = globalConfig.getIntProperty(SimParams.SIM_DURATION);
			
			AppCbrBase ac = new AppCbrIp(sendRate, waitTime, wTS, wTE, pPC, this.id, this.nodeCount, duration);
			
			protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
			net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, (NetInterface.NetHandler) ac.getNetProxy());
			
			this.app = ac;
			this.appEntity = ac.getAppProxy();
			ac.setNetEntity(this.netEntity);
			this.appEntity.run();
			
			stats.registerStaticCollector(ac);
			log.debug("  Added Cbr application module");

		} else if (appOpt.equals(SimParams.TRAFFIC_TYPE_CBRGEO)) {
			int sendRate, waitTime, pPC, wTS, wTE, duration;
			sendRate = options.getIntProperty(SimParams.TRAFFIC_CBR_RATE);
			waitTime = options.getIntProperty(SimParams.TRAFFIC_CBR_WAITTIME);
			pPC      = options.getIntProperty(SimParams.TRAFFIC_CBR_PACKET_PER_CONNECTION);
			wTS      = options.getIntProperty(SimParams.TRAFFIC_WAITSTART);
			wTE      = options.getIntProperty(SimParams.TRAFFIC_WAITEND);
			duration = globalConfig.getIntProperty(SimParams.SIM_DURATION);
			
			AppCbrBase ac;
			
			// Unfortunately it is not possible to simply use StreetPlacement :-(
			if (field.getMobility() instanceof StreetMobility) {
				ac = new AppCbrGeo(sendRate, waitTime, wTS, wTE, pPC, this.id,
						this.nodeCount, duration, field.getRadioData(new Integer(id)), 
						(StreetMobility) field.getMobility());
			} else {
				Location2D fieldsize = new Location2D(globalConfig.getIntProperty(SimParams.SCENE_NAMEPSPACE+"."+SimParams.SCENE_FIELD_SIZE_X),
							  globalConfig.getIntProperty(SimParams.SCENE_NAMEPSPACE+"."+SimParams.SCENE_FIELD_SIZE_Y));
				ac = new AppCbrGeo(sendRate, waitTime, wTS, wTE, pPC, this.id,
						this.nodeCount, duration, field.getRadioData(new Integer(id)), place, fieldsize);
			}
		    
			protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
			net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, (NetInterface.NetHandler) ac.getNetProxy());
			
			this.app = ac;
			this.appEntity = ac.getAppProxy();
			ac.setNetEntity(this.netEntity);
			this.appEntity.run();
			
			stats.registerStaticCollector(ac);
			log.debug("  Added CbrGeo application module");
		} else if (appOpt.equals(SimParams.TRAFFIC_TYPE_GEOCAST)) {
			
			float msgPerMin;
			int minRad, maxRad, wTS, wTE, duration;
			msgPerMin = options.getFloatProperty(SimParams.TRAFFIC_GC_MSG_PER_MIN);
			minRad    = options.getIntProperty(SimParams.TRAFFIC_GC_MIN_RADIUS);
			maxRad    = options.getIntProperty(SimParams.TRAFFIC_GC_MAX_RADIUS);
			wTS       = options.getIntProperty(SimParams.TRAFFIC_WAITSTART, 5);
			wTE       = options.getIntProperty(SimParams.TRAFFIC_WAITEND, 5);
			duration  = globalConfig.getIntProperty(SimParams.SIM_DURATION);
			
			AppGeocast appgc = new AppGeocast(this.id, field.getRadioData(new Integer(id)), 
					minRad, maxRad, null, msgPerMin, wTS, wTE, duration);
			
			// for statistics setup (may be left out in config file)
			int statOutputType = options.getIntProperty(SimParams.TRAFFIC_GC_STAT_TYPE,0);
			String statParam   = options.getStringProperty(SimParams.TRAFFIC_GC_STAT_PARAM,"0");
			AppGeocast.setStatisticOutput(statOutputType, statParam);
			
			protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
			net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, (NetInterface.NetHandler) appgc.getNetProxy());
			
			this.app = appgc;
			this.appEntity = appgc.getAppProxy();
			appgc.setNetEntity(this.netEntity);
			this.appEntity.run();
			
			stats.registerStaticCollector(appgc);
			log.debug("  Added AppGeocast application module");
			
		} else {
			log.warn("  Did NOT add any application layer object!");
		}
	}

}