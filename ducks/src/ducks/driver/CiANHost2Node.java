package ducks.driver;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

public class CiANHost2Node extends CiANBaseNode
{
    private static Logger log = Logger.getLogger(CiANHost2Node.class.getName());

    public CiANHost2Node() {
        super();
        args = new String[] { "Host 2", "standard", "-p", "../CiAN/src/examples/simpleSimu/properties2.ini", "-c",
                "../CiAN/src/examples/simpleSimu/simplehost2.hcfg", "-a", "localhost" };
    }
}
