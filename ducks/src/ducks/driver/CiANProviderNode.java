package ducks.driver;

import java.util.HashMap;

import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.misc.Mapper;
import jist.swans.net.NetInterface;

import org.apache.log4j.Logger;

import ext.jist.swans.app.AppCiANBase;
import ext.jist.swans.app.AppCiANProvider;
import ext.util.stats.DucksCompositionStats;

public class CiANProviderNode extends GenericNode
{
    private static Logger log = Logger.getLogger(CiANProviderNode.class.getName());

    public CiANProviderNode() {
        super();
    }

    protected void addApplication(Mapper protMap, Field field, Placement place) throws Exception {
        this.app = null;
        String appOpt = options.getStringProperty(SimParams.TRAFFIC_TYPE);

        if (appOpt.equals(SimParams.TRAFFIC_TYPE_SERVICE)) {
            HashMap<Character, Integer> repository = new HashMap<Character, Integer>();
            String mode = options.getStringProperty(SimParams.COMPOSITION_TYPE);
            int reqSize = options.getIntProperty(SimParams.COMPOSITION_LENGTH);

            // init provider-specific service repository
            char service = 'A';
            int execTime = 0;
            while (repository.size() < reqSize) {
                execTime = options.getIntProperty(SimParams.SERVICE_EXEC_TIME_PREFIX + "." + service + "." + this.id);
                repository.put(service, execTime);
                // System.out.println("Add to rep: id="+this.id+" service="+serviceString+" exec="+execTime);
                service++;
            }

            DucksCompositionStats compoStats = null;
            try {
                // get composition stats, and automatically create and register,
                // if necessary
                // compoStats = (DucksCompositionStats)
                // stats.getStaticCollector(DucksCompositionStats.class, true);
                // TODO: Christin, decide whether provider nodes should print
                // out compo stats, too
                compoStats = new DucksCompositionStats();
            } catch (Exception e) {

            }

            AppCiANBase ac = new AppCiANProvider(this.id, compoStats, mode, repository);

            // Currently we do not use transport layer instead we hand
            // over from the network layer directly to application layer
            protMap.testMapToNext(AppCiANBase.NET_PROTOCOL_NUMBER);
            net.setProtocolHandler(AppCiANBase.NET_PROTOCOL_NUMBER, (NetInterface.NetHandler) ac.getNetProxy());

            this.app = ac;
            this.appEntity = ac.getAppProxy();
            ac.setNetEntity(this.netEntity);
            this.appEntity.run();

            // stats.registerStaticCollector(as);
            log.debug("  Added composition provider application module");
        }
    }
}
