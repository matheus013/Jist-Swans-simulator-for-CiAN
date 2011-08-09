package ducks.driver;


public class CianHost2Node extends CianBaseNode {

	public CianHost2Node() {
		super();
	}

	@Override
	protected String[] getCianArguments() {
		return new String[] { "Host 2", "standard", "-p", "properties2.ini",
				"-c", "simplehost2.hcfg" };
	}

}
