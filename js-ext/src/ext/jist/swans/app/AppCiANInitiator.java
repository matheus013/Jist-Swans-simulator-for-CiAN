package ext.jist.swans.app;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import ext.util.stats.DucksCompositionStats;

public class AppCiANInitiator extends AppCiANBase
{
    private static final int              STATE_SEARCHING      = 0;
    private static final int              STATE_AWAITING_TIMER = 1;
    private static final int              STATE_OBSERVING      = 2;

    private int                           state;
    private HashMap<String, CiANWorkflow> pendingWf;
    private List<Message>                 completedMessages;
    private int                           msgId;
    private int                           reqSize;
    private int                           waitTimeStart;
    private int                           waitTimeEnd;
    private int                           duration;

    public AppCiANInitiator(int nodeId, DucksCompositionStats stats, int reqSize, int reqRate, int waitTimeStart,
            int waitTimeEnd, int duration) {
        super(nodeId, stats);
        this.state = STATE_SEARCHING;
        this.pendingWf = new HashMap<String, CiANWorkflow>();
        this.completedMessages = new ArrayList<Message>();
        this.msgId = 1;
        this.reqSize = reqSize;
        this.waitTimeStart = waitTimeStart;
        this.waitTimeEnd = waitTimeEnd;
        this.duration = duration;
    }

    @Override
    public void run(String[] args) {
        // startup delay
        if (JistAPI.getTime() == 0) {
            JistAPI.sleep((this.waitTimeStart) * Constants.SECOND);
        }

        CiANWorkflow wf = createWorkflow();
        search(wf);
        compositionStats.incrementNumReq();
    }

    @Override
    public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority,
            byte ttl) {

        // filter message duplicates
        if (completedMessages.contains(msg)) {
            return;
        } else {
            completedMessages.add(msg);
        }

        // ignore message if not a pending workflow
        if (msg instanceof CiANWorkflowMessage) {
            if (!pendingWf.containsKey(((CiANWorkflowMessage) msg).getId()))
                return;
        }

        // handle workflow message
        if (msg instanceof CiANWorkflowRequest) {
            handleWorkflowRequest((CiANWorkflowRequest) msg, src);
        } else if (msg instanceof CiANServiceAd) {
            handleServiceAd((CiANServiceAd) msg, src);
        } else if (msg instanceof CiANToken) {
            handleToken((CiANToken) msg, src);
        } else {
            return;
        }
    }

    protected void handleWorkflowRequest(CiANWorkflowRequest req, NetAddress src) {
        CiANWorkflow localWf = pendingWf.get(req.getId());
        if (localWf == null)
            return;

        switch (state) {
            case STATE_OBSERVING:
                if (localWf.getVersion() > req.getVersion())
                    return;
                // TODO What for? Need to be put back to pendingWf
                localWf = new CiANWorkflow(req.getId(), req.getVersion(), req.getServicesCopy(), req.getInputCopy(),
                        req.getNextIndexToExecute(), JistAPI.getTime());
                break;
            default:
                break;
        }

    }

    protected void handleServiceAd(CiANServiceAd ad, NetAddress src) {
        CiANWorkflow wf = pendingWf.get(ad.getId());
        switch (state) {
            case STATE_SEARCHING:
                state = STATE_AWAITING_TIMER;
            default:
                wf.updateProviderFor(ad.getService(), new CiANProvider(ad.getAdvertiser(), ad.getConnectivity()));
                break;
        }
    }

    protected void handleToken(CiANToken t, NetAddress src) {
        CiANWorkflow wf = pendingWf.get(t.getId());
        switch (state) {
            case STATE_OBSERVING:
                char service = t.getService();
                int in = t.getInput();
                wf.updateInputFor(service, in);
                if (wf.isLastService(service)) {
                    compositionStats.incrementInvokeSuccess(CiANCompositionMessage.STRING_DESTINATION);
                    compositionStats.registerForwardToExecEndTime(CiANCompositionMessage.STRING_DESTINATION,
                            wf.getId(), JistAPI.getTime());
                    compositionStats.setLastServiceExecuted(CiANCompositionMessage.STRING_DESTINATION);
                    compositionStats.setiKnowsLastServiceExecuted(CiANCompositionMessage.STRING_DESTINATION);
                    compositionStats.setiKnowsLastServiceBound(CiANCompositionMessage.STRING_DESTINATION);
                } else {
                    compositionStats.setiKnowsLastServiceExecuted(String.valueOf((char) (service - 1)));
                    compositionStats.setiKnowsLastServiceBound(String.valueOf(service));
                }
                break;
            default:
                break;
        }
    }

    protected void timeout(String wkId) {
        CiANWorkflow wf = pendingWf.get(wkId);
        if (wf == null)
            return;

        switch (state) {
            case STATE_SEARCHING:
                if (JistAPI.getTime() <= ((this.duration - this.waitTimeEnd) * Constants.SECOND) - reqSize
                        * (TIMEOUT_SEND_SERVICE_AD + TIMEOUT_COOL_OFF)) {
                    JistAPI.sleep(SLEEP_BEFORE_RETRY);
                    search(wf);
                }
                break;
            case STATE_AWAITING_TIMER:
                handOff(wf);
                state = STATE_OBSERVING;
                break;
            default:
                break;
        }
    }

    public static void timeoutProxyMethod(String wfId, AppCiANInitiator provider) {
        provider.timeout(wfId);
    }

    private void scheduleTimeoutFor(String wfId, Long at) {
        try {
            Method m = getClass().getMethod("timeoutProxyMethod", new Class[] { String.class, getClass() });
            Object[] parameters = new Object[] { wfId, this };
            JistAPI.callStaticAt(m, parameters, at);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private CiANWorkflow createWorkflow() {
        CiANWorkflow wf = new CiANWorkflow(nodeId, msgId, reqSize);
        pendingWf.put(wf.getId(), wf);
        msgId++;
        compositionStats.setLastService(Character.toString(wf.getLastRealService()));
        compositionStats.registerBindStartTime("A", wf.getId(), JistAPI.getTime());

        return wf;
    }

    private void search(CiANWorkflow wf) {
        CiANWorkflowRequest wfreq = new CiANWorkflowRequest(wf.getId(), wf.getVersion(), wf.getServices(),
                wf.getInputs(), wf.getNextIndexToExecute());
        send(wfreq);
        scheduleTimeoutFor(wf.getId(), JistAPI.getTime() + TIMEOUT_SEND_SERVICE_AD + TIMEOUT_COOL_OFF);
    }

    private void handOff(CiANWorkflow wf) {
        int index = wf.getNextIndexToExecute();
        List<CiANProvider> providers = wf.getProvidersFor(index);
        if (providers.size() > 0) {
            char service = wf.getServiceForIndex(index);
            CiANToken t = new CiANToken(wf.getId(), service, wf.getInputForService(service), providers.get(0)
                    .getNodeId());
            send(t);
            compositionStats.addServiceProvider(wf.getId(), new Integer(providers.get(0).getNodeId()).toString());
            compositionStats.incrementSearchSuccess(String.valueOf(service));
            compositionStats.setLastServiceBound(String.valueOf(service));
            compositionStats.setiKnowsLastServiceBound(String.valueOf(service));
        } else {
            state = STATE_SEARCHING;
            search(wf);
        }
    }

}
