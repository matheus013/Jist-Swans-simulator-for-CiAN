package ducks.driver;

public class CiANPlannerNode extends CiANBaseNode
{

    public CiANPlannerNode() {
        super();
    }

    @Override
    public String[] getCiANArguments() {
        return new String[] { "Host 1", "planner", "-p", "../CiAN/src/examples/simpleSimu/properties1.ini", "-c",
                "../CiAN/src/examples/simpleSimu/simplehost1.hcfg", "-w",
                "../CiAN/src/examples/simpleSimu/simple.cian", "-a", netEntity.getAddress().getIP().getHostAddress() };
    }

}
