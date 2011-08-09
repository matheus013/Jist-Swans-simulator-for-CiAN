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

import java.util.Random;

import jist.swans.Constants;
import jist.swans.field.Fading;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.PathLoss;
import jist.swans.field.Spatial;
import jist.swans.misc.Location.Location2D;

import org.apache.log4j.Logger;

import vans.straw.StreetMobility;
import ducks.misc.DucksException;
import ext.util.ExtendedProperties;
import ext.util.ReflectionUtils;
import ext.util.stats.MultipleStatsCollector;

/**
 * GenericScene represents the environment in a simulation, i.e containing the
 * field and all its components like radio propagation models and so on.
 * 
 * @author Elmar Schoch
 * 
 */
public class GenericScene implements Scene {

	// log4j Logger
	private static Logger log = Logger.getLogger(GenericScene.class.getName());

	protected ExtendedProperties options;
	protected ExtendedProperties globalConfig;
	protected MultipleStatsCollector statsCollector = new MultipleStatsCollector();

	protected Field field;

	public Field getField() {
		return field;
	}

	public void setGlobalConfig(ExtendedProperties config) {
		this.globalConfig = config;
	}

	public void configure(ExtendedProperties config) throws DucksException {
		this.options = config;

		try {
			field = createField();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DucksException(e.getMessage());
		}

		statsCollector.addStatParam("ducks.env.connectivity.density");
		statsCollector.addStatParam("ducks.env.connectivity.sensing");
		statsCollector.addStatParam("ducks.env.connectivity.receive");
		statsCollector.addStatParam("ducks.env.overall.nodespeed");
	}

	public ExtendedProperties getConfig() {
		return this.options;
	}

	public String[] getStatParams() {
		return statsCollector.getStatParams();
	}

	public ExtendedProperties getStats() {

		statsCollector.putStats("ducks.env.connectivity.density",
				Double.toString(field.computeDensity() * 1000 * 1000));
		statsCollector.putStats("ducks.env.connectivity.sensing",
				Double.toString(field.computeAvgConnectivity(true)));
		statsCollector.putStats("ducks.env.connectivity.receive",
				Double.toString(field.computeAvgConnectivity(false)));
		statsCollector.putStats("ducks.env.overall.nodespeed",
				computeAvgNodeSpeed());

		return statsCollector.getStats();
	}

	public String computeAvgNodeSpeed() {
		Mobility mobility = field.getMobility();
		if (mobility instanceof StreetMobility) {
			try {
				return ((StreetMobility) mobility).printAverageSpeed(
						globalConfig.getIntProperty(SimParams.SIM_DURATION),
						false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return "";
	}

	/**
	 * Build field
	 * 
	 * @throws DucksException
	 */
	private Field createField() throws Exception {

		// initialize node mobility model ..............................
		Mobility mobility = createMobility();

		// initialize spatial binning (defaults like in aodvsim)
		// ..........................................
		Location2D corners[] = new Location2D[4];
		if (options.getStringProperty(SimParams.MOBILITY_MODEL).equals(
				SimParams.MOBILITY_MODEL_STRAW_SIMPLE)
				|| options.getStringProperty(SimParams.MOBILITY_MODEL).equals(
						SimParams.MOBILITY_MODEL_STRAW_OD)) {

			// invoke method via reflection (allows "soft" binding)
			Location2D[] cornersTemp;
			try {
				cornersTemp = (Location2D[]) ReflectionUtils.invokeMethod(
						"vans.straw.StreetMobility", "getBounds", null,
						mobility, null);
			} catch (Exception e) {
				throw new DucksException(e.getMessage());
			}

			corners[0] = cornersTemp[2];
			corners[1] = cornersTemp[3];
			corners[2] = cornersTemp[0];
			corners[3] = cornersTemp[1];
		} else {
			float x, y;
			try {
				x = options.getFloatProperty(SimParams.SCENE_FIELD_SIZE_X);
				y = options.getFloatProperty(SimParams.SCENE_FIELD_SIZE_Y);
			} catch (Exception e) {
				throw new DucksException(e.getMessage());
			}

			corners[0] = new Location2D(0, 0);
			corners[1] = new Location2D(x, 0);
			corners[2] = new Location2D(0, y);
			corners[3] = new Location2D(x, y);
		}

		Spatial spatial = createSpatial(corners);

		// initialize pathloss
		PathLoss pathloss = createPathloss();

		// initialize fading
		Fading fading = createFading();

		return new Field(spatial, fading, pathloss, mobility,
				Constants.PROPAGATION_LIMIT_DEFAULT);
	}

	private Mobility createMobility() throws Exception {

		// initialize node mobility model ..............................
		Mobility mobility = null;
		String mobilityModel = options
				.getStringProperty(SimParams.MOBILITY_MODEL);
		log.debug("Creating mobility " + mobilityModel);

		// get field size (only for non-STRAW mobility models)
		Location2D bounds = null;
		if (!(mobilityModel.equals(SimParams.MOBILITY_MODEL_STRAW_SIMPLE) || mobilityModel
				.equals(SimParams.MOBILITY_MODEL_STRAW_OD))) {
			float x = options.getFloatProperty(SimParams.SCENE_FIELD_SIZE_X);
			float y = options.getFloatProperty(SimParams.SCENE_FIELD_SIZE_Y);
			bounds = new Location2D(x, y);
		}

		if (mobilityModel.equals(SimParams.MOBILITY_MODEL_STATIC)) {
			mobility = new Mobility.Static();
		}

		if (mobilityModel.equals(SimParams.MOBILITY_MODEL_RWP)) {

			long pt = options.getLongProperty(SimParams.MOBILITY_RWP_PAUSETIME)
					* Constants.SECOND;
			float prec = options
					.getFloatProperty(SimParams.MOBILITY_RWP_PRECISION);
			float smin = options
					.getFloatProperty(SimParams.MOBILITY_RWP_SPEED_MIN);
			float smax = options
					.getFloatProperty(SimParams.MOBILITY_RWP_SPEED_MAX);

			mobility = new Mobility.RandomWaypoint(bounds, pt, prec, smin, smax);
			log.debug("Initialized RWP mobility: pausetime=" + pt + " prec="
					+ prec + " minspeed=" + smin + " maxspeed=" + smax);
		}

		if (mobilityModel.equals(SimParams.MOBILITY_MODEL_TELEPORT)) {
			mobility = new Mobility.Teleport(bounds, Long.parseLong(options
					.getProperty(SimParams.MOBILITY_TELEPORT_PAUSETIME)));
		}

		if (mobilityModel.equals(SimParams.MOBILITY_MODEL_WALK)) {

			double fr = options
					.getDoubleProperty(SimParams.MOBILITY_WALK_FIXEDRADIUS);
			double rr = options
					.getDoubleProperty(SimParams.MOBILITY_WALK_RANDOMRADIUS);
			long pt = options
					.getLongProperty(SimParams.MOBILITY_WALK_PAUSETIME)
					* Constants.SECOND;

			mobility = new Mobility.RandomWalk(bounds, fr, rr, pt);
		}

		if (mobilityModel.equals(SimParams.MOBILITY_MODEL_STRAW_SIMPLE)
				|| mobilityModel.equals(SimParams.MOBILITY_MODEL_STRAW_OD)) {

			float trLong = options
					.getFloatProperty(SimParams.MOBILITY_STRAW_LONG_MAX);
			float trLat = options
					.getFloatProperty(SimParams.MOBILITY_STRAW_LAT_MAX);
			float blLong = options
					.getFloatProperty(SimParams.MOBILITY_STRAW_LONG_MIN);
			float blLat = options
					.getFloatProperty(SimParams.MOBILITY_STRAW_LAT_MIN);

			Location2D tr = new Location2D(trLong, trLat);
			Location2D bl = new Location2D(blLong, blLat);

			String segmentFile = options
					.getProperty(SimParams.MOBILITY_STRAW_SEGMENTMAP);
			String streetFile = options
					.getProperty(SimParams.MOBILITY_STRAW_STREETMAP);
			String shapeFile = options
					.getProperty(SimParams.MOBILITY_STRAW_SHAPEMAP);

			int degree = options
					.getIntProperty(SimParams.MOBILITY_STRAW_DEGREE);
			int granularity = options
					.getIntProperty(SimParams.MOBILITY_STRAW_GRANULARITY);
			double probability = options
					.getDoubleProperty(SimParams.MOBILITY_STRAW_PROBABILITY);

			Class[] paramTypes = null;
			Object[] params = null;
			String className = null;
			if (mobilityModel.equals(SimParams.MOBILITY_MODEL_STRAW_SIMPLE)) {
				paramTypes = new Class[] { String.class, String.class,
						String.class, int.class, double.class, int.class,
						Location2D.class, Location2D.class, Random.class };
				params = new Object[] { segmentFile, streetFile, shapeFile,
						degree, probability, granularity, bl, tr,
						Constants.random };
				className = "vans.straw.StreetMobilityRandom";
			} else if (mobilityModel.equals(SimParams.MOBILITY_MODEL_STRAW_OD)) {
				paramTypes = new Class[] { String.class, String.class,
						String.class, int.class, Location2D.class,
						Location2D.class, Random.class };
				params = new Object[] { segmentFile, streetFile, shapeFile,
						degree, bl, tr, Constants.random };
				className = "vans.straw.StreetMobilityOD";
			}
			// create object using reflection
			try {
				mobility = (Mobility) ReflectionUtils.createObject(className,
						paramTypes, params);
			} catch (Exception e) {
				throw new DucksException("Could not create STRAW mobility: "
						+ e.getMessage());
			}

		}

		// ** Pedestrians - Social Force **
		if (mobilityModel.equals(SimParams.MOBILITY_MODEL_SOCIALFORCE)) {
			long duration = globalConfig
					.getLongProperty(SimParams.SIM_DURATION) * Constants.SECOND;

			long pausetime = options
					.getLongProperty(SimParams.MOBILITY_SOCIALFORCE_PAUSETIME)
					* Constants.SECOND;
			float speedmax = options
					.getFloatProperty(SimParams.MOBILITY_SOCIALFORCE_SPEED_MAX);
			int precision = options
					.getIntProperty(SimParams.MOBILITY_SOCIALFORCE_PRECISION);
			boolean relaxation = true;
			if (options.contains(SimParams.MOBILITY_SOCIALFORCE_USE_RELAXATION))
				relaxation = options
						.getBooleanProperty(SimParams.MOBILITY_SOCIALFORCE_USE_RELAXATION);
			int force_mode = 2;
			if (options.contains(SimParams.MOBILITY_SOCIALFORCE_FORCE_MODE))
				force_mode = options
						.getIntProperty(SimParams.MOBILITY_SOCIALFORCE_FORCE_MODE);
			int fluctuation_mode = 1;
			if (options
					.contains(SimParams.MOBILITY_SOCIALFORCE_FLUCTUATION_MODE))
				fluctuation_mode = options
						.getIntProperty(SimParams.MOBILITY_SOCIALFORCE_FLUCTUATION_MODE);

			// mobility = new Pedestrian(loc, duration, pausetime, speedmax,
			// precision,
			// relaxation, (fluctuation_mode>0), (fluctuation_mode>1),
			// (force_mode>0), (force_mode>1));

			// create object via reflection to enable soft binding
			try {
				Class[] paramTypes = new Class[] { Location2D.class,
						long.class, long.class, float.class, int.class,
						boolean.class, boolean.class, boolean.class,
						boolean.class, boolean.class };
				Object[] params = new Object[] { bounds, duration, pausetime,
						speedmax, precision, relaxation,
						(fluctuation_mode > 0), (fluctuation_mode > 1),
						(force_mode > 0), (force_mode > 1) };
				mobility = (Mobility) ReflectionUtils.createObject(
						"privacy.socialforce.Pedestrian", paramTypes, params);
			} catch (Exception e) {
				throw new DucksException(
						"Socialforce mobility model could not be loaded: "
								+ e.getMessage());
			}

		}

		if (mobilityModel.equals(SimParams.MOBILITY_MODEL_GPSREPLAY)) {
			long delay = options
					.getIntProperty(SimParams.MOBILITY_GPSREPLAY_DELAY)
					* Constants.SECOND;
			int precision = options
					.getIntProperty(SimParams.MOBILITY_GPSREPLAY_PRECISION);
			float maxspeed = options
					.getFloatProperty(SimParams.MOBILITY_GPSREPLAY_MAXSPEED);
			String files = options
					.getStringProperty(SimParams.MOBILITY_GPSREPLAY_FILES);

			// mobility = new
			// GPSReplay(loc,delay,precision,maxspeed,options.getProperty(SimParams.MOBILITY_GPSREPLAY_FILES));

			// create object via reflection to enable soft binding
			try {
				Class[] paramTypes = new Class[] { Location2D.class,
						long.class, int.class, float.class, String.class };
				Object[] params = new Object[] { bounds, delay, precision,
						maxspeed, files };
				mobility = (Mobility) ReflectionUtils.createObject(
						"privacy.gpsreplay.GPSReplay", paramTypes, params);
			} catch (Exception e) {
				throw new DucksException("Could not load GPSReplay mobility: "
						+ e.getMessage());
			}
		}

		if (mobilityModel.equalsIgnoreCase(SimParams.MOBILITY_MODEL_REPLAY)) {

			int precision = options
					.getIntProperty(SimParams.MOBILITY_REPLAY_PRECISION);
			String readerClass = options
					.getStringProperty(SimParams.MOBILITY_REPLAY_CLASS);
			String file = options
					.getStringProperty(SimParams.MOBILITY_REPLAY_FILE);

			// mobility = new MobilityReplay(loc,precision,file,readerClass);

			// create object via reflection to enable soft binding
			try {
				Class[] paramTypes = new Class[] { Location2D.class, int.class,
						String.class, String.class };
				Object[] params = new Object[] { bounds, precision, file,
						readerClass };
				mobility = (Mobility) ReflectionUtils.createObject(
						"ext.jist.swans.mobility.MobilityReplay", paramTypes,
						params);
			} catch (Exception e) {
				throw new DucksException("Could not load MobilityReplay: "
						+ e.getMessage());
			}

		}

		if (mobility == null) {
			throw new DucksException("Mobility model could not be created: "
					+ mobilityModel);
		}

		return mobility;
	}

	private Spatial createSpatial(Location2D[] corners) throws Exception {

		// initialize spatial binning
		int spatial_div = 5; // like in aodvsim
		Spatial spatial = null;
		String binning = options.getStringProperty(SimParams.SPATIAL,
				SimParams.SPATIAL_HIER);

		log.debug("Creating spatial model " + binning);

		if (binning.equals(SimParams.SPATIAL_LINEAR)) {
			spatial = new Spatial.LinearList(corners[0], corners[1],
					corners[2], corners[3]);
		} else if (binning.equals(SimParams.SPATIAL_GRID)) {
			spatial = new Spatial.Grid(corners[0], corners[1], corners[2],
					corners[3], spatial_div);
		} else if (binning.equals(SimParams.SPATIAL_HIER)) {
			spatial = new Spatial.HierGrid(corners[0], corners[1], corners[2],
					corners[3], spatial_div);
		} else {
			throw new DucksException("Unknown spatial binning model: "
					+ binning);
		}

		boolean wrap = options.getBooleanProperty(SimParams.SPATIAL_WRAPAROUND,
				false);

		if (wrap) {
			spatial = new Spatial.TiledWraparound(spatial);
		}

		return spatial;
	}

	private PathLoss createPathloss() throws Exception {

		PathLoss pathloss = null;
		String loss = options.getStringProperty(SimParams.PATHLOSS);
		log.debug("Creating pathloss: " + loss);

		if (loss.equals(SimParams.PATHLOSS_FREE_SPACE)) {
			pathloss = new PathLoss.FreeSpace();
		}

		if (loss.equals(SimParams.PATHLOSS_TWO_RAY)) {
			pathloss = new PathLoss.TwoRay();
		}

		if (pathloss == null) {
			throw new DucksException("PathLoss not initialized properly: "
					+ loss);
		} else {
			return pathloss;
		}
	}

	private Fading createFading() throws Exception {
		Fading fading;
		String fadProp = options.getStringProperty(SimParams.FADING);
		log.debug("Creating fading model: " + fadProp);

		if (fadProp.equals(SimParams.FADING_NONE)) {
			fading = new Fading.None();
		} else if (fadProp.equals(SimParams.FADING_RAYLEIGH)) {
			fading = new Fading.Rayleigh();
		} else if (fadProp.equals(SimParams.FADING_RICIAN)) {
			double kfactor = Double.parseDouble(options
					.getProperty(SimParams.FADING_RICIAN_KFACTOR));
			fading = new Fading.Rician(kfactor);
		} else {
			throw new DucksException("Unknown fading model: " + fadProp);
		}

		return fading;
	}

}
