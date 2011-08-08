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
package ducks.driver;

import jist.runtime.JistAPI;

/**
 * Class containing simulation parameter strings used in the DUCKS generic driver.
 * Note that these parameters are _only_ comprise parameters as understood by
 * the basic components of the generic driver (GenericScene, GenericNodes, GenericNodes).
 * More parameters may be found in classes that use the DUCKS model
 * 
 * @author Elmar Schoch
 *
 */
public class SimParams implements JistAPI.DoNotRewrite {
	
	// General parameters
	
	public static final String SIM_DURATION   = "ducks.sim.duration";
	public static final String SIM_RANDOMSEED = "ducks.sim.random.seed";
	public static final String SIM_CLASS      = "ducks.sim.class";
	public static final String SIM_CLASS_DEFAULT = "ducks.driver.GenericDriver";
	
	public static final String SCENE_NAMEPSPACE = "ducks.scene";
	public static final String SCENE_CLASS      = "ducks.scene.class";
	public static final String SCENE_CLASS_DEFAULT = "ducks.driver.GenericScene";
	
	public static final String NODES_NAMESPACE = "ducks.nodes";
	public static final String NODES_CLASS     = "ducks.nodes.class";
	public static final String NODES_CLASS_DEFAULT = "ducks.driver.GenericNodes";
	
	public static final String NODE_COUNT = "count";
	public static final String NODE_CLASS = "class";
	
	// if count=scene, then 
	public static final String NODE_COUNT_FROM_SCENE = "scene";
	
	
	// Event log
	
	public static final String EVENTLOG_DEST = "ducks.eventlog.dest";
	public static final String EVENTLOG_MODULES = "ducks.eventlog.modules";
	public static final String EVENTLOG_MODULEPREFIX = "ducks.eventlog.module.";
	
	
    // Scene parameters ...............................................................
	// The parameter constants are given relative to the Scene namespace, which means that
	// the config file entries require the scene namespace as well. For example
	// Scene namespace is "ducks.scene", parameter is fieldsize.x, then the entry in the
	// config file must be "ducks.scene.fieldsize.x"
	
	public static final String SCENE_FIELD_SIZE_X = "fieldsize.x";
	public static final String SCENE_FIELD_SIZE_Y = "fieldsize.y";
	
	public static final String SCENE_STRAWVIZ = "strawviz";
	
    // Spatial
    public static final String SPATIAL = "spatial.binning";
    public static final String SPATIAL_LINEAR = "linear";
    public static final String SPATIAL_GRID = "grid";
    public static final String SPATIAL_HIER = "hier";
   
    public static final String SPATIAL_WRAPAROUND = "spatial.binning.wraparound";
    public static final String SPATIAL_WRAPAROUND_TRUE = "true";
    public static final String SPATIAL_WRAPAROUND_FALSE = "false";
    
    // Fading
    public static final String FADING = "fading";
    public static final String FADING_NONE = "none";
    public static final String FADING_RAYLEIGH = "rayleigh";
    public static final String FADING_RICIAN = "rician";
    public static final String FADING_RICIAN_KFACTOR = "fading.rician.kfactor";
    
    // Pathloss
	public static final String PATHLOSS = "pathloss";
    public static final String PATHLOSS_FREE_SPACE = "freespace";
	public static final String PATHLOSS_TWO_RAY = "tworay";
    
	
    // Node parameters .....................................................................
	// Also all node related parameters are relative to the namespace. To support
	// different node classes, the GenericNodes implementation 
	// also supports subnamespaces. An example
	// Nodes namespace: "ducks.nodes"
	// Node subtype, let's say "static"
	// Parameter "radio.txpower"
	// => full property is "ducks.nodes.static.radio.txpower"
	
	public static final String MOBILITY_MODEL = "mobility";
	
	public static final String MOBILITY_MODEL_STATIC = "static";
	public static final String MOBILITY_MODEL_RWP = "waypoint";
	public static final String MOBILITY_MODEL_WALK = "walk";
	public static final String MOBILITY_MODEL_TELEPORT = "teleport";
	public static final String MOBILITY_MODEL_STRAW_SIMPLE = "straw-simple";
	public static final String MOBILITY_MODEL_STRAW_OD = "straw-od";
    // ** Pedestrians - Social Force **
	public static final String MOBILITY_MODEL_SOCIALFORCE = "socialforce";
	// ** GPS replay **
	public static final String MOBILITY_MODEL_GPSREPLAY = "gpsreplay";
	
	public static final String MOBILITY_MODEL_REPLAY = "replay";

	
	public static final String MOBILITY_RWP_PAUSETIME = "mobility.waypoint.pausetime";
	public static final String MOBILITY_RWP_PRECISION = "mobility.waypoint.precision";
	public static final String MOBILITY_RWP_SPEED_MIN = "mobility.waypoint.speed.min";
	public static final String MOBILITY_RWP_SPEED_MAX = "mobility.waypoint.speed.max";
	
	public static final String MOBILITY_WALK_FIXEDRADIUS = "mobility.walk.fixedradius";
	public static final String MOBILITY_WALK_RANDOMRADIUS = "mobility.walk.randomradius";
	public static final String MOBILITY_WALK_PAUSETIME = "mobility.walk.pausetime";

	public static final String MOBILITY_TELEPORT_PAUSETIME = "mobility.teleport.pausetime";
	
    // ** Pedestrians - Social Force **
	public static final String MOBILITY_SOCIALFORCE_PAUSETIME = "mobility.socialforce.pausetime";
	public static final String MOBILITY_SOCIALFORCE_SPEED_MAX = "mobility.socialforce.speed.max";
	public static final String MOBILITY_SOCIALFORCE_PRECISION = "mobility.socialforce.precision";
	public static final String MOBILITY_SOCIALFORCE_USE_RELAXATION = "mobility.socialforce.use_relaxation";
	public static final String MOBILITY_SOCIALFORCE_FORCE_MODE = "mobility.socialforce.force_mode";
	public static final String MOBILITY_SOCIALFORCE_FLUCTUATION_MODE = "mobility.socialforce.fluctuation_mode";
    
	// ** GPS replay
	public static final String MOBILITY_GPSREPLAY_DELAY = "mobility.gpsreplay.delay";
	public static final String MOBILITY_GPSREPLAY_FILES = "mobility.gpsreplay.files";
	public static final String MOBILITY_GPSREPLAY_PRECISION = "mobility.gpsreplay.precision";
	public static final String MOBILITY_GPSREPLAY_MAXSPEED = "mobility.gpsreplay.maxspeed";
	
	public static final String MOBILITY_REPLAY_PRECISION = "mobility.replay.precision";
	public static final String MOBILITY_REPLAY_CLASS = "mobility.replay.class";
	public static final String MOBILITY_REPLAY_FILE = "mobility.replay.file";
	
	public static final String MOBILITY_STRAW_LAT_MAX = "mobility.straw.latitude.max";
	public static final String MOBILITY_STRAW_LAT_MIN = "mobility.straw.latitude.min";
	public static final String MOBILITY_STRAW_LONG_MAX = "mobility.straw.longitude.max";
	public static final String MOBILITY_STRAW_LONG_MIN = "mobility.straw.longitude.min";	
	public static final String MOBILITY_STRAW_SEGMENTMAP = "mobility.straw.maps.segments";
	public static final String MOBILITY_STRAW_STREETMAP = "mobility.straw.maps.streets";
	public static final String MOBILITY_STRAW_SHAPEMAP = "mobility.straw.maps.shapes";
	public static final String MOBILITY_STRAW_DEGREE = "mobility.straw.degree";
	public static final String MOBILITY_STRAW_GRANULARITY = "mobility.straw.granularity";
	public static final String MOBILITY_STRAW_PROBABILITY = "mobility.straw.probability";
    
	// Radio
	public static final String RADIO_TX_POWER = "radio.txpower";
	public static final double RADIO_TX_POWER_250M = 7.1;
	
    // Packetloss
    public static final String PACKETLOSS_IN = "loss.in";
    public static final String PACKETLOSS_OUT = "loss.out";
    public static final String PACKETLOSS_ZERO = "zero";
    public static final String PACKETLOSS_UNIFORM = "uniform";
    public static final String PACKETLOSS_IN_UNIFORM = "loss.in.uniform";
    public static final String PACKETLOSS_OUT_UNIFORM = "loss.out.uniform";
    
	// Placement
	public static final String PLACEMENT_MODEL = "placement";
	
	public static final String PLACEMENT_MODEL_STREETRANDOM = "street-random";
	public static final String PLACEMENT_MODEL_RANDOM = "random";
	public static final String PLACEMENT_MODEL_GRID = "grid";
	
	public static final String PLACEMENT_GRID_I = "placement.i";
	public static final String PLACEMENT_GRID_J = "placement.j";
	
	// Noise
	public static final String NOISE_TYPE = "noise";
	public static final String NOISE_TYPE_INDEPENDENT = "independent";
	public static final String NOISE_TYPE_ADDITIVE = "additive";
	
	// MAC
	public static final String MAC_PROTOCOL = "mac";
	
	public static final String MAC_PROTOCOL_802_11 = "802.11";
	public static final String MAC_PROTOCOL_DUMB = "dumb";
	
	public static final String MAC_PROMISCUOUS = "mac.promiscuous";
    public static final String MAC_PROMISCUOUS_TRUE = "true";
    public static final String MAC_PROMISCUOUS_FALSE = "false";
	
	// Routing
	public static final String ROUTING_PROTOCOL = "routing";
	
	public static final String ROUTING_PROTOCOL_AODV = "aodv";
	public static final String ROUTING_PROTOCOL_DSR = "dsr";
	public static final String ROUTING_PROTOCOL_ZRP = "zrp";
	public static final String ROUTING_PROTOCOL_GPSR = "gpsr";
	public static final String ROUTING_PROTOCOL_CGGC = "cggc";
	
	public static final String ROUTING_CGGC_DO_BEACONING = "routing.cggc.beaconing";
	
	
	// Application
	public static final String TRAFFIC_TYPE = "traffic";
	
	public static final String TRAFFIC_TYPE_CBR = "cbr";
	public static final String TRAFFIC_TYPE_CBRGEO = "cbrgeo";
	public static final String TRAFFIC_TYPE_GEOCAST = "geocast";
	public static final String TRAFFIC_TYPE_SERVICE = "service";
	
	public static final String TRAFFIC_CBR_RATE = "traffic.cbr.rate";
	public static final String TRAFFIC_CBR_WAITTIME = "traffic.cbr.waittime";
	public static final String TRAFFIC_CBR_PACKET_PER_CONNECTION = "traffic.cbr.packetspercon";
	
	public static final String TRAFFIC_GC_MIN_RADIUS = "traffic.geocast.radius.min";
	public static final String TRAFFIC_GC_MAX_RADIUS = "traffic.geocast.radius.max";
	public static final String TRAFFIC_GC_MSG_PER_MIN = "traffic.geocast.msgperminute";
	public static final String TRAFFIC_GC_STAT_TYPE = "traffic.geocast.stats.type";
	public static final String TRAFFIC_GC_STAT_PARAM = "traffic.geocast.stats.param";
	
	public static final String TRAFFIC_SERV_RATE = "traffic.service.request.rate";
	public static final String TRAFFIC_SERV_INVOKE_DELAY = "traffic.service.invoke.delay";
	
	public static final String COMPOSITION_TYPE = "traffic.service.composition.type";
	public static final String COMPOSITION_RESTRICTION = "traffic.service.composition.restriction";
	public static final String COMPOSITION_RESTRICTION_NO_REPEAT = "no-repeat";
	public static final String COMPOSITION_RESTRICTION_NO_PING_PONG = "no-ping-pong";	
	public static final String COMPOSITION_TYPE_CONVENT = "conventional";
	public static final String COMPOSITION_TYPE_INTEGRA = "integrated";
	public static final String COMPOSITION_LENGTH = "traffic.service.composition.length";
	public static final String SERVICE_EXEC_TIME_PREFIX = "traffic.service.exec.time";
	
	
	public static final String TRAFFIC_WAITSTART = "app.waittime.start";
	public static final String TRAFFIC_WAITEND = "app.waittime.end";
	

}
