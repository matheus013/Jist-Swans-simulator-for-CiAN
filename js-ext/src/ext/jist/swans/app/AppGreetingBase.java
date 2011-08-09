package ext.jist.swans.app;

import jist.runtime.JistAPI;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import ext.util.ExtendedProperties;
import ext.util.stats.StatsCollector;

public abstract class AppGreetingBase implements AppInterface, NetInterface.NetHandler, StatsCollector
{
    /**
     * Defines the IP protocol number since we directly use the network layer,
     * surpassing e.g. transport layer (UDP,TCP) Note: this protocol number
     * should be defined in jist.swans.Constants but is done here for modularity
     * reasons
     */
    public static final short NET_PROTOCOL_NUMBER = 524;

    // network entity.
    protected NetInterface    netEntity;

    // self-referencing proxy entity.
    protected Object          self;

    protected int             nodeId;

    public AppGreetingBase(int nodeId) {
        this.nodeId = nodeId;
        // init self reference
        this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class, NetInterface.NetHandler.class });
    }

    public void run() {
        run(null);
    }

    public abstract void run(String[] args);

    public abstract void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst,
            byte priority, byte ttl);

    public abstract String[] getStatParams();

    public abstract ExtendedProperties getStats();

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

class GreetingMessage implements Message
{
    public static final String REQUEST_TEXT  = "Hello?";
    public static final String RESPONSE_TEXT = "Yes";
    private String             msg           = null;
    private int                msgId         = 0;
    private int                attempt       = 0;

    public GreetingMessage(String msg, int msgId, int attempt) {
        this.msg = msg;
        this.msgId = msgId;
        this.attempt = attempt;
    }

    public int getAttempt() {
        return attempt;
    }

    public int getMsgId() {
        return msgId;
    }

    public boolean isRequest() {
        return msg.equals(REQUEST_TEXT);
    }

    public boolean isResponse() {
        return msg.equals(RESPONSE_TEXT);
    }

    public void getBytes(byte[] msg, int offset) {
        throw new RuntimeException("not implemented");
    }

    public int getSize() {
        return 0;
    }

}
