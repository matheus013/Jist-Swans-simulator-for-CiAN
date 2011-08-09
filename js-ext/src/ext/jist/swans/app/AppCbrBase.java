/*
 * Ulm University JiST/SWANS project
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
package ext.jist.swans.app;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import ext.util.ExtendedProperties;
import ext.util.stats.StatsCollector;

/**
 * AppCbr sends series of packets from the current node to a randomly selected
 * destination. After having sent a sequence, it waits for a certain time before
 * starting again with a different destination.
 * 
 * @author Elmar Schoch
 */
public abstract class AppCbrBase implements AppInterface,
		NetInterface.NetHandler, StatsCollector {

	/**
	 * delay before sending begins after simulation start
	 */
	public static final long CBR_STARTUP_DELAY = 5 * Constants.SECOND;

	public static final int CBR_MESSAGE_SIZE = 10;
	public static final byte CBR_MESSAGE_TTL = 64;

	/** Maximum jitter before starting a burst (in seconds) */
	public static final int CBR_JITTER = 5;

	private static final String CBR_STAT_RECV = "ducks.app.cbr.recv";
	private static final String CBR_STAT_SENT = "ducks.app.cbr.sent";
	private static final String CBR_STAT_DELAY = "ducks.app.cbr.delay";
	private static final String CBR_STAT_HOPS = "ducks.app.cbr.hops";

	/**
	 * Defines the IP protocol number since we directly use the network layer,
	 * surpassing e.g. transport layer (UDP,TCP) Note: this protocol number
	 * should be defined in jist.swans.Constants but is done here for modularity
	 * reasons
	 */
	public static final short NET_PROTOCOL_NUMBER = 524;

	// network entity.
	protected NetInterface netEntity;

	// self-referencing proxy entity.
	private Object self;

	// CBR parameters
	protected int sendRate;
	private int waitTime;
	private int waitTimeStart;
	private int waitTimeEnd;
	protected int packetsPerConnection;
	protected int nodeCount;
	protected int nodeId;
	private int duration;

	protected static long sentPackets;
	protected static long receivedPackets;

	private static long delaySum;
	private static float hopCountSum;

	/**
	 * 
	 * @param sendRate
	 *            number of messages per minute
	 * @param waitTime
	 *            time to wait before beginning a new connection (seconds)
	 * @param packetsPerConnection
	 *            number of packets to send during one connection
	 */
	public AppCbrBase(int sendRate, int waitTimeBetween, int waitTimeStart,
			int waitTimeEnd, int packetsPerConnection, int nodeId,
			int nodeCount, int duration) {

		this.sendRate = sendRate;
		this.waitTime = waitTimeBetween;
		this.waitTimeStart = waitTimeStart;
		this.waitTimeEnd = waitTimeEnd;
		this.packetsPerConnection = packetsPerConnection;
		this.nodeId = nodeId;
		this.nodeCount = nodeCount;
		this.duration = duration;

		// init self reference
		this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class,
				NetInterface.NetHandler.class });
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	// Implementations of AppInterface

	/**
	 * 
	 */
	public void run() {
		run(null);
	}

	/**
	 * Run the application
	 * 
	 * @args actually not necessary
	 */
	public void run(String[] args) {

		int jitter = Constants.random.nextInt(2 * CBR_JITTER) - CBR_JITTER;

		// staggered beginning
		if (JistAPI.getTime() == 0) {
			// assure that first start is really > 0
			while (jitter + this.waitTimeStart < 0) {
				jitter = Constants.random.nextInt(2 * CBR_JITTER) - CBR_JITTER;
			}
			// startup delay
			JistAPI.sleep((this.waitTimeStart + jitter) * Constants.SECOND);
		}

		beginMessageSequence();

		jitter = Constants.random.nextInt(2 * CBR_JITTER) - CBR_JITTER;

		// check if the next messages sequence will be finished before
		// the cool down phase at the simulation end begins. If not, do
		// not reschedule this method again.
		if (JistAPI.getTime() <= (this.duration - this.waitTimeEnd
				- this.waitTime - jitter)
				* Constants.SECOND) {

			// wait before starting next message sequence
			JistAPI.sleep((this.waitTime + jitter) * Constants.SECOND);
			((AppInterface) self).run();
		}
	}

	/**
	 * Starts sending a message sequence. A message sequence is a sequence of
	 * packets that are sent to one destination in a certain interval.
	 * 
	 */
	protected void beginMessageSequence() {
		// select a random destination
		int dst = this.nodeId;
		if (nodeCount > 1) {
			while (dst == this.nodeId) {
				dst = Constants.random.nextInt(this.nodeCount) + 1;
			}
		} else {
			// in case we only have one node in the simulation
			dst = 2;
		}
		NetAddress addr = new NetAddress(dst);

		// System.out.println(JistUtils.getSimulationTime()+": "+
		// this.nodeId+" about to send "+this.packetsPerConnection+" to "+dst);

		// calc wait time between messages
		long delay = Constants.MINUTE / this.sendRate;

		for (int i = 0; i < this.packetsPerConnection; i++) {
			// create message
			AppCbrBase.MessageCbr msg = new AppCbrBase.MessageCbr();
			msg.sentTime = JistAPI.getTime();
			msg.msgID = sentPackets + 1;

			// send message
			netEntity.send(msg, addr, Constants.NET_PROTOCOL_UDP,
					Constants.NET_PRIORITY_NORMAL, CBR_MESSAGE_TTL);

			sentPackets++;

			// wait delay
			JistAPI.sleep(delay);
		}

	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	// implementations of NetInterface

	/**
	 * Receives messages from the network layer and records their arrival
	 */
	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {

		if (msg instanceof AppCbrBase.MessageCbr) {
			AppCbrBase.MessageCbr amsg = (AppCbrBase.MessageCbr) msg;

			// System.out.println(JistUtils.getSimulationTime()+":  Node "+this.nodeId+" received message: "+amsg.msgID+" via "+lastHop+" [dst: "+dst+"]");

			if (amsg.recvTime > 0) {
				// System.out.println(JistUtils.getSimulationTime()+":     Message has reached destination already");
			} else {
				amsg.recvTime = JistAPI.getTime();
				receivedPackets++;

				long delay = amsg.recvTime - amsg.sentTime;
				delaySum += delay;

				hopCountSum -= ttl - CBR_MESSAGE_TTL;
			}
		}
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	// entity related

	/**
	 * Set network entity.
	 * 
	 * @param netEntity
	 *            network entity
	 */
	public void setNetEntity(NetInterface netEntity) {
		this.netEntity = netEntity;
	}

	/**
	 * Return self-referencing NETWORK proxy entity.
	 * 
	 * @return self-referencing NETWORK proxy entity
	 */
	public NetInterface.NetHandler getNetProxy() {
		return (NetInterface.NetHandler) self;
	}

	/**
	 * Return self-referencing APPLICATION proxy entity.
	 * 
	 * @return self-referencing APPLICATION proxy entity
	 */
	public AppInterface getAppProxy() {
		return (AppInterface) self;
	}

	/**
	 * Packets for CBR app
	 */
	public static class MessageCbr implements Message {
		/** {@inheritDoc} */
		public int getSize() {
			return AppCbrBase.CBR_MESSAGE_SIZE;
		}

		/** {@inheritDoc} */
		public void getBytes(byte[] b, int offset) {
			throw new RuntimeException("not implemented");
		}

		public long msgID = 1;
		public long sentTime = 0;
		public long recvTime = 0;
	}

	// implementations of StatsCollector interface ....................

	/**
	 * Retrieve statistics from AppCbr which currently is - the number of sent
	 * packets, - the number of received packets, - the average delay of packets
	 */
	public ExtendedProperties getStats() {
		ExtendedProperties stats = new ExtendedProperties();
		stats.put(CBR_STAT_SENT, Long.toString(sentPackets));
		stats.put(CBR_STAT_RECV, Long.toString(receivedPackets));
		long avgDelay = receivedPackets == 0 ? 0 : delaySum / receivedPackets;
		stats.put(CBR_STAT_DELAY, Long.toString(avgDelay));
		float avgHopCount = receivedPackets == 0 ? 0 : hopCountSum
				/ receivedPackets;
		stats.put(CBR_STAT_HOPS, Float.toString(avgHopCount));
		return stats;
	}

	public String[] getStatParams() {
		return getStatParameters();
	}

	private static String[] getStatParameters() {
		return new String[] { CBR_STAT_SENT, CBR_STAT_RECV, CBR_STAT_DELAY };
	}

}
