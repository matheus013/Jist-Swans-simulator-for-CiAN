package ext.jist.swans.app;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.trans.TcpSocket;
import jist.swans.trans.TransInterface.TransTcpInterface;
import jist.swans.trans.TransInterface.TransUdpInterface;
import jist.swans.trans.TransTcp;
import jist.swans.trans.TransUdp;
import ext.util.stats.DucksCompositionStats;

public abstract class AppCianBase implements AppInterface,
		NetInterface.NetHandler {
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
	protected Object self;

	protected int nodeId;

	// composition stats accumulator
	protected DucksCompositionStats compositionStats;

	public AppCianBase(int nodeId, DucksCompositionStats compositionStats) {
		this.nodeId = nodeId;
		this.compositionStats = compositionStats;
		// init self reference
		this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class,
				NetInterface.NetHandler.class });
	}

	protected void send(Message msg) {
		netEntity.send(msg, NetAddress.ANY, NET_PROTOCOL_NUMBER,
				Constants.NET_PRIORITY_NORMAL, new Byte("0"));
	}

	public void run() {
		run(null);
	}

	public abstract void run(String[] args);
	
	public abstract void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl);

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

}
