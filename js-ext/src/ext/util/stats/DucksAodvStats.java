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
package ext.util.stats;

import ext.util.ExtendedProperties;
import jist.swans.route.RouteAodv.AodvStats;

/**
 * StatsCollector - capable version of AodvStats for DUCKS
 * 
 * @author Elmar Schoch
 * 
 */
public class DucksAodvStats extends AodvStats implements StatsCollector {

	public ExtendedProperties getStats() {
		ExtendedProperties stats = new ExtendedProperties();

		stats.put("ducks.routing.aodv.rreq.sent",
				Long.toString(send.rreqPackets));
		stats.put("ducks.routing.aodv.rreq.recv",
				Long.toString(recv.rreqPackets));
		stats.put("ducks.routing.aodv.rrep.sent",
				Long.toString(send.rrepPackets));
		stats.put("ducks.routing.aodv.rrep.recv",
				Long.toString(recv.rrepPackets));
		stats.put("ducks.routing.aodv.rerr.sent",
				Long.toString(send.rerrPackets));
		stats.put("ducks.routing.aodv.rerr.recv",
				Long.toString(recv.rerrPackets));
		stats.put("ducks.routing.aodv.hello.sent",
				Long.toString(send.helloPackets));
		stats.put("ducks.routing.aodv.hello.recv",
				Long.toString(recv.helloPackets));
		stats.put("ducks.routing.aodv.total.sent",
				Long.toString(send.aodvPackets));
		stats.put("ducks.routing.aodv.total.recv",
				Long.toString(recv.aodvPackets));
		stats.put("ducks.routing.aodv.rreq", Long.toString(rreqOrig));
		stats.put("ducks.routing.aodv.rrep", Long.toString(rrepOrig));
		stats.put("ducks.routing.aodv.rreq.succ", Long.toString(rreqSucc));
		return stats;
	}

	public String[] getStatParams() {
		String[] stats = new String[13];
		stats[0] = "ducks.routing.aodv.rreq.sent";
		stats[1] = "ducks.routing.aodv.rreq.recv";
		stats[2] = "ducks.routing.aodv.rrep.sent";
		stats[3] = "ducks.routing.aodv.rrep.recv";
		stats[4] = "ducks.routing.aodv.rerr.sent";
		stats[5] = "ducks.routing.aodv.rerr.recv";
		stats[6] = "ducks.routing.aodv.hello.sent";
		stats[7] = "ducks.routing.aodv.hello.recv";
		stats[8] = "ducks.routing.aodv.total.sent";
		stats[9] = "ducks.routing.aodv.total.recv";
		stats[10] = "ducks.routing.aodv.rreq";
		stats[11] = "ducks.routing.aodv.rrep";
		stats[12] = "ducks.routing.aodv.rreq.succ";
		return stats;
	}

}
