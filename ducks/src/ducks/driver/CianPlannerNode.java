package ducks.driver;

import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.misc.Mapper;
import jist.swans.net.NetInterface;

import org.apache.log4j.Logger;

import ext.jist.swans.app.AppCianBase;
import ext.jist.swans.app.AppCianPlanner;
import ext.jist.swans.app.AppGreetingBase;
import ext.util.stats.DucksCompositionStats;

public class CianPlannerNode extends GenericNode {
	private static Logger log = Logger.getLogger(CianPlannerNode.class
			.getName());

	public CianPlannerNode() {
		super();
	}

	protected void addApplication(Mapper protMap, Field field, Placement place)
			throws Exception {
		this.app = null;
		String appOpt = options.getStringProperty(SimParams.TRAFFIC_TYPE);

		if (appOpt.equals(SimParams.TRAFFIC_TYPE_SERVICE)) {
			DucksCompositionStats compoStats = null;
			try {
				// get composition stats, and automatically create and register,
				// if necessary
				compoStats = (DucksCompositionStats) stats.getStaticCollector(
						DucksCompositionStats.class, true);
			} catch (Exception e) {
				e.printStackTrace();
			}

			AppCianBase ac = new AppCianPlanner(this.id, compoStats);

			protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
			net.setProtocolHandler(Constants.NET_PROTOCOL_UDP,
					ac.getUdpEntity());

			protMap.testMapToNext(Constants.NET_PROTOCOL_TCP);
			net.setProtocolHandler(Constants.NET_PROTOCOL_TCP,
					ac.getTcpEntity());

			this.app = ac;
			this.appEntity = ac.getAppProxy();
			ac.setNetEntity(this.netEntity);
			this.appEntity.run();

			log.debug("  Added composition initiator application module");
		}
	}

}
