package ducks.driver;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

public class CiANPlannerNode extends CiANBaseNode
{
    private static Logger log = Logger.getLogger(CiANPlannerNode.class.getName());

    public CiANPlannerNode() {
        super();
        args = new String[] { "Host 1", "planner", "-p", "../CiAN/src/examples/simpleSimu/properties1.ini", "-c",
                "../CiAN/src/examples/simpleSimu/simplehost1.hcfg", "-w",
                "../CiAN/src/examples/simpleSimu/simple.cian", "-a", "localhost" };
    }
}
