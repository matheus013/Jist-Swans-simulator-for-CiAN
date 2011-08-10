package ducks.driver;

public class CiANHost3Node extends CiANBaseNode
{

    public CiANHost3Node() {
        super();
    }

    @Override
    protected String[] getCiANArguments() {
        return new String[] { "Host 3", "standard", "-p", "properties3.ini", "-c", "simplehost3.hcfg" };
    }
}
