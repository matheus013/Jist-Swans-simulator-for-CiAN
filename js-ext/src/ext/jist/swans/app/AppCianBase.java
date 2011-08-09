package ext.jist.swans.app;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.trans.TransInterface.SocketHandler;
import jist.swans.trans.TransInterface.TransTcpInterface;
import jist.swans.trans.TransInterface.TransUdpInterface;
import jist.swans.trans.TransTcp;
import jist.swans.trans.TransTcp.TcpMessage;
import jist.swans.trans.TransUdp;
import jist.swans.trans.TransUdp.UdpMessage;
import ext.util.stats.DucksCompositionStats;

public abstract class AppCianBase implements AppInterface, AppInterface.TcpApp,
		AppInterface.UdpApp, SocketHandler {

	// network entity.
	protected NetInterface netEntity;

	protected TransTcp transTCP;

	protected TransUdp transUDP;

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
				AppInterface.TcpApp.class, AppInterface.UdpApp.class });

		this.transTCP = new TransTcp();
		this.transUDP = new TransUdp();
	}

	public void send(Message msg, NetAddress addr) throws Exception {
		if (msg instanceof TcpMessage) {
			TcpMessage tcpMsg = (TcpMessage) msg;
			transTCP.send(tcpMsg, addr, tcpMsg.getDstPort(),
					tcpMsg.getSrcPort(), Constants.NET_PRIORITY_NORMAL);
		} else if (msg instanceof UdpMessage) {
			UdpMessage udpMsg = (UdpMessage) msg;
			transUDP.send(udpMsg.getPayload(), addr, udpMsg.getDstPort(),
					udpMsg.getSrcPort(), Constants.NET_PRIORITY_NORMAL);
		} else {
			throw new Exception("Invalid message type");
		}
	}

	public void receive(Message msg, NetAddress src, int srcPort)
			throws Continuation {
		// TODO Auto-generated method stub
		
	}

	public void run() {
		run(null);
	}

	public abstract void run(String[] args);

	/**
	 * Set network entity.
	 * 
	 * @param netEntity
	 *            network entity
	 */
	public void setNetEntity(NetInterface netEntity) {
		this.netEntity = netEntity;
		this.transTCP.setNetEntity(netEntity);
		this.transUDP.setNetEntity(netEntity);
	}

	public AppInterface getAppProxy() {
		return (AppInterface) self;
	}
	
	public AppInterface.TcpApp getTcpAppProxy() {
		return (AppInterface.TcpApp) self;
	}
	
	public AppInterface.UdpApp getUdpAppProxy() {
		return (AppInterface.UdpApp) self;
	}

	public TransTcpInterface getTcpEntity() throws Continuation {
		return transTCP.getProxy();
	}

	public TransUdpInterface getUdpEntity() throws Continuation {
		return transUDP.getProxy();
	}

}
