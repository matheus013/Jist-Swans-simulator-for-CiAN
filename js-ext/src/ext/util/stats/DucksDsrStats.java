/*
 * Ulm University DUCKS project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
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
package ext.util.stats;

import jist.swans.route.RouteDsr.DsrStats;
import ext.util.ExtendedProperties;

/**
 * StatsCollector - capable version of DsrStats for DUCKS
 * 
 * @author Elmar Schoch
 * 
 */
public class DucksDsrStats extends DsrStats implements StatsCollector
{

    public ExtendedProperties getStats() {
        ExtendedProperties stats = new ExtendedProperties();

        stats.put("ducks.routing.dsr.rreq.sent.orig", Long.toString(originated.rreqPackets));
        stats.put("ducks.routing.dsr.rreq.sent.forward", Long.toString(forwarded.rreqPackets));
        stats.put("ducks.routing.dsr.rreq.recv", Long.toString(recv.rreqPackets));

        stats.put("ducks.routing.dsr.rrep.sent.orig", Long.toString(originated.rrepPackets));
        stats.put("ducks.routing.dsr.rrep.sent.forwarded", Long.toString(forwarded.rrepPackets));
        stats.put("ducks.routing.dsr.rrep.recv", Long.toString(recv.rrepPackets));

        stats.put("ducks.routing.dsr.rerr.sent.orig", Long.toString(originated.rerrPackets));
        stats.put("ducks.routing.dsr.rerr.sent.forward", Long.toString(forwarded.rerrPackets));
        stats.put("ducks.routing.dsr.rerr.recv", Long.toString(recv.rerrPackets));

        stats.put("ducks.routing.dsr.total.sent.orig", Long.toString(originated.totalDsrPackets()));
        stats.put("ducks.routing.dsr.total.sent.forward", Long.toString(forwarded.totalDsrPackets()));
        stats.put("ducks.routing.dsr.total.recv", Long.toString(recv.totalDsrPackets()));
        return stats;
    }

    public String[] getStatParams() {
        String[] stats = new String[12];
        stats[0] = "ducks.routing.dsr.rreq.sent.orig";
        stats[1] = "ducks.routing.dsr.rreq.sent.forward";
        stats[2] = "ducks.routing.dsr.rreq.recv";
        stats[3] = "ducks.routing.dsr.rrep.sent.orig";
        stats[4] = "ducks.routing.dsr.rrep.sent.forward";
        stats[5] = "ducks.routing.dsr.rrep.recv";
        stats[6] = "ducks.routing.dsr.rerr.sent.orig";
        stats[7] = "ducks.routing.dsr.rerr.sent.forward";
        stats[8] = "ducks.routing.dsr.rerr.recv";
        stats[9] = "ducks.routing.dsr.total.sent.orig";
        stats[10] = "ducks.routing.dsr.total.sent.forward";
        stats[11] = "ducks.routing.dsr.total.recv";
        return stats;
    }

}
