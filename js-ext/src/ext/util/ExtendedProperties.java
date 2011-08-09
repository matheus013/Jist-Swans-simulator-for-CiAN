/*
 * Ulm University JiS/SWANS extensions project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package ext.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import jist.runtime.JistAPI.DoNotRewrite;

import org.apache.log4j.Logger;

/**
 * Extended properties class, e.g. implementing some properties loading
 * mechanisms
 * 
 * @author Elmar Schoch
 * 
 */
public class ExtendedProperties extends Properties implements DoNotRewrite
{

    /**
     * 
     */
    private static final long  serialVersionUID  = 1L;

    private static Logger      log               = Logger.getLogger(ExtendedProperties.class.getName());

    public static final String DEFAULT_SEPARATOR = "#";
    public static final String NEWLINE_SEPARATOR = "\n";
    public static final int    FILTER_INCLUDE    = 1;
    public static final int    FILTER_EXCLUDE    = 2;

    /**
     * Test for valid Java property names Valid is a string consisting of
     * substrings separated by dots, where the substrings may contain any
     * alphanumeric character and some special chars like "_" , but not e.g.
     * "*, /, -" etc. Moreover, dots must not occur at the beginning or at the
     * end.
     * 
     * @param propertyName
     *            The property name to test
     * @return True if property name is ok, false otherwise
     */
    public static boolean isValidPropertyName(String propertyName) {
        String regex = "^\\w+(\\.\\w+)*$";
        return propertyName.matches(regex);
    }

    // Advanced loading and storing functionality
    // ..................................
    /**
     * Load properties from file
     * 
     * @param filename
     *            File to load
     * @throws IOException
     *             Occurs if loading failed (e.g. file not found)
     */
    public void loadFromFile(String filename) throws IOException {

        File f = new File(filename);
        FileInputStream fin = new FileInputStream(f.getCanonicalPath());
        doLoad(fin);
        fin.close();

    }

    /**
     * Load properties from a string, where properties are split by newlines
     * (like in a file)
     * 
     * @param in
     *            The properties string to load
     * @throws IOException
     */
    public void loadFromString(String in) throws IOException {

        ByteArrayInputStream ins = new ByteArrayInputStream(in.getBytes());
        doLoad(ins);
    }

    /**
     * Load properties from a single line string, where properties are separated
     * with a separating character, e.g. '#'
     * 
     * @param props
     *            The properties string to load
     */
    public void loadFromString(String props, String separator) throws Exception {

        String[] propStrings = props.split(separator);
        for (int i = 0; i < propStrings.length; i++) {
            try {
                String[] property = propStrings[i].split("=");
                String key = property[0].trim();
                String val = "";
                if (property.length == 2) {
                    val = property[1].trim();
                }
                super.setProperty(key, val);
            } catch (Exception e) {
                throw new Exception("Error parsing property " + propStrings[i]);
            }
        }
    }

    private void doLoad(InputStream input) throws IOException {
        super.load(input);
        // As the load method of the Java implementation of Properties does not
        // trim tailing white spaces, do it here
        Enumeration pnames = super.propertyNames();
        while (pnames.hasMoreElements()) {
            String k = (String) pnames.nextElement();
            super.setProperty(k, super.getProperty(k).trim());
        }
    }

    /**
     * Saves properties to a string that is loadable by the "loadFromString"
     * method
     * 
     * @return
     */
    public String saveToString(String separator) {

        String propString = "";
        Enumeration pnames = super.propertyNames();
        while (pnames.hasMoreElements()) {
            String k = (String) pnames.nextElement();
            propString += k + "=" + super.getProperty(k);
            if (pnames.hasMoreElements()) {
                propString += separator;
            }
        }
        // System.out.println(propString);
        return propString;
    }

    /**
     * Save to separated string with '#' as separator
     * 
     * @return
     */
    public String saveToString() {
        return saveToString(DEFAULT_SEPARATOR);
    }

    /**
     * Saves properties to a string that is loadable by the "loadFromString"
     * method
     * 
     * @return
     */
    public String saveToSortedString(String separator) {

        String propString = "";
        Enumeration pnames = super.propertyNames();
        ArrayList sortnames = new ArrayList();
        while (pnames.hasMoreElements()) {
            sortnames.add(pnames.nextElement());
        }
        Collections.sort(sortnames);

        Iterator names = sortnames.iterator();
        while (names.hasNext()) {
            String k = (String) names.next();
            propString += k + "=" + super.getProperty(k);
            if (names.hasNext()) {
                propString += separator;
            }
        }
        return propString;
    }

    // Advanced property set manipulation
    // .........................................

    /**
     * Returns all properties in the given namespace, where namespace in this
     * case means a common prefix. To retrieve e.g. all properties
     * ducks.results.*, give "ducks.results" as namespace parameter. Notes: <li>
     * Wildcards are currently not supported!</li> <li>The result will inlcud a
     * property matching the namespace exactly!</li>
     * 
     * @nameSpace the namespace to extract
     */
    public ExtendedProperties getNamespace(String nameSpace, boolean fullNames) {
        ExtendedProperties props = new ExtendedProperties();

        nameSpace = nameSpace.trim();
        if (nameSpace.endsWith(".")) {
            nameSpace = nameSpace.substring(0, nameSpace.length() - 2);
        }

        Enumeration pnames = super.propertyNames();
        while (pnames.hasMoreElements()) {
            String k = ((String) pnames.nextElement()).trim();
            if (k.startsWith(nameSpace)) {
                if (fullNames) {
                    props.put(k, super.getProperty(k));
                } else {
                    props.put(k.substring(nameSpace.length() + 1), super.getProperty(k));
                }
            }
        }
        return props;
    }

    /**
     * Returns a hashmap that contains the properties of several subnamespaces
     * under the given namespace. For instance, take the following example:
     * <code>
     * 	a.b.c
     *  a.b.d
     *  a.e.f
     *  g.h.i
     * </code> If we call the method with "a" as parameter, then it will return
     * a map with two entries "b" and "e", both assigned a set of properties
     * (for "b", it would be "a.b.c" and "a.b.d" and for "e" it would contain
     * "a.e.f".
     * 
     * Furthermore, the resulting properties can be set to contain the full
     * qualifying name of the property, or only the last part (e.g. "c" and "d"
     * in the properties of "b").
     * 
     * @param nameSpace
     *            the namespace prefix
     * @param fullNames
     *            flag whether to include full qualifying property names in
     *            result properties
     * @return
     */
    public HashMap<String, ExtendedProperties> getSubNamespaces(String nameSpace, boolean fullNames) {
        HashMap<String, ExtendedProperties> result = new HashMap<String, ExtendedProperties>();

        // find out how may hierarchy steps we have in base namespace
        // ... first remove eventually trailing dot character
        nameSpace = nameSpace.trim();
        if (nameSpace.endsWith(".")) {
            nameSpace = nameSpace.substring(0, nameSpace.length() - 2);
        }
        String[] nsparts = nameSpace.split("\\.");

        Enumeration pnames = super.propertyNames();
        while (pnames.hasMoreElements()) {
            String k = (String) pnames.nextElement();
            k.trim();
            if (k.startsWith(nameSpace)) {

                String[] parts = k.split("\\.");
                if (parts.length <= nsparts.length) {
                    continue;
                } else {
                    String subns = parts[nsparts.length];

                    // do we have this subnamespace already in the map?
                    if (!result.containsKey(subns)) {
                        result.put(subns, new ExtendedProperties());
                    }

                    // add current key+value to properties
                    if (fullNames) {
                        result.get(subns).put(k, super.getProperty(k));
                    } else {
                        String subkey = "";
                        for (int i = nsparts.length + 1; i < parts.length; i++) {
                            subkey = subkey + parts[i];
                            if (i != parts.length - 1)
                                subkey = subkey + ".";
                        }
                        result.get(subns).put(subkey, super.getProperty(k));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns a cloned set of properties. <br>
     * Note that only the properties object is cloned, not the contained objects
     * (no deep clone).
     * 
     * @return The cloned ExtendedProperties object
     */
    public ExtendedProperties cloneProperties() {
        ExtendedProperties clone = new ExtendedProperties();
        clone.putAll(this);
        return clone;
    }

    /**
     * Returns a new properties object that contains a subset of these
     * properties, where includeFilter is a list of properties to include, and
     * excludeFilter specifically can remove some of them again, e.g. when
     * wildcards were used for include. Note that also includeFilter and
     * excludeFilter may be null, if only one filter type shall be used Example:
     * includeFilter == null, excludeFilter is given => all properties, except
     * the excluded are returned.
     * 
     * @param includeFilter
     *            A comma separated list of properties. As a wildcard, the *
     *            character is allowed. Note that properties must not contain
     *            special characters like * \ / - . + ?
     * @param excludeFilter
     *            Same as includeFilter
     * @return Filtered properties object
     */
    public ExtendedProperties getFilteredSet(String includeFilter, String excludeFilter) {

        ExtendedProperties result;

        if ((includeFilter != null) && (excludeFilter != null)) {
            result = this.getFilteredSet(FILTER_INCLUDE, includeFilter);
            result = result.getFilteredSet(FILTER_EXCLUDE, excludeFilter);
        } else if (includeFilter != null) {
            result = this.getFilteredSet(ExtendedProperties.FILTER_INCLUDE, includeFilter);
        } else if (excludeFilter != null) {
            result = this.getFilteredSet(ExtendedProperties.FILTER_EXCLUDE, excludeFilter);
        } else {
            result = this.cloneProperties();
        }

        return result;
    }

    /**
     * Returns a new, filtered properties collection. For now, filters can
     * exclude or include all properties by default and filter them accordingly.
     * 
     * @param filterType
     *            Kind of filter to use: <br>
     *            <li>FILTER_INCLUDE defaults to exclude all by default, and
     *            include all properties matching the filter <li>FILTER_EXCLUDE
     *            defaults to include all by default, and take out those
     *            matching the filter
     * @param filters
     *            The filter as a comma separated list of property names. As
     *            wildcard, * is allowed. Note that property names must not
     *            cotain special characters like / \ . * + [ ] - , to work
     *            correctly with this.
     * @return Filtered properties object
     */
    public ExtendedProperties getFilteredSet(int filterType, String filters) {

        ExtendedProperties result = new ExtendedProperties();

        List<String> filterList = StringUtils.getListFromString(filters, ",");

        switch (filterType) {
            case FILTER_INCLUDE:
                // filterInclude -> default = exclude, except given filter
                if (filters.equals("*")) {
                    result.putAll(this);
                } else {
                    for (String f : filterList) {
                        String regex = "^" + f.replace(".", "\\.") + "$"; // replace
                                                                          // .
                                                                          // with
                                                                          // \.
                        regex = regex.replace("*", "[\\w\\.]+"); // replace *
                                                                 // with
                                                                 // [\w\.]+
                        Enumeration keys = this.keys();
                        String key;
                        while (keys.hasMoreElements()) {
                            key = (String) keys.nextElement();
                            if (key.matches(regex)) {
                                result.put(key, this.get(key));
                            }
                        }
                    }
                }

                break;
            case FILTER_EXCLUDE:
                // filterExclude -> default = include, except given filter
                result.putAll(this);
                for (String filter : filterList) {
                    String regex = "^" + filter.replace(".", "\\.") + "$"; // replace
                                                                           // '.'
                                                                           // with
                                                                           // '\.'
                    regex = regex.replace("*", "[\\w\\.]+"); // replace * with
                                                             // [\w\.]+
                    Enumeration keys = result.keys();
                    String key;
                    while (keys.hasMoreElements()) {
                        key = (String) keys.nextElement();
                        if (key.matches(regex)) {
                            result.remove(key);
                        }
                    }
                }
                break;
            default:
                throw new RuntimeException("Undefined filter type");
        }

        return result;
    }

    public ExtendedProperties getFilteredSet(String[] excludeNamespaces) {

        for (String ns : excludeNamespaces) {
            ns.trim();
        }

        ExtendedProperties result = new ExtendedProperties();

        Enumeration pnames = super.propertyNames();
        while (pnames.hasMoreElements()) {
            String k = ((String) pnames.nextElement()).trim();
            if (!StringUtils.startsWith(k, excludeNamespaces)) {
                result.put(k, super.get(k));
            }
        }
        return result;
    }

    public void replaceVariables(ExtendedProperties varset) throws Exception {

        Enumeration keys = this.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String val = (String) this.getProperty(key);

            val = val.trim();
            // is the value a variable?
            if (val.startsWith("${") && val.endsWith("}")) {
                // yes, we have a variable
                // -> get variable name
                String varname = val.substring(2, val.length() - 2).trim();
                // replace with appropriate variable value
                String resolvedValue = varset.getProperty(varname);
                if (resolvedValue == null) {
                    throw new Exception("Variable " + varname + " for param " + key + " was not found!");
                }

                this.put(key, resolvedValue);
            }
        }
    }

    /**
     * Add a prefix to all propery names
     * 
     * @param prefix
     */
    public void addPrefix(String prefix) {
        Enumeration pnames = super.propertyNames();
        while (pnames.hasMoreElements()) {
            String key = ((String) pnames.nextElement());
            String val = super.getProperty(key);
            if (val == null)
                val = "";
            super.remove(key);
            super.put(prefix + "." + key, val);
        }
    }

    // Specific "put" methods for non-object values
    // ...............................
    // Problem: if the original "put" gets an int as value, it effectively adds
    // null as value which later on can lead to serious problems.

    public void put(String identifier, int value) {
        if (identifier == null)
            throw new NullPointerException("Property idenifiere must not be null");
        super.put(identifier, Integer.toString(value));
    }

    public void put(String identifier, long value) {
        if (identifier == null)
            throw new NullPointerException("Property identifier must not be null");
        super.put(identifier, Long.toString(value));
    }

    public void put(String identifier, float value) {
        if (identifier == null)
            throw new NullPointerException("Property identifier must not be null");
        super.put(identifier, Float.toString(value));
    }

    // Retrieval of properties
    // ....................................................

    public String getStringProperty(String key, String defaultValue) {
        String val = super.getProperty(key);
        if (val != null) {
            return val;
        } else {
            return defaultValue;
        }
    }

    public String getStringProperty(String key) throws Exception {
        String val = super.getProperty(key);
        if (val != null) {
            return val;
        } else {
            throw new Exception("Property " + key + " not found!");
        }
    }

    /**
     * Retrieve a boolean property along certain typical boolean values like
     * "1", "yes" or "true".
     * 
     * @param key
     *            The property to retrieve
     * @param defaultValue
     *            The default value to return, if the property is not found
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        try {
            return getBooleanProperty(key);
        } catch (Exception e) {
            log.info("Taking default value (" + defaultValue + ") for property " + key + " (" + e.getMessage() + ")");
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key) throws Exception {
        String val = super.getProperty(key);
        if (val != null) {
            return val.equals("1") || val.toLowerCase().equals("yes") || val.toLowerCase().equals("true");
        } else {
            throw new Exception("Property " + key + " not found!");
        }
    }

    public int getIntProperty(String key) throws Exception {
        int result;
        String val = super.getProperty(key);
        if (val == null) {
            throw new Exception("Property " + key + " not found!");
        }
        try {
            result = Integer.parseInt(val);
        } catch (Exception e) {
            throw new Exception("Integer conversion failed for param " + key + "=" + val);
        }
        return result;
    }

    public int getIntProperty(String key, int defaultValue) {
        try {
            return getIntProperty(key);
        } catch (Exception e) {
            log.info("Taking default value (" + defaultValue + ") for property " + key + " (" + e.getMessage() + ")");
            return defaultValue;
        }
    }

    public double getDoubleProperty(String key) throws Exception {
        double result;
        String val = super.getProperty(key);
        if (val == null) {
            throw new Exception("Property " + key + " not found!");
        }
        try {
            result = Double.parseDouble(val);
        } catch (Exception e) {
            throw new Exception("Double conversion failed for param " + key + "=" + val);
        }
        return result;
    }

    public double getDoubleProperty(String key, double defaultValue) {
        try {
            return getDoubleProperty(key);
        } catch (Exception e) {
            log.info("Taking default value (" + defaultValue + ") for property " + key + " (" + e.getMessage() + ")");
            return defaultValue;
        }
    }

    public float getFloatProperty(String key) throws Exception {
        float result;
        String val = super.getProperty(key);
        if (val == null) {
            throw new Exception("Property " + key + " not found!");
        }
        try {
            result = Float.parseFloat(val);
        } catch (Exception e) {
            throw new Exception("Float conversion failed for param " + key + "=" + val);
        }
        return result;
    }

    public float getFloatProperty(String key, float defaultValue) {
        try {
            return getFloatProperty(key);
        } catch (Exception e) {
            log.info("Taking default value (" + defaultValue + ") for property " + key + " (" + e.getMessage() + ")");
            return defaultValue;
        }
    }

    public long getLongProperty(String key) throws Exception {
        long result;
        String val = super.getProperty(key);
        if (val == null) {
            throw new Exception("Property " + key + " not found!");
        }
        try {
            result = Long.parseLong(val);
        } catch (Exception e) {
            throw new Exception("Float conversion failed for param " + key + "=" + val);
        }
        return result;
    }

    public long getLongProperty(String key, long defaultValue) throws Exception {
        try {
            return getLongProperty(key);
        } catch (Exception e) {
            log.info("Taking default value (" + defaultValue + ") for property " + key + " (" + e.getMessage() + ")");
            return defaultValue;
        }
    }

    // for testing only .................................................

    /*
     * public static void main(String[] args) { //String regex = "^"+(String)
     * args[0].replaceAll("\\.","\\\\.")+"$"; // replace '.' with '\.' String
     * regex = "^"+(String) args[0].replace(".","\\.")+"$"; // replace '.' with
     * '\.' regex = regex.replace("*", "[\\w\\.]+"); System.out.println(regex);
     * System.out.println("\\*"); //regex.replaceAll("\\*", "[\\\\w]\\*"); //
     * note: all '\' have to duplicated to get a single '\';
     * 
     * System.out.println("Testing "+args[1]+" against "+args[0]+
     * " (regex: "+regex+")"); if ( args[1].matches(regex)) {
     * System.out.println("Match");; } else { System.out.println("No match"); }
     * }
     */

    public static void main(String[] args) {
        ExtendedProperties ep = new ExtendedProperties();
        ep.put("a.b.c", "1");
        ep.put("a.b.d", "2");
        ep.put("a.e.f", "3");
        ep.put("g.h.i", "4");

        HashMap<String, ExtendedProperties> esub = ep.getSubNamespaces("a", false);
        for (String sub : esub.keySet()) {
            ExtendedProperties props = esub.get(sub);
            System.out.println("Listing props of ns " + sub);
            props.list(System.out);
        }

        String regex = "^\\w+(\\.\\w+)*$";
        String test = "abc.de.ss";
        if (test.matches(regex)) {
            System.out.println("Match");
        } else {
            System.out.println("No match");
        }

    }
}
