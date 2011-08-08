package ext.util.stats;

import ext.util.ExtendedProperties;

public class DucksRadioNoiseAdditiveStats implements StatsCollector {
    private static final String NOISE_ADD_STAT_SENSED       = "ducks.radio.noise.additive.app.disc.req.sensed";
    private static final String NOISE_ADD_STAT_LOST_WEAK    = "ducks.radio.noise.additive.app.disc.req.lost.weaksig";
    private static final String NOISE_ADD_STAT_LOST_COLL    = "ducks.radio.noise.additive.app.disc.req.lost.collision";
    private static final String NOISE_ADD_STAT_LOST_TRAN    = "ducks.radio.noise.additive.app.disc.req.lost.transmit";
    private static final String NOISE_ADD_STAT_LOST_SLEE    = "ducks.radio.noise.additive.app.disc.req.lost.sleep";

    private static int appDiscReqSensed;
    private static int appDiscReqLostWeak;
    private static int appDiscReqLostColl;
    private static int appDiscReqLostTran;
    private static int appDiscReqLostSlee;
    
    public DucksRadioNoiseAdditiveStats(){
        clear();
    }
    
    public void clear(){
        appDiscReqSensed = 0;
        appDiscReqLostWeak = 0;
        appDiscReqLostColl = 0;
        appDiscReqLostTran = 0;
        appDiscReqLostSlee = 0;      
    }
    
    public ExtendedProperties getStats() {
        ExtendedProperties stats = new ExtendedProperties();

        stats.put(NOISE_ADD_STAT_SENSED, Integer.toString(appDiscReqSensed));  
        stats.put(NOISE_ADD_STAT_LOST_WEAK, Integer.toString(appDiscReqLostWeak));  
        stats.put(NOISE_ADD_STAT_LOST_COLL, Integer.toString(appDiscReqLostColl));  
        stats.put(NOISE_ADD_STAT_LOST_TRAN, Integer.toString(appDiscReqLostTran));  
        stats.put(NOISE_ADD_STAT_LOST_SLEE, Integer.toString(appDiscReqLostSlee));  

        return stats;
    }

    public String[] getStatParams() {
        String[] stats = new String[5];
        stats[0] = "NOISE_ADD_STAT_SENSED";
        stats[1] = "NOISE_ADD_STAT_LOST_WEAK";
        stats[2] = "NOISE_ADD_STAT_LOST_COLL";
        stats[3] = "NOISE_ADD_STAT_LOST_TRAN";
        stats[4] = "NOISE_ADD_STAT_LOST_SLEE";
        return stats;
    }

    public static void incAppDiscReqSensed() {
        appDiscReqSensed = appDiscReqSensed+1;
    }

    public static void incAppDiscReqLostWeak() {
        appDiscReqLostWeak = appDiscReqLostWeak+1;
    }

    public static void incAppDiscReqLostColl() {
        appDiscReqLostColl = appDiscReqLostColl+1;
    }

    public static void incAppDiscReqLostTran() {
        appDiscReqLostTran = appDiscReqLostTran+1;
    }

    public static void incAppDiscReqLostSlee() {
        appDiscReqLostSlee = appDiscReqLostSlee+1;
    }
}
