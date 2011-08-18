package ducks.driver;

import org.apache.log4j.Logger;

public class CiANHost2Node extends CiANBaseNode
{
    public CiANHost2Node() {
        super();
        log = Logger.getLogger(CiANHost2Node.class.getName());
        args = new String[] { "Host 2", "standard", "-p", "../CiAN/src/examples/simpleSimu/properties2.ini", "-c",
                "../CiAN/src/examples/simpleSimu/simplehost2.hcfg", "-a",
                netEntity.getAddress().getIP().getHostAddress() };
    }
}
