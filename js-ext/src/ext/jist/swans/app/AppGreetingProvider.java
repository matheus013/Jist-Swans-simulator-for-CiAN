package ext.jist.swans.app;

import ext.util.ExtendedProperties;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;

public class AppGreetingProvider extends AppGreetingBase implements
		AppInterface {

	public AppGreetingProvider(int nodeId) {
		super(nodeId);
	}

	@Override
	public String[] getStatParams() {
		return null;
	}

	@Override
	public ExtendedProperties getStats() {
		return null;
	}

	@Override
	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {
		if (!msg.getClass().equals(GreetingMessage.class))
			return;

		GreetingMessage gmsg = (GreetingMessage) msg;
		if (gmsg.isRequest()) {
			Message reply = new GreetingMessage(GreetingMessage.RESPONSE_TEXT,
					gmsg.getMsgId(), gmsg.getAttempt());
			netEntity.send(reply, src, NET_PROTOCOL_NUMBER,
					Constants.NET_PRIORITY_NORMAL, (byte) 1);
		}
	}

	@Override
	public void run(String[] args) {
	}

}
