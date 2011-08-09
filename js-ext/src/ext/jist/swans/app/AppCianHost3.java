package ext.jist.swans.app;

import java.util.HashMap;

import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import ext.util.stats.DucksCompositionStats;

public class AppCianHost3 extends AppCianBase {

	public AppCianHost3(int nodeId, DucksCompositionStats stats, String mode,
			HashMap<Character, Integer> repository, String compoRestrict) {
		super(nodeId, stats);

	}

	@Override
	public void run(String[] args) {
	}

	@Override
	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {

	}

}
