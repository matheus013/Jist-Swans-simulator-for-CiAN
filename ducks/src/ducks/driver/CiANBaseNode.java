package ducks.driver;

import jist.swans.Constants;
import jist.swans.app.AppJava;
import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.misc.Mapper;
import jist.swans.trans.TransTcp;
import jist.swans.trans.TransUdp;

import org.apache.log4j.Logger;

public abstract class CiANBaseNode extends GenericNode
{
    protected static Logger log;

    protected TransTcp      tcp;
    protected TransUdp      udp;

    protected String[]      args;

    public CiANBaseNode() {
        super();
    }

    protected void addApplication(Mapper protMap, Field field, Placement place) throws Exception {
        this.app = null;

        tcp = new TransTcp();
        udp = new TransUdp();

        protMap.testMapToNext(Constants.NET_PROTOCOL_TCP);
        net.setProtocolHandler(Constants.NET_PROTOCOL_TCP, tcp.getProxy());
        tcp.setNetEntity(net.getProxy());

        protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
        net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp.getProxy());
        udp.setNetEntity(net.getProxy());

        AppJava app = new AppJava("system.CiAN");

        app.setTcpEntity(tcp.getProxy());
        app.setUdpEntity(udp.getProxy());

        this.app = app;
        this.appEntity = app.getProxy();
        this.appEntity.run(args);

        log.debug("  Added composition initiator application module");
    }

}
