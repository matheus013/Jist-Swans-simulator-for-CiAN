package ducks.driver;

import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.misc.Mapper;
import jist.swans.net.NetInterface;

import org.apache.log4j.Logger;

import ext.jist.swans.app.AppGreetingBase;
import ext.jist.swans.app.AppGreetingProvider;

public class ProviderNode extends GenericNode{
    private static Logger log = Logger.getLogger(ProviderNode.class.getName());
    
    public ProviderNode(){
        super();
    }
    
    protected void addApplication(Mapper protMap, Field field, Placement place) throws Exception {
        
        this.app = null;
        String appOpt = options.getStringProperty(SimParams.TRAFFIC_TYPE);
        
        if (appOpt.equals(SimParams.TRAFFIC_TYPE_SERVICE)) { 
            AppGreetingBase as = new AppGreetingProvider(this.id);
                        
            protMap.testMapToNext(Constants.NET_PROTOCOL_UDP);
            net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, (NetInterface.NetHandler) as.getNetProxy());
            
            protMap.testMapToNext(AppGreetingBase.NET_PROTOCOL_NUMBER);
            net.setProtocolHandler(AppGreetingBase.NET_PROTOCOL_NUMBER, (NetInterface.NetHandler) as.getNetProxy());
            
            this.app = as;
            this.appEntity = as.getAppProxy();
            as.setNetEntity(this.netEntity);
            this.appEntity.run();
            
//            stats.registerStaticCollector(as);
            log.debug("  Added service provider application module");
        } 
    }

}
