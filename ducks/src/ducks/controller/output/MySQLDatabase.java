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
package ducks.controller.output;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import ducks.controller.simulation.Simulation;
import ducks.controller.simulation.SimulationInstance;
import ducks.controller.simulation.SimulationStudy;
import ducks.misc.DucksException;

import ext.util.ExtendedProperties;
import ext.util.StringUtils;

public class MySQLDatabase implements StorageProvider {

	// log4j Logger
	private static Logger log = Logger.getLogger(MySQLDatabase.class.getName());

	// Database connection
	public static final String HOSTNAME = "hostname";
	public static final String PORT = "port";
	public static final String DATABASENAME = "dbname";
	public static final String AUTH_USERNAME = "username";
	public static final String AUTH_PASSWORD = "password";

	private String dbHost;
	private String dbName;
	private String dbPort = "3306"; // Default MySQL port
	private String dbUsername;
	private String dbPassword;

	private Connection dbConnection;

	// Structure of database
	// - table for simulation study (i.e. 1 entry per config file)
	// - table for simulation configurations (including all input parameters)
	// (1:n)
	// - table for simulation instances (included all output parameters) (1:n)
	// (should contain both foreign keys (of simu study and simu config)

	public static final String DB_SIMU_STUD = "simu_stud";
	public static final String DB_SIMU_CONF = "simu_conf";
	public static final String DB_SIMU_INST = "simu_inst";

	public static final String PRIMARY_KEY = "ID";
	public static final String STUDY_FOREIGN_KEY = "StudyID";
	public static final String CONFIG_FOREIGN_KEY = "ConfigID";

	// Relation between objects and IDs (since we only have IDs in the database)
	private HashMap<Object, Long> simuIds = new HashMap<Object, Long>();

	// Initialization, opening & closing
	// ..........................................

	public void configure(ExtendedProperties config) throws DucksException {

		dbHost = config.getProperty(HOSTNAME);
		String port = config.getProperty(PORT);
		if (port != null) {
			dbPort = port;
		}
		dbName = config.getProperty(DATABASENAME);
		dbUsername = config.getProperty(AUTH_USERNAME);
		dbPassword = config.getProperty(AUTH_PASSWORD);

		if (dbHost == null || dbName == null || dbUsername == null
				|| dbPassword == null) {
			throw new DucksException(
					"Missing result database params in config file");
		}

		dbHost.trim();
		dbPort.trim();
		dbName.trim();
		dbUsername.trim();
		dbPassword.trim();
	}

	public void enable() throws DucksException {

		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			dbConnection = DriverManager.getConnection("jdbc:mysql://" + dbHost
					+ ":" + dbPort + "/" + dbName, dbUsername, dbPassword);
		} catch (Exception e) {
			dbConnection = null;
			throw new DucksException("Could not connect to database");
		}
	}

	public void disable() throws DucksException {

		try {
			if (!dbConnection.getAutoCommit()) {
				// can not commit, if auto commit is true
				dbConnection.commit();
			}
			dbConnection.close();
			dbConnection = null;
		} catch (SQLException e) {
			throw new DucksException("DB disconnect failed: " + e.getMessage());
		}
	}

	// Saving of data
	// .............................................................
	public void saveSimulationStudy(SimulationStudy study)
			throws DucksException {

		// if the object has been saved yet:
		Long id = simuIds.get(study);
		if (id != null)
			return;

		// check database connection
		if (dbConnection == null)
			throw new DucksException("No connection to db");

		// create structure, if required
		ExtendedProperties fields = new ExtendedProperties();
		fields.put("Description", "TEXT");
		fields.put("StartDate", "DATE");

		boolean consistent = checkDatabaseStructure(DB_SIMU_STUD, PRIMARY_KEY,
				null, fields, true, true);
		if (!consistent) {
			throw new DucksException(
					"Database structure not consistent. Skipping saving of study! ");
		}

		String query = "INSERT INTO " + DB_SIMU_STUD
				+ " (Description,StartDate) VALUES ('" + study.getDescription()
				+ "','" + StringUtils.formatDateTime(new Date()) + "');";
		long studyID = -1;
		log.debug("Inserting simulation run: " + query);

		try {
			Statement s = dbConnection.createStatement();
			s.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = s.getGeneratedKeys();

			rs.next();
			studyID = rs.getLong(1);
			simuIds.put(study, new Long(studyID));
			log.debug("Successfully saved simulation study under ID=" + studyID);
		} catch (SQLException e) {
			throw new DucksException(
					"Simulation study could not be saved to MySQL database: "
							+ e.getMessage());
		}
	}

	/**
	 * Save simulation configuration, including the results of all contained
	 * instances.
	 * 
	 * @param simu
	 *            The simulation to save
	 */
	public void saveSimulation(Simulation simu) throws DucksException {

		Long studyID = simuIds.get(simu.getSimStudy());
		log.debug("Saving simu under study ID " + studyID.toString());
		if (studyID == null) {
			log.error("Simulation study ID was not found when saving simulation config. Skipping saving");
			throw new DucksException(
					"Simulation study ID not found when saving simu config!");
		}

		// what do we need to save
		ExtendedProperties config = simu.getSimuConfig();
		String excParams = config
				.getProperty(StorageManager.CFG_EXCLUDE_PARAMS);
		String incParams = config
				.getProperty(StorageManager.CFG_INCLUDE_PARAMS);
		// NOTE: incParams and excParams may be null intentionally!!
		ExtendedProperties fields = config.getFilteredSet(incParams, excParams);

		// check structure
		checkDatabaseStructure(DB_SIMU_CONF, PRIMARY_KEY,
				new String[] { STUDY_FOREIGN_KEY }, fields, true, false);

		// save param values
		long simuID;
		Long simuIDObj;
		try {
			fields.put(STUDY_FOREIGN_KEY, studyID.toString());
			simuID = saveValues(DB_SIMU_CONF, fields);
			simuIDObj = new Long(simuID);
			log.debug("Successfully saved simulation configuration under ID="
					+ simuID);
			simuIds.put(simu, simuIDObj);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DucksException(
					"Simulation config could not be saved to DB: "
							+ e.getMessage());
		}

		// save results
		String excResults = config
				.getProperty(StorageManager.CFG_EXCLUDE_RESULTS);
		String incResults = config
				.getProperty(StorageManager.CFG_INCLUDE_RESULTS);
		Vector<SimulationInstance> instances = simu.getFinishedInstances();
		for (int i = 0; i < instances.size(); i++) {
			SimulationInstance si = instances.get(i);
			ExtendedProperties results = si.getResults();
			if (results == null) {
				log.warn("Simulation instance " + si.getIdentifier()
						+ " did not provide results!");
				continue;
			}
			results = results.getFilteredSet(incResults, excResults);

			checkDatabaseStructure(DB_SIMU_INST, PRIMARY_KEY, new String[] {
					STUDY_FOREIGN_KEY, CONFIG_FOREIGN_KEY }, results, true,
					false);

			// save result values
			try {
				// Caution: Adding 'long' values to the Properties object is
				// syntactically correct,
				// but leaves 'null' references instead of the values. Thus, add
				// as Long object or String
				results.put(STUDY_FOREIGN_KEY, studyID.toString());
				results.put(CONFIG_FOREIGN_KEY, simuIDObj.toString());

				saveValues(DB_SIMU_INST, results);
			} catch (Exception e) {
				e.printStackTrace();
				throw new DucksException(
						"Simulation instance could not be saved to DB: "
								+ e.getMessage());
			}

		}

	}

	/**
	 * Check the table structure of a certain table, and create fields, if
	 * wanted.
	 * 
	 * @param table
	 *            The table to check. Will also be created, if addDynamically is
	 *            true
	 * @param priKeyField
	 *            Name of the primary key field, if table needs to be created
	 * @param foreignKeys
	 *            Names of foreign keys, that will be also created as
	 *            bigint(20), when the table needs to be created
	 * @param fields
	 *            Properties that contain the fields to check. Only the names
	 *            will be taken, unless the <code>propertyValueIsDataType</code>
	 *            indicates to use the value as corresponding data type
	 * @param addDynamically
	 *            Add table and fields, if they are not there yet. otherwise
	 *            return false.
	 * @param propertyValueIsDataType
	 *            Use the value of the fields property as datatype, if true
	 */
	private boolean checkDatabaseStructure(String table, String priKeyField,
			String[] foreignKeys, ExtendedProperties fields,
			boolean addDynamically, boolean propertyValueIsDataType) {

		// check if table is available
		try {
			DatabaseMetaData dbmd = dbConnection.getMetaData();
			String[] objectCategories = { "TABLE" };
			ResultSet tabs = dbmd.getTables(dbName, null, null,
					objectCategories);

			boolean found = false;
			while (tabs.next()) {
				String tab = tabs.getString("TABLE_NAME");
				log.debug("Found table: " + tab);
				if (tab.equals(table))
					found = true;
			}

			if (!found) {
				log.info("checkDB: table " + table + " not found");
				if (addDynamically) {
					// table not found, instant adding wanted
					Statement s = dbConnection.createStatement();
					String query = "CREATE TABLE `" + table + "` ("
							+ priKeyField
							+ " bigint(20) unsigned NOT NULL auto_increment, ";
					if (foreignKeys != null) {
						for (int i = 0; i < foreignKeys.length; i++) {
							query += foreignKeys[i]
									+ " bigint(20) unsigned NOT NULL, ";
						}
					}
					query += "PRIMARY KEY (" + priKeyField
							+ ") ) ENGINE=MyISAM DEFAULT CHARSET=latin1;";
					log.debug("Executing: " + query);
					try {
						s.execute(query);
					} catch (SQLException sqle) {
						log.debug("Executing query failed: " + query
								+ " Exception: " + sqle.getMessage());
						return false;
					}

				} else {
					// table not found, no instant adding wanted
					log.debug("checkDB: table "
							+ table
							+ " not found, dynamic adding not wanted -> DB inconsistent");
					return false;
				}
			} else {
				// table was found
				log.debug("checkDB: table " + table + " already exists");
			}

		} catch (Exception e) {
			log.debug("Checking for tables failed: " + e.getMessage());
			return false;
		}

		// check fields .......................................
		log.debug("CheckDB: Checking fields ...");
		ExtendedProperties wantedfields = fields.cloneProperties();

		try {
			Statement s = dbConnection.createStatement();
			ResultSet cols = s.executeQuery("SELECT * FROM `" + table
					+ "` WHERE 1=2");
			ResultSetMetaData rsmd = cols.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				String col = rsmd.getColumnName(i);
				log.debug("CheckDB: Found column: " + col);
				wantedfields.remove(col);
			}
		} catch (Exception e) {
			log.warn("Checking for table columns failed: " + e.getMessage());
			return false;
		}

		// now, only the fields remain which are not in the table
		if (addDynamically) {
			// create columns, that were not found
			Enumeration newFields = wantedfields.keys();
			while (newFields.hasMoreElements()) {
				String fieldname = (String) newFields.nextElement();
				String datatype = propertyValueIsDataType ? wantedfields
						.getProperty(fieldname) : "TEXT";
				try {
					Statement s = dbConnection.createStatement();
					s.execute("ALTER TABLE `" + table + "` ADD COLUMN `"
							+ fieldname + "` " + datatype + ";");
				} catch (Exception e) {
					e.printStackTrace();
					log.warn("Could not add column " + fieldname + " ("
							+ datatype + ") to table " + table + ": "
							+ e.getMessage());
					return false;
				}
			}
		} else {
			if (wantedfields.size() > 0) {
				// have fields not in the table, but do not want to add
				// dynamically
				log.debug("CheckDB: Columns were noet found, no dynamic adding wanted - "
						+ wantedfields.toString());
				return false;
			}
		}

		return true;
	}

	/**
	 * Save a properties list to the database. CAUTION: Only valid properties
	 * are allowed as column names, and values must not contain ' signs!!
	 * Otherwise, the query will fail!
	 * 
	 * @param table
	 *            The table to add the data
	 * @param props
	 *            The properties where the key is used as column name, the value
	 *            as row value
	 * @return The ID of the row in the rable
	 * @throws Exception
	 */
	private long saveValues(String table, ExtendedProperties props)
			throws Exception {

		// Build query
		// Note: Tables must be embraced by ` signs, but values must not!
		// Instead, values need ' or " signs
		//
		String q = "INSERT INTO " + table + " SET ";
		Enumeration fields = props.keys();
		while (fields.hasMoreElements()) {
			String name = (String) fields.nextElement();
			String val = props.getProperty(name);
			q += "`" + name + "`='" + val + "'";
			if (fields.hasMoreElements()) {
				q += ", ";
			}
		}
		q += ";";

		log.debug("Execute: " + q);

		Statement s = dbConnection.createStatement();
		s.execute(q, Statement.RETURN_GENERATED_KEYS);

		ResultSet rs = s.getGeneratedKeys();
		rs.next();
		long configID = rs.getLong(1);
		return configID;
	}

}