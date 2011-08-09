package ducks.driver;

import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.misc.Mapper;

import org.apache.log4j.Logger;

import ext.jist.swans.app.AppCian;
import ext.util.stats.DucksCompositionStats;

public abstract class CianBaseNode extends GenericNode {

	protected static Logger log = Logger
			.getLogger(CianBaseNode.class.getName());

	public CianBaseNode() {
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

			AppCian ac = new AppCian(this.id, compoStats, getCianArguments());

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

	protected abstract String[] getCianArguments();

}
