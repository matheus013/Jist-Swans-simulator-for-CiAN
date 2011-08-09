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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import jist.swans.misc.Location;
import ducks.driver.SimParams;
import ducks.eventlog.EventLog;

/**
 * Plot log data in gnuplot file
 * 
 * Required parameter: ducks.eventlog.dest.GnuPlot.outputfile Optional
 * parameter: ducks.eventlog.dest.GnuPlot.skipfirstvalue
 * 
 * The outputfilename may contain placeholders {your.property.name}, which will
 * be replaced by the given property of the simulation instance. For example,
 * "trace-{ducks.general.nodes}.txt" will create a file named "trace-100.txt" if
 * the simulation instance had a setting "ducks.general.nodes=100".
 * 
 * @author Stefan Schlott
 * 
 */
public class GnuPlot extends EventLog
{
    String                    datafilename;
    PrintWriter               out;
    Map<Integer, PrintWriter> dataout;
    boolean                   skipfirstvalue = false;

    @Override
    public void configure(Properties config, String configprefix) {
        dataout = new Hashtable<Integer, PrintWriter>();
        datafilename = configStringReplacer(config, config.getProperty(configprefix + ".outputfile"));
        try {
            out = new PrintWriter(new FileOutputStream(datafilename, false));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        out.println("set multiplot");
        out.println("set xrange [0:"
                + config.getProperty(SimParams.SCENE_NAMEPSPACE + "." + SimParams.SCENE_FIELD_SIZE_X) + "]");
        out.println("set yrange [0:"
                + config.getProperty(SimParams.SCENE_NAMEPSPACE + "." + SimParams.SCENE_FIELD_SIZE_Y) + "]");
        String skip = config.getProperty(configprefix + ".skipfirstvalue");
        if ((skip != null) && (skip.equalsIgnoreCase("true") || skip.equals("1")))
            skipfirstvalue = true;
    }

    @Override
    public void finalize() {
        Iterator i = dataout.keySet().iterator();

        while (i.hasNext()) {
            ((PrintWriter) dataout.get(i.next())).flush();
        }
        out.flush();
    }

    @Override
    public void logEvent(int node, long time, Location loc, String type, String comment) {
        // Node movement
        if (type.equals("move")) {
            PrintWriter dout = (PrintWriter) dataout.get(node);
            if (dout == null) {
                try {
                    dout = new PrintWriter(new FileOutputStream(datafilename + "." + node, false));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                dataout.put(node, dout);
                out.println("plot '" + datafilename + "." + node + "' using 1:2 with lines notitle");
                if (skipfirstvalue)
                    return;
            }
            dout.println(loc.getX() + "\t" + loc.getY());
        }

    }

}
