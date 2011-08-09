/**
 * 
 */
package vans.route;

import vans.route.RouteCGGC.CggcStats;
import ext.util.ExtendedProperties;
import ext.util.stats.StatsCollector;

/**
 * @author mfeiri
 * 
 */
public class DucksCggcStats extends CggcStats implements StatsCollector {

	/*
	 * (non-Javadoc)
	 * 
	 * @see extensions.misc.StatsCollector#getStatParams()
	 */
	public String[] getStatParams() {
		String[] stats = new String[5];
		stats[0] = "ducks.routing.cggc.beacon.sent";
		stats[1] = "ducks.routing.cggc.beacon.recv";
		stats[2] = "ducks.routing.cggc.beacon.updt";
		stats[3] = "ducks.routing.cggc.packet.recv";
		stats[4] = "ducks.routing.cggc.packet.sent";
		return stats;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see extensions.misc.StatsCollector#getStats()
	 */
	public ExtendedProperties getStats() {
		ExtendedProperties stats = new ExtendedProperties();
		stats.put("ducks.routing.cggc.beacon.sent", Long.toString(sentBeacon));
		stats.put("ducks.routing.cggc.beacon.recv", Long.toString(recvBeacon));
		stats.put("ducks.routing.cggc.beacon.updt", Long.toString(updtBeacon));
		stats.put("ducks.routing.cggc.packet.recv", Long.toString(recvPacket));
		stats.put("ducks.routing.cggc.packet.sent", Long.toString(sentPacket));
		return stats;
	}

}
