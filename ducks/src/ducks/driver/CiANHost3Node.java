package ducks.driver;

import org.apache.log4j.Logger;

public class CiANHost3Node extends CiANBaseNode
{
    public CiANHost3Node() {
        super();
        log = Logger.getLogger(CiANHost3Node.class.getName());
        args = new String[] { "Host 3", "standard", "-p", "../CiAN/src/examples/simpleSimu/properties3.ini", "-c",
                "../CiAN/src/examples/simpleSimu/simplehost3.hcfg", "-a",
                netEntity.getAddress().getIP().getHostAddress() };
    }
}
