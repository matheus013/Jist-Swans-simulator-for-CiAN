package vans.route;

import java.util.Iterator;
import java.util.Map;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Field.RadioData;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.route.RouteInterface;

import org.apache.log4j.Logger;

import vans.net.NetAddressGeo;
import ext.util.CacheMap;
import ext.util.CacheSet;
import ext.util.Region;

/**
 * Cached Greedy GeoCast
 * 
 * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
 */

public class RouteCGGC implements RouteInterface.Cggc {

	// statistics collection classes
	public static class CggcStats {
		public long recvBeacon;
		public long sentBeacon;
		public long updtBeacon;
		public long sentPacket;
		public long recvPacket;

		public void clear() {
			recvBeacon = 0;
			sentBeacon = 0;
			updtBeacon = 0;
			sentPacket = 0;
			recvPacket = 0;
		}
	}

	/** statistics accumulator. */
	private CggcStats stats;

	private RouteInterface.Cggc self;
	private RadioData localRadio;
	private NetInterface net;

	private CacheMap neighbourTable;
	private CacheSet packetCache;

	private long nextBeaconing;
	private boolean doBeaconing;

	// log4j Logger
	private static Logger log = Logger.getLogger(RouteCGGC.class.getName());

	public static class GeoBeacon implements Message {
		public Location senderPos;

		public GeoBeacon(Location pos) {
			senderPos = pos;
		}

		public int getSize() {
			return 0; /* senderPos.size(); */
		} // comparability to cggc ns-2

		public void getBytes(byte[] b, int offset) {
			throw new RuntimeException("not implemented");
		}

		public String toString() {
			return "Beacon" + senderPos;
		}
	}

	// macId used as the key for values like this
	private static class NeighbourEntry {
		public Location loc;
		public byte intId;
		public long receiveTime;
	}

	private static class PacketEntry {
		public PacketEntry(NetMessage.Ip netM) {
			nm = netM;
			receiveTime = JistAPI.getTime();
		}

		public NetMessage.Ip nm;
		public long receiveTime;
	}

	private static final Byte hopLoc = new Byte((byte) 138);
	private static final long BEACONING_INTERVAL = 1 * Constants.SECOND;
	private static final long BEACONING_JITTER = BEACONING_INTERVAL / 4;
	private static final long BEACON_EXPIRY_TIME = 2 * BEACONING_INTERVAL;
	private static final long PACKET_EXPIRY_TIME = 10 * Constants.MINUTE;
	private static final int BEACON_CACHE_SIZE = 100;
	private static final int PACKET_CACHE_SIZE = 10;

	/**
	 * Create and initialize new CGGC instance.
	 * 
	 * @param netEntity
	 *            network entity
	 * @param loc
	 *            geographic location
	 */
	public RouteCGGC(RadioData rd) {
		this.self = (RouteInterface.Cggc) JistAPI.proxy(this,
				RouteInterface.Cggc.class);
		this.localRadio = rd;

		neighbourTable = new CacheMap(BEACON_CACHE_SIZE);// HashMap();
		packetCache = new CacheSet(PACKET_CACHE_SIZE);// HashSet();

		doBeaconing = true;
	}

	/**
	 * Must use Protocol interface because scheduling events in simulation time
	 * is not allowed in the constructor.
	 */
	public void start() {
		self.timeout();
	}

	public void setBeaconingActive(boolean active) {
		doBeaconing = active;
	}

	// ////////////////////////////////////////////////
	// RouteInterface functions
	//

	/**
	 * Called by the network layer for every incoming packet. A routing
	 * implementation may wish to look at these packets for informational
	 * purposes, but should not change their contents.
	 * 
	 * @param msg
	 *            incoming packet
	 * @param lastHop
	 *            last link-level hop of incoming packet
	 */
	public void peek(NetMessage msg, byte interfaceId, MacAddress lastHop) {

		// sanity check
		if (!(msg instanceof NetMessage.Ip))
			throw new IllegalArgumentException("illegal packet type");
		NetMessage.Ip ipMsg = (NetMessage.Ip) msg;

		// process options
		NetMessage.IpOption ipOpt = (NetMessage.IpOptionHopLoc) ipMsg
				.getOptions().get(hopLoc);
		if (ipOpt != null)
			updateLmp(interfaceId,
					((NetMessage.IpOptionHopLoc) ipOpt).getLoc(), lastHop);

		// update stats
		if (stats != null) {
			stats.updtBeacon++;
		}

	}

	public void dropNotify(Message packet, MacAddress packetNextHop) {
		// 1. Adjust the neighbourhood table
		neighbourTable.remove(packetNextHop);
		// 2. resend the packet
		NetMessage.Ip ipMsg = ((NetMessage.Ip) packet).copy();

		if (!sendLine(ipMsg)) {
			packetCache.offer(new PacketEntry(ipMsg));
		}
	}

	/**
	 * Called by the network layer to request transmission of a packet that
	 * requires routing. It is the responsibility of the routing layer to
	 * provide a best-effort transmission of this packet to an appropriate next
	 * hop by calling the network layer sending routines once this routing
	 * information becomes available.
	 * 
	 * @param msg
	 *            outgoing packet
	 */
	public void send(NetMessage msg) {
		if (!(msg instanceof NetMessage.Ip))
			throw new IllegalArgumentException("illegal packet type");
		NetMessage.Ip ipMsg = (NetMessage.Ip) msg;

		if (stats != null) {
			stats.recvPacket++;
		}

		if (!sendLine(ipMsg)) {
			packetCache.offer(new PacketEntry(ipMsg));
		}
	}

	protected boolean sendLine(NetMessage.Ip ipMsg) {

		Region dest = ((NetAddressGeo) ipMsg.getDst()).getRegion();
		long timeLimit = JistAPI.getTime() - BEACON_EXPIRY_TIME;
		double distance = dest.distance(localRadio.getLoc());
		MacAddress nextHop = null;
		int nextId = 0;

		// find a neighbour that is geographically closer to the destination
		Iterator it = neighbourTable.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry e = (Map.Entry) it.next();
			NeighbourEntry nd = (NeighbourEntry) e.getValue();
			if (nd.receiveTime < timeLimit) {
				// it.remove();
				continue; // break; really need a reverse iterator!
			}
			double compareMe = dest.distance(nd.loc);
			if (compareMe >= distance)
				continue;
			// success
			distance = compareMe;
			nextHop = (MacAddress) e.getKey();
			nextId = nd.intId;
		}

		// send
		if (nextHop != null) {
			net.send(ipMsg, nextId, nextHop);
			if (stats != null) {
				stats.sentPacket++;
			}
			return true;
		}

		return false;
	}

	// ////////////////////////////////////////////////
	// NetHandler functions
	//

	/**
	 * Receive a message from network layer.
	 * 
	 * @param msg
	 *            message received
	 * @param src
	 *            source network address
	 * @param lastHop
	 *            source link address
	 * @param macId
	 *            incoming interface
	 * @param dst
	 *            destination network address
	 * @param priority
	 *            packet priority
	 * @param ttl
	 *            packet time-to-live
	 */
	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {

		// log.info(JistAPI.getTime() +" node="+ net.getAddress() + " received"
		// + msg + " from=" +
		// src + " for=" + dst + " at="+ localRadio.getLoc()
		// +" via="+lastHop+" on="+macId+" prio="+priority+" ttl="+ttl);

		if (stats != null) {
			stats.recvBeacon++;
		}

		updateLmp(macId, ((GeoBeacon) msg).senderPos, lastHop);
	}

	private void updateLmp(byte macId, Location l, MacAddress lastHop) {
		// update table
		NeighbourEntry nd = new NeighbourEntry();
		nd.intId = macId;
		nd.receiveTime = JistAPI.getTime();
		nd.loc = l;
		neighbourTable.put(lastHop, nd);

		// try to deliver packets in chache using this new information
		long timeLimit = JistAPI.getTime() - PACKET_EXPIRY_TIME;
		Iterator it = packetCache.iterator();
		while (it.hasNext()) {
			PacketEntry pe = (PacketEntry) it.next();
			// if (pe.receiveTime < timeLimit) break;
			if (pe.receiveTime < timeLimit)
				continue;

			Region dest = ((NetAddressGeo) pe.nm.getDst()).getRegion();
			if (dest.distance(l) < dest.distance(localRadio.getLoc())) {
				net.send(pe.nm, macId, lastHop);
				if (stats != null) {
					stats.sentPacket++;
				}
				it.remove();
			}

			// movement of this node might make old beacons attractive again.
			// this does
			// not necessarily improve delivery though because beacons quickly
			// get stale
			// if (sendLine(pe.nm)) it.remove();

		}

	}

	/**
	 * Return self-referencing proxy entity.
	 * 
	 * @return self-referencing proxy entity
	 */
	public RouteInterface.Cggc getProxy() {
		return self;
	}

	/**
	 * Sets cggc statistics object.
	 * 
	 * @param stats
	 *            cggc statistics object
	 */
	public void setStats(CggcStats stats) {
		this.stats = stats;
	}

	/**
	 * Sets network entity.
	 * 
	 * @param netEntity
	 *            network entity
	 */
	public void setNetEntity(NetInterface netEntity) {
		this.net = netEntity;
	}

	public void scheduleNextBeacon() {
		long delta = BEACONING_INTERVAL - BEACONING_JITTER
				+ Util.randomTime(2 * BEACONING_JITTER);
		nextBeaconing = JistAPI.getTime() + delta;
		JistAPI.sleep(delta);
		self.timeout();
	}

	public void timeout() {

		if (!doBeaconing)
			return;

		// another beacon was sent and scheduled, skip this event
		if (JistAPI.getTime() < nextBeaconing)
			return;

		// beaconing
		Message msg = new GeoBeacon(localRadio.getLoc());
		net.send(msg, NetAddress.ANY, Constants.NET_PROTOCOL_CGGC,
				Constants.NET_PRIORITY_NORMAL, (byte) 0);

		// stats
		if (stats != null) {
			stats.sentBeacon++;
		}

		// schedule next invocation
		scheduleNextBeacon();
	}

}
