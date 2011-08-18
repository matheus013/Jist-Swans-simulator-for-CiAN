package ducks.driver;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

public class CiANHost3Node extends CiANBaseNode
{
    private static Logger log = Logger.getLogger(CiANHost3Node.class.getName());

    public CiANHost3Node() {
        super();
    }

    @Override
    protected void runApplication() {
        String[] args = new String[] { "Host 3", "standard", "-p", "../CiAN/src/examples/simpleSimu/properties3.ini",
                "-c", "../CiAN/src/examples/simpleSimu/simplehost3.hcfg", "-a", "localhost" };
        this.appEntity.run(args);
    }
}
