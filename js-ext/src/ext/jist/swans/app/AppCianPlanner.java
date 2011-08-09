package ext.jist.swans.app;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ext.util.stats.DucksCompositionStats;
import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.trans.TransInterface.TransTcpInterface;
import jist.swans.trans.TransInterface.TransUdpInterface;

public class AppCianPlanner extends AppCianBase {

	private int msgId;
	private int reqSize;
	private int waitTimeStart;
	private int waitTimeEnd;
	private int duration;

	public AppCianPlanner(int nodeId, DucksCompositionStats stats, int reqSize,
			int reqRate, int waitTimeStart, int waitTimeEnd, int duration) {
		super(nodeId, stats);
		this.msgId = 1;
		this.reqSize = reqSize;
		this.waitTimeStart = waitTimeStart;
		this.waitTimeEnd = waitTimeEnd;
		this.duration = duration;
	}

	@Override
	public void run(String[] args) {
		// startup delay
		if (JistAPI.getTime() == 0) {
			JistAPI.sleep((this.waitTimeStart) * Constants.SECOND);
		}

		compositionStats.incrementNumReq();
	}

	@Override
	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {

	}

}
