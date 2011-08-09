package ducks.driver;

import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.misc.Mapper;
import jist.swans.net.NetInterface;

import org.apache.log4j.Logger;

import ext.jist.swans.app.AppGreetingBase;
import ext.jist.swans.app.AppWorkflowBase;
import ext.jist.swans.app.AppWorkflowInitiator;
import ext.util.stats.DucksCompositionStats;

public class WorkflowInitiatorNode extends GenericNode {
	private static Logger log = Logger.getLogger(WorkflowInitiatorNode.class
			.getName());

	public WorkflowInitiatorNode() {
		super();
	}

	protected void addApplication(Mapper protMap, Field field, Placement place)
			throws Exception {
		this.app = null;
		String appOpt = options.getStringProperty(SimParams.TRAFFIC_TYPE);

		if (appOpt.equals(SimParams.TRAFFIC_TYPE_SERVICE)) {
			int reqSize, reqRate, wTS, wTE, duration;
			reqSize = options.getIntProperty(SimParams.COMPOSITION_LENGTH);
			reqRate = options.getIntProperty(SimParams.TRAFFIC_SERV_RATE);
			wTS = options.getIntProperty(SimParams.TRAFFIC_WAITSTART);
			wTE = options.getIntProperty(SimParams.TRAFFIC_WAITEND);
			duration = globalConfig.getIntProperty(SimParams.SIM_DURATION);

			DucksCompositionStats compoStats = null;
			try {
				// get composition stats, and automatically create and register,
				// if necessary
				compoStats = (DucksCompositionStats) stats.getStaticCollector(
						DucksCompositionStats.class, true);
			} catch (Exception e) {
				e.printStackTrace();
			}

			AppWorkflowBase ac = new AppWorkflowInitiator(this.id, compoStats,
					reqSize, reqRate, wTS, wTE, duration);

			// currently we do not use transport layer (UDP) instead we hand
			// over from the network layer directly to application layer
			protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
			net.setProtocolHandler(Constants.NET_PROTOCOL_UDP,
					(NetInterface.NetHandler) ac.getNetProxy());

			protMap.testMapToNext(AppGreetingBase.NET_PROTOCOL_NUMBER);
			net.setProtocolHandler(AppGreetingBase.NET_PROTOCOL_NUMBER,
					(NetInterface.NetHandler) ac.getNetProxy());

			this.app = ac;
			this.appEntity = ac.getAppProxy();
			ac.setNetEntity(this.netEntity);
			this.appEntity.run();

			log.debug("  Added composition initiator application module");
		}
	}

}
