package ext.jist.swans.app;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.app.AppInterface;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.trans.TransInterface.SocketHandler;
import jist.swans.trans.TransInterface.TransTcpInterface;
import jist.swans.trans.TransInterface.TransUdpInterface;
import jist.swans.trans.TransTcp;
import jist.swans.trans.TransUdp;
import jist.swans.trans.TransUdp.UdpMessage;
import ext.util.stats.DucksCompositionStats;

/**
 * Application for every node running CiAN
 * 
 * @author Jordan Alliot
 */
public class AppCiAN implements AppInterface, AppInterface.TcpApp, AppInterface.UdpApp, SocketHandler
{
    // network entity.
    protected NetInterface              netEntity;

    protected TransTcp                  transTcp;

    protected TransUdp                  transUdp;

    // self-referencing proxy entity.
    protected Object                    self;

    protected int                       nodeId;

    // composition stats accumulator
    protected DucksCompositionStats     compositionStats;

    protected String[]                  args;

    protected CiANAdapter               adapter;

    protected BlockingQueue<UdpMessage> udpMessageQueue;
    protected int                       multicastPort;

    public AppCiAN(int nodeId, DucksCompositionStats compositionStats, String[] args) {
        this.nodeId = nodeId;
        this.compositionStats = compositionStats;
        this.args = args;
        // init self reference
        this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class, AppInterface.TcpApp.class,
                AppInterface.UdpApp.class });

        this.transTcp = new TransTcp();
        this.transUdp = new TransUdp();

        this.udpMessageQueue = new LinkedBlockingQueue<UdpMessage>();
        this.multicastPort = 0;
    }

    public int getNodeId() {
        return nodeId;
    }

    /**
     * Handler for new UDP packets.
     * 
     * @see CiANAdapter#receiveUdpPacket()
     */
    public void receive(Message msg, NetAddress src, int srcPort) throws Continuation {
        UdpMessage uMsg;
        if (msg instanceof UdpMessage) {
            uMsg = (UdpMessage) msg;
        } else {
            // We can safely say it is a UDP message coming from the correct
            // port since we only use UDP for beaconning (multicast)
            uMsg = new UdpMessage(srcPort, multicastPort, msg);
        }
        udpMessageQueue.add(uMsg);
    }

    public void run() {
        run(this.args);
    }

    public void run(String[] args) {
        compositionStats.incrementNumReq();

        // Starting a new CiAN application isolated from the others
        adapter = new CiANAdapter(this);
        new CiANThread(adapter, args).start();
    }

    public NetInterface getNetEntity() {
        return netEntity;
    }

    /**
     * Set network entity.
     * 
     * @param netEntity
     *            network entity
     */
    public void setNetEntity(NetInterface netEntity) {
        this.netEntity = netEntity;
        this.transTcp.setNetEntity(netEntity);
        this.transUdp.setNetEntity(netEntity);
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
        return transTcp.getProxy();
    }

    public TransUdpInterface getUdpEntity() throws Continuation {
        return transUdp.getProxy();
    }

    public BlockingQueue<UdpMessage> getUdpMessageQueue() {
        return udpMessageQueue;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    /**
     * Thread running CiAN isolated thanks to a new ClassLoader
     * 
     * @author Jordan Alliot
     */
    class CiANThread extends Thread
    {
        private String[]    args;
        private CiANAdapter adapter;

        public CiANThread(CiANAdapter adapter, String[] args) {
            super("CiANThread for node " + getNodeId());
            this.args = args;
            this.adapter = adapter;
        }

        @Override
        public void run() {
            // Unfortunately we have to do this little hack in order for every
            // node to be isolated from the others.
            // IMPORTANT: it is vital that CiAN.jar is NOT in the classpath!
            try {
                // We need a new ClassLoader for each node we are creating
                // Working directory is $(basedir)/ducks so we need to go up to
                // $(basedir)
                File cianJar = new File("../CiAN/CiAN.jar");
                if (!cianJar.isFile() || !cianJar.canRead()) {
                    throw new Exception("Cannot find CiAN.jar in " + cianJar.getCanonicalPath());
                }
                ClassLoader loader = new URLClassLoader(new URL[] { cianJar.toURI().toURL() });

                // Loading CiAN class this way ensures us that each node is
                // isolated from each other
                Class<?> cianClass = loader.loadClass("system.CiAN");

                // Add a dependency to this node's application into CiAN
                cianClass.getDeclaredMethod("setExternalTool", Object.class).invoke(null, adapter);

                // Finally we can start CiAN
                cianClass.getDeclaredMethod("main", new Class[] { this.args.getClass() }).invoke(null,
                        new Object[] { this.args });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
