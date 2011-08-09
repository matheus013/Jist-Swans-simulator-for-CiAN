package vans.net;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;

import org.apache.log4j.Logger;

import ext.util.ExtendedProperties;
import ext.util.StringUtils;
import ext.util.stats.StatsCollector;

/**
 * Beaconing is a class that can send out beacon messages periodically via
 * broadcast in 1 hop distance. The period between two messages can be jittered.
 * Beacon messages carry a variable amount of BeaconData objects which are
 * fetched from external objects if they register for being called when a beacon
 * is to be sent. Moreover, the Beaconing class provides listener interfaces
 * that allow external objects to be notified after a beacon was sent or
 * received.
 * 
 * Note: The Beaconing class does not comprise neighbor table functionality.
 * There is another class called "NeighborTable" which provides that. This class
 * can be registered with the Beaconing to be notified when a beacon is received
 * and sent.
 * 
 * @author Elmar Schoch
 * 
 */
public class Beaconing implements NetInterface.NetHandler, AppInterface,
		StatsCollector {

	// log4j Logger
	private static Logger log = Logger.getLogger(Beaconing.class.getName());

	/**
	 * BeaconData class is a container to hold a type and object that is sent
	 * along with beacons.
	 */
	public static class BeaconData {

		public int type;
		public int size;
		public Object data;
	}

	/**
	 * BeaconDataProvider interface must be implemented by external objects that
	 * want to add data to beacons
	 */
	public static interface BeaconDataProvider {
		public BeaconData getBeaconData();
	}

	/**
	 * The BeaconMessage is the class of object that the Beaconing sends. It
	 * basically consists of an array of beacon data objects that is assembled
	 * right before the beacon is sent.
	 */
	public static class BeaconMessage implements Message {

		public static final int BASE_SIZE = 10;
		public HashMap<Integer, BeaconData> beaconData;

		public void getBytes(byte[] msg, int offset) {
			new RuntimeException("BeaconMessage.getBytes not implemented");
		}

		public int getSize() {
			int size = BASE_SIZE;
			if (beaconData != null) {
				for (BeaconData bdata : beaconData.values()) {
					// Add only some bytes for the type field etc.
					size += 2;
					if (bdata != null) {
						size += bdata.size;
					}
				}
			}
			return size;
		}

	}

	/**
	 * The interface BeaconReceiveListener must be implemented by external
	 * objects that want to be informed when a beacon message is received.
	 */
	public static interface BeaconReceiveListener {
		public void beaconReceived(NetAddress src, MacAddress lastHop,
				BeaconMessage msg, int macId);
	}

	public static interface BeaconSendListener {
		public void beaconSent();
	}

	// Proxy objects
	private NetInterface netEntity;
	private Object self;

	public static final String CFG_INTERVAL = "beaconing.interval";
	public static final String CFG_JITTER = "beaconing.jitter";
	public static final int CFG_INTERVAL_DEFAULT = 1000;
	public static final int CFG_JITTER_DEFAULT = 500;

	// Basic settings
	public long BeaconInterval = CFG_INTERVAL_DEFAULT * Constants.MILLI_SECOND;
	public long BeaconJitter = CFG_JITTER_DEFAULT * Constants.MILLI_SECOND;

	private int here;
	private HashMap<Integer, BeaconDataProvider> beaconDataProviders;
	private Vector<BeaconReceiveListener> beaconReceiveListeners;
	private Vector<BeaconSendListener> beaconSendListeners;

	private static int beaconsReceived = 0;
	private static int beaconsSent = 0;

	private static String STATS_BEACONING_RECV = "beaconing.recv";
	private static String STATS_BEACONING_SENT = "beaconing.sent";

	public Beaconing(int thisNode) {

		beaconDataProviders = new HashMap<Integer, BeaconDataProvider>();
		beaconReceiveListeners = new Vector<BeaconReceiveListener>();
		beaconSendListeners = new Vector<BeaconSendListener>();

		this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class,
				NetInterface.NetHandler.class });

		here = thisNode;
	}

	// ///////////////////////////////////////////////////////////////////////
	// Regular sending of beacons

	private long getWaitingTime() {
		return BeaconInterval - BeaconJitter
				+ (long) (2 * BeaconJitter * Constants.random.nextFloat());
	}

	private void sendBeacon() {

		log.debug(StringUtils.timeSeconds() + " - Node " + here
				+ " sends beacon");

		BeaconMessage msg = new BeaconMessage();
		msg.beaconData = assembleBeaconData();

		netEntity.send(msg, NetAddress.ANY, Constants.NET_PROTOCOL_HEARTBEAT,
				Constants.NET_PRIORITY_NORMAL, (byte) 1);

		beaconsSent++;
	}

	// //////////////////////////////////////////////////////////////////////
	// Implementation of interfaces

	public void run(String[] args) {

		if (JistAPI.getTime() == 0) {
			JistAPI.sleep(getWaitingTime());
		}

		// send out beacon
		sendBeacon();

		// notify listeners that a beacon was sent.
		// This can be used e.g. to purge neighbors in the neighbor table.
		notifyBeaconSendListeners();

		// schedule next
		JistAPI.sleep(getWaitingTime());

		((AppInterface) self).run();
	}

	public void run() {
		run(null);
	}

	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {

		if (msg instanceof BeaconMessage) {
			log.debug(StringUtils.timeSeconds() + " - Node " + here
					+ " received beacon from " + lastHop);

			// notify every listener that a beacon was received
			notifyBeaconReceiveListeners(src, lastHop, macId,
					(BeaconMessage) msg);

			beaconsReceived++;
		}
	}

	// ///////////////////////////////////////////////////////////////////////
	// JiST entity handling

	public void setNetEntity(NetInterface netEntity) {
		this.netEntity = netEntity;
	}

	public NetInterface.NetHandler getNetHandlerProxy() {
		return (NetInterface.NetHandler) this.self;
	}

	public AppInterface getAppProxy() {
		return (AppInterface) this.self;
	}

	// /////////////////////////////////////////////////////////////////////
	// Beacon data handling

	public void registerBeaconDataProvider(int type, BeaconDataProvider provider) {
		beaconDataProviders.put(new Integer(type), provider);
	}

	private HashMap<Integer, BeaconData> assembleBeaconData() {
		if (beaconDataProviders.size() == 0) {
			return null;
		}
		HashMap<Integer, BeaconData> beaconDataMap = new HashMap<Integer, BeaconData>();
		Iterator<Integer> it = beaconDataProviders.keySet().iterator();
		while (it.hasNext()) {
			Integer type = it.next();
			BeaconDataProvider provider = beaconDataProviders.get(type);
			BeaconData bdata = provider.getBeaconData();
			beaconDataMap.put(type, bdata);
		}
		return beaconDataMap;
	}

	// //////////////////////////////////////////////////////////////////
	// Receive listener handling

	public void registerBeaconReceiveListener(BeaconReceiveListener listener) {
		beaconReceiveListeners.add(listener);
	}

	private void notifyBeaconReceiveListeners(NetAddress src,
			MacAddress lastHop, int macId, BeaconMessage msg) {
		for (BeaconReceiveListener listener : beaconReceiveListeners) {
			listener.beaconReceived(src, lastHop, msg, macId);
		}
	}

	// //////////////////////////////////////////////////////////////////
	// Send listener handling

	public void registerBeaconSendListener(BeaconSendListener listener) {
		beaconSendListeners.add(listener);
	}

	private void notifyBeaconSendListeners() {
		for (BeaconSendListener listener : beaconSendListeners) {
			listener.beaconSent();
		}
	}

	public String[] getStatParams() {
		String[] params = new String[2];
		params[0] = STATS_BEACONING_RECV;
		params[1] = STATS_BEACONING_SENT;
		return params;
	}

	public ExtendedProperties getStats() {
		ExtendedProperties props = new ExtendedProperties();
		log.debug("Beacons rx: " + beaconsReceived + " tx: " + beaconsSent);
		props.put(STATS_BEACONING_RECV, beaconsReceived);
		props.put(STATS_BEACONING_SENT, beaconsSent);
		return props;
	}

}
