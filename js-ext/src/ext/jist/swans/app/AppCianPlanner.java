package ext.jist.swans.app;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.Constants;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.trans.TransInterface.SocketHandler;
import jist.swans.trans.TransInterface.TransTcpInterface;
import jist.swans.trans.TransInterface.TransUdpInterface;
import ext.util.stats.DucksCompositionStats;

public class AppCianPlanner extends AppCianBase {

	public AppCianPlanner(int nodeId, DucksCompositionStats stats) {
		super(nodeId, stats);
	}

	@Override
	public void run(String[] args) {
		compositionStats.incrementNumReq();
	}

}
