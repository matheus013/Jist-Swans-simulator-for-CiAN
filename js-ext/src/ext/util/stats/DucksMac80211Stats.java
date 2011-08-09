package ext.util.stats;

import ext.util.ExtendedProperties;

public class DucksMac80211Stats implements StatsCollector
{
    private static final String COUNT_APP_SEND      = "ducks.mac.app.send";
    // private static final String COUNT_APP_DISC_REQ_SEND =
    // "ducks.mac.app.disc.req.send";
    // private static final String COUNT_APP_DISC_RSP_SEND =
    // "ducks.mac.app.disc.rsp.send";
    // private static final String COUNT_APP_COMP_MSG_SEND =
    // "ducks.mac.app.comp.msg.send";

    private static final String COUNT_APP_WF_SEND   = "ducks.mac.app.wf.send";
    private static final String COUNT_APP_AD_SEND   = "ducks.mac.app.ad.send";
    private static final String COUNT_APP_TOK_SEND  = "ducks.mac.app.tok.send";

    private static final String COUNT_RTE_SEND      = "ducks.mac.rte.send";
    private static final String COUNT_RTE_RREQ_SEND = "ducks.mac.rte.rreq.send";
    private static final String COUNT_RTE_RREP_SEND = "ducks.mac.rte.rrep.send";

    // private static final String COUNT_APP_RECV = "ducks.mac.app.recv";
    // private static final String COUNT_APP_DISC_REQ_RECV =
    // "ducks.mac.app.disc.req.recv";
    // private static final String COUNT_APP_DISC_RSP_RECV =
    // "ducks.mac.app.disc.rsp.recv";
    // private static final String COUNT_APP_COMP_MSG_RECV =
    // "ducks.mac.app.comp.msg.recv";
    // private static final String COUNT_RTE_RECV = "ducks.mac.rte.recv";
    // private static final String COUNT_RTE_RREQ_RECV =
    // "ducks.mac.rte.rreq.recv";
    // private static final String COUNT_RTE_RREP_RECV =
    // "ducks.mac.rte.rrep.recv";

    // private static int appDiscReqSend;
    // private static int appDiscRspSend;
    // private static int appCompMsgSend;
    private static int          aodvRreqSend;
    private static int          aodvRrspSend;

    private static int          appWfReqSend;
    private static int          appServiceAdSend;
    private static int          appTokenSend;

    // private static int appDiscReqRecv;
    // private static int appDiscRspRecv;
    // private static int appCompMsgRecv;
    // private static int aodvRreqRecv;
    // private static int aodvRrspRecv;

    public DucksMac80211Stats() {
        clear();
    }

    public void clear() {
        // appDiscReqSend = 0;
        // appDiscRspSend = 0;
        // appCompMsgSend = 0;
        aodvRreqSend = 0;
        aodvRrspSend = 0;

        appWfReqSend = 0;
        appServiceAdSend = 0;
        appTokenSend = 0;

        // appDiscReqRecv = 0;
        // appDiscRspRecv = 0;
        // appCompMsgRecv = 0;
        // aodvRreqRecv = 0;
        // aodvRrspRecv = 0;
    }

    public String[] getStatParams() {
        String[] stats = new String[10];
        // stats[0] = "COUNT_APP_DISC_REQ_SEND";
        // stats[1] = "COUNT_APP_DISC_RSP_SEND";
        // stats[2] = "COUNT_APP_COMP_MSG_SEND";

        stats[0] = "COUNT_APP_WF_SEND";
        stats[1] = "COUNT_APP_AD_SEND";
        stats[2] = "COUNT_APP_TOK_SEND";

        stats[3] = "COUNT_RTE_RREQ_SEND";
        stats[4] = "COUNT_RTE_RREP_SEND";
        // stats[5] = "COUNT_APP_DISC_REQ_RECV";
        // stats[6] = "COUNT_APP_DISC_RSP_RECV";
        // stats[7] = "COUNT_APP_COMP_MSG_RECV";
        // stats[8] = "COUNT_RTE_RREQ_RECV";
        // stats[9] = "COUNT_RTE_RREP_RECV";
        return stats;
    }

    public ExtendedProperties getStats() {
        ExtendedProperties stats = new ExtendedProperties();

        // stats.put(COUNT_APP_SEND,
        // Integer.toString(appDiscReqSend+appDiscRspSend+appCompMsgSend));
        // stats.put(COUNT_APP_DISC_REQ_SEND, Integer.toString(appDiscReqSend));
        // stats.put(COUNT_APP_DISC_RSP_SEND, Integer.toString(appDiscRspSend));
        // stats.put(COUNT_APP_COMP_MSG_SEND, Integer.toString(appCompMsgSend));

        stats.put(COUNT_APP_SEND, Integer.toString(appWfReqSend + appServiceAdSend + appTokenSend));
        stats.put(COUNT_APP_WF_SEND, Integer.toString(appWfReqSend));
        stats.put(COUNT_APP_AD_SEND, Integer.toString(appServiceAdSend));
        stats.put(COUNT_APP_TOK_SEND, Integer.toString(appTokenSend));

        stats.put(COUNT_RTE_SEND, Integer.toString(aodvRreqSend + aodvRrspSend));
        stats.put(COUNT_RTE_RREQ_SEND, Integer.toString(aodvRreqSend));
        stats.put(COUNT_RTE_RREP_SEND, Integer.toString(aodvRrspSend));

        // stats.put(COUNT_APP_RECV,
        // Integer.toString(appDiscReqRecv+appDiscRspRecv+appCompMsgRecv));
        // stats.put(COUNT_APP_DISC_REQ_RECV, Integer.toString(appDiscReqRecv));
        // stats.put(COUNT_APP_DISC_RSP_RECV, Integer.toString(appDiscRspRecv));
        // stats.put(COUNT_APP_COMP_MSG_RECV, Integer.toString(appCompMsgRecv));
        //
        // stats.put(COUNT_RTE_RECV,
        // Integer.toString(aodvRreqRecv+aodvRrspRecv));
        // stats.put(COUNT_RTE_RREQ_RECV, Integer.toString(aodvRreqRecv));
        // stats.put(COUNT_RTE_RREP_RECV, Integer.toString(aodvRrspRecv));

        return stats;
    }

    // public void incrementAppDiscReqSend() {
    // appDiscReqSend = appDiscReqSend +1;
    // }
    //
    // public void incrementAppDiscRspSend() {
    // appDiscRspSend = appDiscRspSend +1;
    // }
    //
    // public void incrementAppCompMsgSend() {
    // appCompMsgSend = appCompMsgSend +1;
    // }

    public void incrementAodvRreqSend() {
        aodvRreqSend = aodvRreqSend + 1;
    }

    public void incrementAodvRrspSend() {
        aodvRrspSend = aodvRrspSend + 1;
    }

    // public void incrementAppDiscReqRecv() {
    // appDiscReqRecv = appDiscReqRecv +1;
    // }
    //
    // public void incrementAppDiscRspRecv() {
    // appDiscRspRecv = appDiscRspRecv +1;
    // }
    //
    // public void incrementAppCompMsgRecv() {
    // appCompMsgRecv = appCompMsgRecv +1;
    // }
    //
    // public void incrementAodvRreqRecv() {
    // aodvRreqRecv = aodvRreqRecv +1;
    // }
    //
    // public void incrementAodvRrspRecv() {
    // aodvRrspRecv = aodvRrspRecv +1;
    // }

    /* Workflow stats */
    public void incrementAppWfReqSend() {
        appWfReqSend = appWfReqSend + 1;
    }

    public void incrementAppServiceAdSend() {
        appServiceAdSend = appServiceAdSend + 1;
    }

    public void incrementAppTokenSend() {
        appTokenSend = appTokenSend + 1;
    }

}
