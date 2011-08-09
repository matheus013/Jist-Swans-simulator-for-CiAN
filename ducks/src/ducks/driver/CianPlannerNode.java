package ducks.driver;

public class CianPlannerNode extends CianBaseNode
{

    public CianPlannerNode() {
        super();
    }

    @Override
    protected String[] getCianArguments() {
        return new String[] { "Host 1", "planner", "-p", "properties1.ini", "-c", "simplehost1.hcfg", "-w",
                "simple.cian" };
    }

}
