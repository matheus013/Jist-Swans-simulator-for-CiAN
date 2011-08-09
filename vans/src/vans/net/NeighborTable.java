package vans.net;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import vans.net.Beaconing.BeaconMessage;
import vans.net.Beaconing.BeaconReceiveListener;
import vans.net.Beaconing.BeaconSendListener;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.mac.MacAddress;
import jist.swans.net.NetAddress;

public class NeighborTable implements BeaconReceiveListener, BeaconSendListener {

	public long Lifetime = 5 * Constants.SECOND;

	private HashMap neighbors;
	private Vector<NeighborDiscoveryListener> neighborDiscoveryListeners;
	private Vector<NeighborLostListener> neighborLostListeners;

	public static interface NeighborDiscoveryListener {
		public void neighborDiscovered(NetAddress address, NeighborInfo info);
	}

	public static interface NeighborLostListener {
		public void neighborLost(NetAddress address);
	}

	public static class NeighborInfo {

		public MacAddress macAddress;
		public long lastReceived;

		public HashMap beaconData;
	}

	public NeighborTable() {
		neighbors = new HashMap();

		neighborDiscoveryListeners = new Vector<NeighborDiscoveryListener>();
		neighborLostListeners = new Vector<NeighborLostListener>();
	}

	public NeighborInfo get(NetAddress addr) {
		return (NeighborInfo) neighbors.get(addr);
	}

	private void update(NetAddress addr, MacAddress lastHop, int interfaceId,
			BeaconMessage msg) {

		NeighborInfo ni = (NeighborInfo) neighbors.get(addr);
		if (ni == null) {
			ni = new NeighborInfo();
			neighbors.put(addr, ni);
			discoveredNeighbor(addr, ni);
		}

		if (msg.beaconData != null) {
			syncBeaconData(msg, ni);
		}

		ni.lastReceived = JistAPI.getTime();
		ni.macAddress = lastHop;
	}

	private void purge() {
		Iterator it = neighbors.keySet().iterator();
		while (it.hasNext()) {
			NetAddress addr = (NetAddress) it.next();
			NeighborInfo ni = (NeighborInfo) neighbors.get(addr);
			if (ni.lastReceived + Lifetime < JistAPI.getTime()) {
				lostNeighbor(addr, ni);
				neighbors.remove(addr);
			}
		}
	}

	private void syncBeaconData(BeaconMessage msg, NeighborInfo ni) {
		// TODO Not implemented yet
	}

	private void lostNeighbor(NetAddress addr, NeighborInfo ni) {
		for (NeighborLostListener nll : neighborLostListeners) {
			nll.neighborLost(addr);
		}
	}

	private void discoveredNeighbor(NetAddress addr, NeighborInfo ni) {
		for (NeighborDiscoveryListener ndl : neighborDiscoveryListeners) {
			ndl.neighborDiscovered(addr, ni);
		}
	}

	// //////////////////////////////////////////////////////////////////
	// Main methods that provide information from the beaconing

	public void beaconReceived(NetAddress src, MacAddress lastHop,
			BeaconMessage msg, int macId) {
		update(src, lastHop, macId, msg);
	}

	public void beaconSent() {
		purge();
	}
}
