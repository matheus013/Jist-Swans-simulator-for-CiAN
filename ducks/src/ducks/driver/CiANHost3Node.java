package ducks.driver;

public class CiANHost3Node extends CiANBaseNode
{

    public CiANHost3Node() {
        super();
    }

    @Override
    public String[] getCiANArguments() {
        return new String[] { "Host 3", "standard", "-p", "../CiAN/src/examples/simpleSimu/properties3.ini", "-c",
                "../CiAN/src/examples/simpleSimu/simplehost3.hcfg", "-a",
                netEntity.getAddress().getIP().getHostAddress() };
    }
}
