package ducks.driver;

public class CiANHost2Node extends CiANBaseNode
{

    public CiANHost2Node() {
        super();
    }

    @Override
    protected String[] getCiANArguments() {
        return new String[] { "Host 2", "standard", "-p", "properties2.ini", "-c", "simplehost2.hcfg" };
    }

}
