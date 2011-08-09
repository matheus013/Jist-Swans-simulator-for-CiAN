package ducks.driver;


public class CianHost3Node extends CianBaseNode {

	public CianHost3Node() {
		super();
	}

	@Override
	protected String[] getCianArguments() {
		return new String[] { "Host 3", "standard", "-p", "properties3.ini",
				"-c", "simplehost3.hcfg" };
	}
}
