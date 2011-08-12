package ext.jist.swans.app;

import java.net.InetAddress;

import jist.swans.Constants;
import jist.swans.misc.MessageBytes;
import jist.swans.net.NetAddress;
import jist.swans.trans.TcpServerSocket;
import jist.swans.trans.TcpSocket;
import jist.swans.trans.TransUdp.UdpMessage;

public class CiANAdapter
{
    public AppCiAN app;

    public CiANAdapter(AppCiAN app) {
        this.app = app;
    }

    /**
     * @return the InetAddress of this node in Swans
     */
    public InetAddress getInetAddress() {
        return app.getNetEntity().getAddress().getIP();
    }

    /**
     * Opens a new handler for UDP communications on this port
     * 
     * @param port
     */
    public void addUdpHandler(int port) {
        app.getUdpEntity().addSocketHandler(port, app);
        app.setMulticastPort(port);
    }

    /**
     * Removes a previously opened handler for UDP communications on this
     * port
     * 
     * @param port
     */
    public void removeUdpHandler(int port) {
        app.getUdpEntity().delSocketHandler(port);
        app.setMulticastPort(0);
    }

    /**
     * Sends a multicast packet to all hosts in range
     * 
     * @param packet
     */
    public void sendMulticastPacket(byte[] packet, int port) {
        app.getUdpEntity().send(new MessageBytes(packet), NetAddress.ANY, port, port, Constants.NET_PRIORITY_NORMAL);
    }

    /**
     * Blocks until a new UDP packet arrives and retrieves it.
     * CiAN should call this method to receive a UDP packet.
     * 
     * @return the payload of the UDP packet
     * @throws InterruptedException
     */
    public byte[] receiveUdpPacket() throws InterruptedException {
        byte[] packet = null;
        UdpMessage msg = app.getUdpMessageQueue().take();
        if (msg.getPayload() instanceof MessageBytes) {
            packet = ((MessageBytes) msg.getPayload()).getBytes();
        } else {
            packet = new byte[msg.getPayload().getSize()];
            msg.getPayload().getBytes(packet, 0);
        }

        return packet;
    }

    /**
     * @param ipAddress
     * @param remotePort
     * @return jist.swans.trans.TcpSocket connected to ipAddress:remotePort
     */
    public TcpSocket createTcpSocket(String ipAddress, int remotePort) {
        TcpSocket socket = new TcpSocket(ipAddress, remotePort);
        socket.setTcpEntity(app.getTcpEntity());
        socket._jistPostInit();

        return socket;
    }

    /**
     * @param port
     * @return jist.swans.trans.TcpServerSocket listening on port
     */
    public TcpServerSocket createTcpServerSocket(int port) {
        TcpServerSocket serverSocket = new TcpServerSocket(port);
        serverSocket.setTcpEntity(app.getTcpEntity());
        serverSocket._jistPostInit();

        return serverSocket;
    }
}
