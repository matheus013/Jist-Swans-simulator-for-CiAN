package ducks.driver;

import org.apache.log4j.Logger;

public class CiANPlannerNode extends CiANBaseNode
{
    public CiANPlannerNode() {
        super();
        log = Logger.getLogger(CiANPlannerNode.class.getName());
        args = new String[] { "Host 1", "standard", "-p", "../CiAN/src/examples/simpleSimu/properties1.ini", "-c",
                "../CiAN/src/examples/simpleSimu/simplehost1.hcfg", "-w",
                "../CiAN/src/examples/simpleSimu/simple.cian", "-a", netEntity.getAddress().getIP().getHostAddress() };
    }
}
