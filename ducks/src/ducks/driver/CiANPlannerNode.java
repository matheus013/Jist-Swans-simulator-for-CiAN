package ducks.driver;

public class CiANPlannerNode extends CiANBaseNode
{

    public CiANPlannerNode() {
        super();
    }

    @Override
    protected String[] getCiANArguments() {
        return new String[] { "Host 1", "planner", "-p", "properties1.ini", "-c", "simplehost1.hcfg", "-w",
                "simple.cian" };
    }

}
