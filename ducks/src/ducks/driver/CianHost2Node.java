package ducks.driver;

import java.util.HashMap;

import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.misc.Mapper;
import jist.swans.net.NetInterface;

import org.apache.log4j.Logger;

import ext.jist.swans.app.AppGreetingBase;
import ext.jist.swans.app.AppWorkflowBase;
import ext.jist.swans.app.AppWorkflowProvider;
import ext.util.stats.DucksCompositionStats;

public class CianHost2Node extends GenericNode {
    private static Logger log = Logger.getLogger(CianHost2Node.class.getName());

    public CianHost2Node() {
        super();
    }

    protected void addApplication(Mapper protMap, Field field, Placement place) throws Exception {
        this.app = null;
        String appOpt = options.getStringProperty(SimParams.TRAFFIC_TYPE);

        if (appOpt.equals(SimParams.TRAFFIC_TYPE_SERVICE)) {
            HashMap<Character, Integer> repository = new HashMap<Character, Integer>();
            String mode = options.getStringProperty(SimParams.COMPOSITION_TYPE); 
            int reqSize = options.getIntProperty(SimParams.COMPOSITION_LENGTH);
            String compRestrict = options.getStringProperty(SimParams.COMPOSITION_RESTRICTION);
                       
            // init provider-specific service repository
            char service='A';
            int execTime=0;
            while (repository.size()<reqSize) {
                execTime = options.getIntProperty(SimParams.SERVICE_EXEC_TIME_PREFIX+"."+service+"."+this.id);
                repository.put(service, execTime);
//                System.out.println("Add to rep: id="+this.id+" service="+serviceString+" exec="+execTime);
                service++;
            }
                                                
            DucksCompositionStats compoStats = null;
            try {
                // get composition stats, and automatically create and register, if necessary
//                compoStats = (DucksCompositionStats) stats.getStaticCollector(DucksCompositionStats.class, true);
                // TODO: Christin, decide whether provider nodes should print out compo stats, too
                compoStats = new DucksCompositionStats();
            } catch (Exception e) {
                
            }    
            
            AppWorkflowBase ac = new AppWorkflowProvider(this.id, compoStats, mode, repository, compRestrict);
            
            protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
            net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, (NetInterface.NetHandler) ac.getNetProxy());

            protMap.testMapToNext(AppGreetingBase.NET_PROTOCOL_NUMBER);
            net.setProtocolHandler(AppGreetingBase.NET_PROTOCOL_NUMBER, (NetInterface.NetHandler) ac.getNetProxy());

            this.app = ac;
            this.appEntity = ac.getAppProxy();
            ac.setNetEntity(this.netEntity);
            this.appEntity.run();

            // stats.registerStaticCollector(as);
            log.debug("  Added composition provider application module");
        }
    }
}
