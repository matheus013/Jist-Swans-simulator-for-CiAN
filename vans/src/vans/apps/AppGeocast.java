package vans.apps;

import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.field.Field.RadioData;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;

import org.apache.log4j.Logger;

import vans.net.NetAddressGeo;
import ext.util.ExtendedProperties;
import ext.util.Region;
import ext.util.StringUtils;
import ext.util.stats.StatsCollector;

/**
 * Geocast application
 * 
 * @author eschoch
 * 
 */
public class AppGeocast implements AppInterface, NetInterface.NetHandler,
		StatsCollector {

	private static Logger log = Logger.getLogger(AppGeocast.class.getName());

	/**
	 * Packets for geocast app
	 */
	public static class MessageGeocast implements Message {

		public static final int SIZE = 50;
		public static final byte TTL = 64;

		public int getSize() {
			return SIZE;
		}

		public void getBytes(byte[] b, int offset) {
			throw new RuntimeException("not implemented");
		}

		public int msgID = 1;
		public long sentTime = 0;
		public long recvTime = 0;
		public Location origin;
		public int radius = 0;

		public String toString() {
			return "id=" + msgID + " sentTime=" + sentTime + " recvTime="
					+ recvTime;
		}
	}

	// self-referencing proxy entity.
	private Object self;
	// entity reference to network entity.
	protected NetInterface netEntity;

	protected int nodeId;
	protected RadioData radio;
	protected int minRadius;
	protected int maxRadius;
	protected Location.Location2D[] srcArea;
	protected double msgPerMinute;
	protected int waitTimeStart;
	protected int waitTimeEnd;
	protected int duration;

	public AppGeocast(int nodeId, RadioData radio, int minRadius,
			int maxRadius, Location.Location2D[] srcArea,
			double messagesPerMinute, int waitTimeStart, int waitTimeEnd,
			int duration) {
		this.nodeId = nodeId;
		this.radio = radio;
		this.minRadius = minRadius;
		this.maxRadius = maxRadius;
		this.srcArea = srcArea;
		this.msgPerMinute = messagesPerMinute;
		this.waitTimeStart = waitTimeStart;
		this.waitTimeEnd = waitTimeEnd;
		this.duration = duration;

		sentMessagesCount = 0;
		recvMessagesCount = 0;

		this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class,
				NetInterface.NetHandler.class });

		log.debug("Created AppGeocast at node=" + nodeId + " minRadius="
				+ minRadius + " maxRadius=" + maxRadius + " msgPerMinute="
				+ msgPerMinute + " waitStart=" + waitTimeStart + " waitEnd="
				+ waitTimeEnd + " duration=" + duration);

		if (duration - waitTimeEnd < waitTimeStart) {
			log.warn("ERR: waitTimeStart (="
					+ waitTimeStart
					+ " and waitTimeEnd (="
					+ waitTimeEnd
					+ ") do not allow sending messages in duration of simulation (="
					+ duration + ")");
		}
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	// Implementations of AppInterface

	public void run() {
		run(null);
	}

	public void run(String[] args) {

		// if (this.nodeId != 94) return;

		// at the beginning, wait a little until messages can be sent
		if (JistAPI.getTime() == 0) {
			JistAPI.sleep(waitTimeStart * Constants.SECOND);
		}

		String[] nextArgs = null;

		if (args == null) {
			// calculate period, in which a message should be sent based on the
			// number of messages to send per minute (may be < 1 also)
			long period = Math.round((60 * Constants.SECOND)
					/ this.msgPerMinute);
			long waitUntilMessage = Math.round(Constants.random.nextDouble()
					* period);
			long waitAfterMessage = period - waitUntilMessage;

			log.debug("node=" + this.nodeId + " period="
					+ StringUtils.timeSeconds(period) + " waitUntil="
					+ StringUtils.timeSeconds(waitUntilMessage));

			// wait until message shall be sent
			JistAPI.sleep(waitUntilMessage);

			nextArgs = new String[] { Long.toString(waitAfterMessage) };

		} else {
			// send message.
			// It is important to come to this point without having a delay
			// before, because
			// sendMessage reads the current position of the node!
			sendMessage();

			// sleep again for the remainder of the period
			long waitAfterMessage = Long.parseLong(args[0]);
			JistAPI.sleep(waitAfterMessage);
		}

		// if it's still before the deadline for new messages, schedule next
		// round
		log.debug("next run? t="
				+ StringUtils.timeSeconds()
				+ " deadline="
				+ StringUtils.timeSeconds((duration - waitTimeEnd)
						* Constants.SECOND));
		if (JistAPI.getTime() <= (duration - waitTimeEnd) * Constants.SECOND) {
			log.debug("node=" + this.nodeId + " scheduling next run at t="
					+ StringUtils.timeSeconds());
			((AppInterface) self).run(nextArgs);
		}
	}

	private void sendMessage() {

		// determine a destination
		Location.Location2D localPos = (Location.Location2D) radio.getLoc();
		float radius = Constants.random.nextInt(maxRadius - minRadius)
				+ minRadius;
		Region dest = new Region.Circle(localPos, radius);

		MessageGeocast msg = new MessageGeocast();
		msg.msgID = ++sentMessagesCount;
		msg.sentTime = JistAPI.getTime();
		msg.origin = localPos;
		msg.radius = (int) radius;

		NetAddress addr = new NetAddressGeo(dest, NetAddress.NULL);
		netEntity.send(msg, addr, Constants.NET_PROTOCOL_UDP,
				Constants.NET_PRIORITY_NORMAL, MessageGeocast.TTL);

		log.info("send t=" + StringUtils.timeSeconds() + " node=" + nodeId
				+ " msg=" + msg.msgID + " at x=" + localPos.getX() + " y="
				+ localPos.getY() + " r=" + radius);

	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	// implementations of NetInterface

	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {

		recvMessagesCount++;

		int msgid = -1;
		if (msg instanceof MessageGeocast) {
			msgid = ((MessageGeocast) msg).msgID;
		}
		Location.Location2D localPos = (Location.Location2D) radio.getLoc();

		log.info("recv t=" + StringUtils.timeSeconds() + " node=" + nodeId
				+ " loc=" + localPos + " src=" + src + " msg=(" + msg + ")");

		boolean found = false;
		for (ReceiveStats rs : receivedMessages) {
			if (rs.src.equals(src) && rs.msgId == msgid) {
				float x = localPos.getX();
				if (x < rs.leftMostEncounter.getX()) {
					rs.leftMostEncounter = localPos;
				} else if (x > rs.rightMostEncounter.getX()) {
					rs.rightMostEncounter = localPos;
				}

				if (rs.ttl > ttl) {
					rs.ttl = ttl;
				}
				rs.receiveCount++;

				found = true;
			}
		}
		if (!found) {
			ReceiveStats rs = new ReceiveStats();
			rs.src = src;
			rs.msgId = msgid;
			rs.origin = ((MessageGeocast) msg).origin;
			rs.radius = ((MessageGeocast) msg).radius;
			rs.receiveCount = 1;
			rs.leftMostEncounter = localPos;
			rs.rightMostEncounter = localPos;
			rs.ttl = ttl;
			receivedMessages.add(rs);
		}

		// Consistency checks
		if (dst instanceof NetAddressGeo) {
			if (!((NetAddressGeo) dst).getRegion().contains(localPos)) {
				log.warn("ERR - got geocast outside of destination region");
			}
		} else {
			log.warn("ERR - got message with no geo-address");
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

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	// Statistics related

	public String[] getStatParams() {
		// TODO Auto-generated method stub
		return null;
	}

	public ExtendedProperties getStats() {

		ExtendedProperties results = new ExtendedProperties();

		results.put("app.geocast.sent", Integer.toString(sentMessagesCount));
		results.put("app.geocast.recv", Integer.toString(recvMessagesCount));

		if (statOutputType == 1) {
			int count = 0;
			for (ReceiveStats rs : receivedMessages) {
				log.debug(rs.src
						+ "/"
						+ rs.msgId
						+ " left="
						+ rs.leftMostEncounter.getX()
						+ " right="
						+ rs.rightMostEncounter.getX()
						+ " dist="
						+ (rs.rightMostEncounter.getX() - rs.leftMostEncounter
								.getX()) + " origin=" + rs.origin + " radius="
						+ rs.radius + " ttl=" + rs.ttl + " recvCount="
						+ rs.receiveCount);
				if (rs.leftMostEncounter.getX() < statParam
						&& rs.rightMostEncounter.getX() > statParam) {
					count++;
				}
			}

			results.put("app.geocast.recv.leftright", Integer.toString(count));
		}

		return results;
	}

	/**
	 * Type of statistics requested at the end type = 1 -> assumes stationary
	 * attacker at the highway, attacker at x = param getStats will return
	 * number of geocasts that travelled across x
	 * 
	 * @param type
	 * @param param
	 */
	public static void setStatisticOutput(int type, String param) {
		statOutputType = type;
		statParam = Integer.parseInt(param);
	}

	/**
	 * Data on sent and received messages
	 */
	private static int sentMessagesCount = 0;
	private static int recvMessagesCount = 0;
	private static Vector<ReceiveStats> receivedMessages = new Vector<ReceiveStats>();

	// statOutputType = 1 -> evaluate packets that passed the statParam point
	// from left to right or vice versa
	private static int statOutputType;
	private static int statParam;

	/**
	 * Storage class to keep information about received messages
	 * 
	 */
	public static class ReceiveStats {
		public NetAddress src;
		public int msgId;
		public Location origin;
		public int radius;
		public int receiveCount;
		public Location leftMostEncounter;
		public Location rightMostEncounter;
		public int ttl;
	}

}
