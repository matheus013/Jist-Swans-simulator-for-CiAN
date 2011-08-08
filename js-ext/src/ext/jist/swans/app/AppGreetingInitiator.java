package ext.jist.swans.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import ext.util.ExtendedProperties;

public class AppGreetingInitiator extends AppGreetingBase implements AppInterface {
    private static final String SERV_STAT_REQ           = "ducks.app.serv.total.requests";
    private static final String SERV_STAT_SUC_SEARCH    = "ducks.app.serv.success.search";
    private static final String SERV_STAT_SUC_INVOKE    = "ducks.app.serv.success.invoke";
    
    private int msgId;
    private ServiceStats stats;
    private int reqRate;
    private int invokeDelay;
    private int waitTimeStart;
    private int waitTimeEnd;
    private int duration;

    public AppGreetingInitiator(int nodeId, int reqRate, int invokeDelay, int waitTimeStart, int waitTimeEnd, int duration) {
        super(nodeId);
        this.stats = new ServiceStats();
        this.msgId = 0;
        this.reqRate = reqRate;
        this.invokeDelay = invokeDelay;
        this.waitTimeStart = waitTimeStart;
        this.waitTimeEnd = waitTimeEnd;
        this.duration = duration;
    }

    @Override
    public String[] getStatParams() {
        return new String[] { SERV_STAT_REQ, SERV_STAT_SUC_SEARCH, SERV_STAT_SUC_INVOKE };
    }

    @Override
    public ExtendedProperties getStats() {
        ExtendedProperties s = new ExtendedProperties();
        s.put(SERV_STAT_REQ, Double.toString(stats.getNumReq()));
        s.put(SERV_STAT_SUC_SEARCH, Double.toString(stats.getNumSearchSuccess()));
        s.put(SERV_STAT_SUC_INVOKE, Double.toString(stats.getNumInvokeSuccess()));
        
        return s;
    }

    @Override
    public void receive(Message msg, final NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority,
            byte ttl) {
        if(!msg.getClass().equals(GreetingMessage.class)) return;
        
        final GreetingMessage gmsg = (GreetingMessage)msg;
        if(gmsg.isResponse()){    
            if(gmsg.getAttempt()==1){
                if(stats.isFirstProvider(gmsg.getMsgId(), src)){
                    stats.markSearchSuccess(gmsg.getMsgId());
                    JistAPI.runAt(new Runnable() {
                        public void run() {
                            netEntity.send(new GreetingMessage(GreetingMessage.REQUEST_TEXT, gmsg.getMsgId(), 2), src, NET_PROTOCOL_NUMBER, Constants.NET_PRIORITY_NORMAL, (byte) 1);
                        }
                    }, JistAPI.getTime()+invokeDelay*Constants.SECOND);
                }
            }else if(gmsg.getAttempt()==2 && stats.isBoundProvider(gmsg.getMsgId(), src)){
                stats.markInvokeSuccess(gmsg.getMsgId());
            }
        }    
    }

    @Override
    public void run(String[] args) {
        // startup delay
        if(JistAPI.getTime()==0) {
            JistAPI.sleep((this.waitTimeStart) * Constants.SECOND);
        }
        
        msgId++;
        Message msg = new GreetingMessage(GreetingMessage.REQUEST_TEXT, msgId, 1);
        netEntity.send(msg, NetAddress.ANY, NET_PROTOCOL_NUMBER, Constants.NET_PRIORITY_NORMAL, (byte) 1);
        stats.addNewEntry(msgId);
                
        // check if the next messages sequence will be finished before 
        // the cool down phase at the simulation end begins. If not, do
        // not reschedule this method again.
        if ( JistAPI.getTime() <= (this.duration - this.waitTimeEnd - this.reqRate) * Constants.SECOND ) {
            JistAPI.sleep(this.reqRate*Constants.SECOND);
            ((AppInterface) self).run();      
        }  
    }   
}

class ServiceStats {
    private List<StatsEntry> results;
    private double numReq;
    private double numSearchSuccess;
    private double numInvokeSuccess;
    
    public ServiceStats(){
        this.results = new ArrayList<StatsEntry>();
        this.numReq = 0;
        this.numSearchSuccess = 0;
        this.numInvokeSuccess = 0;
    }

    public synchronized void addNewEntry(int msgId) {
        StatsEntry e = findEntry(msgId);
        if (e == null){
            results.add(new StatsEntry(msgId));
            numReq++;
        }
    }

    public synchronized boolean isFirstProvider(int msgId, NetAddress provider) {
        StatsEntry e = findEntry(msgId);
        if (e != null && e.getProvider() == null) {
            e.setProvider(provider);
            return true;
        } else
            return false;
    }

    public synchronized void markSearchSuccess(int msgId) {
        StatsEntry e = findEntry(msgId);
        if (e != null){
            e.setSearchSuccess(true);
            numSearchSuccess++;
        }
    }

    public synchronized boolean isBoundProvider(int msgId, NetAddress provider) {
        StatsEntry e = findEntry(msgId);
        if (e != null && e.getProvider().equals(provider)) return true;
        else return false;
    }
    
    public synchronized void markInvokeSuccess(int msgId) {
        StatsEntry e = findEntry(msgId);
        if (e != null){
            if(e.isInvokeSuccess()) System.out.println("entry has already invoke success marked");
            e.setInvokeSuccess(true);
            numInvokeSuccess++;
        }
    }
    
    private StatsEntry findEntry(int msgId) {
        StatsEntry e = null;
        Iterator<StatsEntry> it = results.iterator();
        while (it.hasNext()) {
            e = it.next();
            if (e.getMsgId() == msgId)
                return e;
        }

        return null;
    }

    public double getNumReq() {
        return numReq;
    }

    public double getNumSearchSuccess() {
        return numSearchSuccess;
    }

    public double getNumInvokeSuccess() {
        return numInvokeSuccess;
    }
}

class StatsEntry {
    private int msgId;
    private NetAddress provider;
    private boolean searchSucess;
    private boolean invokeSuccess;

    public StatsEntry(int msgId) {
        this.msgId = msgId;
        this.provider = null;
        this.searchSucess = false;
        this.invokeSuccess = false;
    }

    public int getMsgId() {
        return msgId;
    }

    public NetAddress getProvider() {
        return provider;
    }

    public void setProvider(NetAddress provider) {
        this.provider = provider;
    }

    public boolean isSearchSuccess() {
        return searchSucess;
    }

    public void setSearchSuccess(boolean searchSucess) {
        this.searchSucess = searchSucess;
    }

    public boolean isInvokeSuccess() {
        return invokeSuccess;
    }

    public void setInvokeSuccess(boolean invokeSucess) {
        this.invokeSuccess = invokeSucess;
    }
}

