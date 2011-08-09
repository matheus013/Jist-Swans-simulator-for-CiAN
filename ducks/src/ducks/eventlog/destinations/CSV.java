/*
 * Ulm University DUCKS project
 * 
 * Author: Stefan Schlott <stefan.schlott@uni-ulm.de>
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
package ducks.eventlog.destinations;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import jist.swans.misc.Location;
import ducks.eventlog.EventLog;

/**
 * Write log data into a CSV file Required parameter:
 * ducks.eventlog.dest.CSV.outputfile Optional parameter:
 * ducks.eventlog.dest.CSV.separator
 * 
 * The outputfilename may contain placeholders {your.property.name}, which will
 * be replaced by the given property of the simulation instance. For example,
 * "trace-{ducks.general.nodes}.txt" will create a file named "trace-100.txt" if
 * the simulation instance had a setting "ducks.general.nodes=100".
 * 
 * @author Stefan Schlott
 */
public class CSV extends EventLog
{
    PrintWriter out;
    String      sep = "\t";

    @Override
    public void configure(Properties config, String configprefix) {
        String datafilename = config.getProperty(configprefix + ".outputfile");
        String separator = config.getProperty(configprefix + ".separator");
        try {
            out = new PrintWriter(new FileOutputStream(configStringReplacer(config, datafilename), false));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (separator != null)
            sep = separator;
    }

    @Override
    public void finalize() {
        out.flush();
    }

    @Override
    public void logEvent(int node, long time, Location loc, String type, String comment) {
        if (type == null)
            type = "";
        if (comment == null)
            comment = "";
        if (type.contains(sep))
            type = "\"" + type + "\"";
        if (comment.contains(sep))
            comment = "\"" + comment + "\"";
        out.println(node + sep + time + sep + loc.getX() + sep + loc.getY() + sep + type + sep + comment);
    }

}
