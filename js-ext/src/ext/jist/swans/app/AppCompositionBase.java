package ext.jist.swans.app;

import jist.runtime.JistAPI;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.misc.Sizeof;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import ext.util.stats.DucksCompositionStats;

public abstract class AppCompositionBase implements AppInterface,
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

	public AppCompositionBase(int nodeId, DucksCompositionStats compositionStats) {
		this.nodeId = nodeId;
		this.compositionStats = compositionStats;
		// init self reference
		this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class,
				NetInterface.NetHandler.class });
	}

	public void run() {
		run(null);
	}

	public abstract void run(String[] args);

	public abstract void receive(Message msg, NetAddress src,
			MacAddress lastHop, byte macId, NetAddress dst, byte priority,
			byte ttl);

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

abstract class ServiceMessage implements Message {
	protected String id;

	public ServiceMessage(int nodeId, int msgId) {
		this.id = (new Integer(nodeId)).toString() + "-"
				+ (new Integer(msgId)).toString();
	}

	public ServiceMessage(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public abstract void getBytes(byte[] msg, int offset);

	public abstract int getSize();
}

class DiscoveryRequest extends ServiceMessage {
	private char serviceName;
	private String compositionRequestId;

	public DiscoveryRequest(int nodeId, int msgId, char serviceName) {
		super(nodeId, msgId);
		this.serviceName = serviceName;
		this.compositionRequestId = this.getId();
	}

	public DiscoveryRequest(int nodeId, int msgId, char serviceName,
			String compositionRequestId) {
		super(nodeId, msgId);
		this.serviceName = serviceName;
		this.compositionRequestId = compositionRequestId;
	}

	public char getServiceName() {
		return serviceName;
	}

	public String getCompositionRequestId() {
		return compositionRequestId;
	}

	public void getBytes(byte[] msg, int offset) {
		throw new RuntimeException("not implemented");
	}

	public int getSize() {
		return Sizeof.inst(id.toCharArray()) + Sizeof.inst(serviceName);
	}
}

class DiscoveryResponse extends ServiceMessage {
	private String service;

	public DiscoveryResponse(String id, String service) {
		super(id);
		this.service = service;
	}

	public String getService() {
		return service;
	}

	public void getBytes(byte[] msg, int offset) {
		throw new RuntimeException("not implemented");
	}

	public int getSize() {
		return Sizeof.inst(id.toCharArray()) + Sizeof.inst(service);
	}
}

class CompositionMessage extends ServiceMessage {
	public final static char SYMBOL_DESTINATION = '\r';
	public final static String STRING_DESTINATION = "dst";
	private char[] services;
	private int[] providers;
	private int[] input;
	private int lastModifiedBy;

	public CompositionMessage(String id, char[] services, int[] providers,
			int[] input, int lastModifiedBy) {
		super(id);
		this.services = services;
		this.providers = providers;
		this.input = input;
		this.lastModifiedBy = lastModifiedBy;
	}

	public int[] getProvidersCopy() {
		int[] copy = new int[providers.length];
		System.arraycopy(providers, 0, copy, 0, providers.length);
		return copy;
	}

	public char[] getServicesCopy() {
		char[] copy = new char[services.length];
		System.arraycopy(services, 0, copy, 0, services.length);
		return copy;
	}

	public int[] getInputCopy() {
		int[] copy = new int[input.length];
		System.arraycopy(input, 0, copy, 0, input.length);
		return copy;
	}

	public int getLastModifiedBy() {
		return lastModifiedBy;
	}

	public int getIndexNextEmptyProvider() {
		for (int i = 0; i < providers.length; i++) {
			if (providers[i] == -1)
				return i;
		}
		return -1;
	}

	public int getIndexNextInput() {
		for (int i = 0; i < input.length; i++) {
			if (input[i] == -1)
				return i - 1;
		}
		return -1;
	}

	public String getServiceAt(int index) {
		if (index == services.length - 1)
			return STRING_DESTINATION;
		try {
			char s = services[index];
			return Character.toString(s);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("index = " + index);
			throw e;
		}
	}

	public int getIndexLastService() {
		return services.length - 2;
	}

	public int getDestinationId() {
		return providers[providers.length - 1];
	}

	public int getDestinationInput() {
		return input[providers.length - 1];
	}

	public void getBytes(byte[] msg, int offset) {
		throw new RuntimeException("not implemented");
	}

	public int getSize() {
		return Sizeof.inst(id.toCharArray()) + Sizeof.inst(services)
				+ Sizeof.inst(providers) + Sizeof.inst(input);
	}
}
