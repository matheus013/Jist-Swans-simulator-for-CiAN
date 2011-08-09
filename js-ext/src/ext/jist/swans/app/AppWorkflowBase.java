package ext.jist.swans.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jist.swans.Constants;
import jist.swans.misc.Message;
import jist.swans.misc.Sizeof;
import jist.swans.net.NetAddress;
import ext.util.stats.DucksCompositionStats;

public abstract class AppWorkflowBase extends AppCompositionBase
{
    /**
     * Time window to send a service ad
     */
    public static final long TIMEOUT_SEND_SERVICE_AD = 80 * Constants.MILLI_SECOND;
    /**
     * Cool-off time after sending in which messages can only be received
     */
    public static final long TIMEOUT_COOL_OFF        = 20 * Constants.MILLI_SECOND;
    /**
     * sleep before sending the retry wf-req
     */
    public static final long SLEEP_BEFORE_RETRY      = 2 * Constants.SECOND;

    public AppWorkflowBase(int nodeId, DucksCompositionStats compositionStats) {
        super(nodeId, compositionStats);
    }

    protected abstract void handleWorkflowRequest(WorkflowRequest req, NetAddress src);

    protected abstract void handleServiceAd(ServiceAd ad, NetAddress src);

    protected abstract void handleToken(Token t, NetAddress src);

    protected abstract void timeout(String wkId);

    protected void send(Message msg) {
        netEntity.send(msg, NetAddress.ANY, NET_PROTOCOL_NUMBER, Constants.NET_PRIORITY_NORMAL, new Byte("0"));
    }

}

/**
 * This is the local representation of a workflow.
 * 
 * @author ristin
 * 
 */
class Workflow
{
    public final static char     SYMBOL_DESTINATION = '\r';
    public final static String   STRING_DESTINATION = "dst";
    public final static int      INITIAL_WF_VERSION = 1;

    private int                  initiatorId;
    private String               id;
    private int                  version;                   // currently not
                                                             // used, but later
                                                             // when wf is
                                                             // transformed
    private char[]               services;
    private int[]                inputs;
    private List<List<Provider>> providers;
    private long                 arrival;
    private int                  nextIndexToExecute;

    public Workflow(int nodeId, int msgId, int reqSize) {
        this.id = (new Integer(nodeId)).toString() + "-" + (new Integer(msgId)).toString();
        this.version = INITIAL_WF_VERSION;
        this.services = createServices(reqSize);
        this.inputs = createInputs(services.length);
        this.providers = createProviders(services.length);
        this.nextIndexToExecute = 0;
    }

    public Workflow(String id, int version, char[] services, int[] inputs, int nextIndexToExecute, long arrival) {
        this.initiatorId = Integer.parseInt(id.split("-")[0]);
        this.id = id;
        this.version = version;
        this.services = services;
        this.inputs = inputs;
        this.nextIndexToExecute = nextIndexToExecute;
        this.providers = createProviders(services.length);
        this.arrival = arrival;
    }

    private char[] createServices(int size) {
        char[] services = new char[size + 1];
        char service = 'A';
        int index = 0;

        while (index < size) {
            services[index] = service;
            index++;
            service++;
        }
        services[size] = SYMBOL_DESTINATION;
        // compositionStats.setLastService(Character.toString(services[size-1]));
        return services;
    }

    private int[] createInputs(int size) {
        int[] inputs = new int[size];

        for (int i = 0; i < size; i++) {
            if (i == 0)
                inputs[i] = 1;
            else
                inputs[i] = -1;
        }

        return inputs;
    }

    private List<List<Provider>> createProviders(int size) {
        List<List<Provider>> providers = new ArrayList<List<Provider>>();

        for (int i = 0; i < size; i++) {
            List<Provider> tmp = new ArrayList<Provider>();
            // tmp.add(new NetAddress(new byte[] { 0, 0, 0, (byte) initiatorId
            // }))
            Provider p = new Provider(initiatorId, 0);
            if (i == size - 1)
                tmp.add(p);
            providers.add(tmp);
        }

        return providers;
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public char[] getServices() {
        return services;
    }

    public int[] getInputs() {
        return inputs;
    }

    public List<List<Provider>> getProviders() {
        return providers;
    }

    public int getNextIndexToExecute() {
        return nextIndexToExecute;
    }

    public void advanceIndexToExecute() {
        nextIndexToExecute = nextIndexToExecute + 1;
    }

    public void advanceIndexToExecute(char service) {
        nextIndexToExecute = getIndexForService(service);
    }

    public long getArrival() {
        return arrival;
    }

    public void updateProviderFor(char service, Provider provider) {
        int index = getIndexForService(service);
        boolean isAdded = false;
        if (index == -1)
            return;

        List<Provider> l = providers.get(index);
        for (int i = 0; i < l.size(); i++) {
            Provider cur = l.get(i);
            if (provider.getConnectivity() > cur.getConnectivity()) {
                l.add(i, provider);
                isAdded = true;
                break;
            }
        }
        if (!isAdded)
            l.add(provider);
    }

    public List<Provider> getProvidersFor(int index) {
        return providers.get(index);
    }

    public void updateInputFor(char service, int input) {
        int index = getIndexForService(service);
        if (index == -1)
            return;

        inputs[index] = input;
    }

    private int getIndexForService(char service) {
        for (int i = 0; i < services.length; i++) {
            if (services[i] == service)
                return i;
        }
        return -1;
    }

    public int getInputForService(char service) {
        int input = -2;

        int index = getIndexForService(service);
        if (index > -1 && index < inputs.length) {
            return inputs[index];
        }

        return input;
    }

    public char getServiceForIndex(int index) {
        char s = '\0';
        if (index > -1 && index < services.length) {
            return services[index];
        }
        return s;
    }

    public char getSuccessorService(char service) {
        char s = '\0';
        int index = getIndexForService(service);

        if (index > -1 && index < services.length - 1) {
            return services[index + 1];
        }

        return s;
    }

    public char getLastRealService() {
        return services[services.length - 2];
    }

    public boolean isLastService(char service) {
        if (service == SYMBOL_DESTINATION) {
            return true;
        }
        return false;
    }

    public boolean isServiceProvided(int index) {
        if (providers.get(index).size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isPartOfProviderList(char service, int providerId) {
        List<Provider> providers = getProvidersFor(getIndexForService(service));
        Iterator<Provider> it = providers.iterator();
        Provider p;
        while (it.hasNext()) {
            p = it.next();
            if (p.getNodeId() == providerId) {
                return true;
            }
        }
        return false;
    }
}

class Provider
{
    int    nodeId;
    double connectivity;

    public Provider(int nodeId, double connectivity) {
        this.nodeId = nodeId;
        this.connectivity = connectivity;
    }

    public int getNodeId() {
        return nodeId;
    }

    public double getConnectivity() {
        return connectivity;
    }
}

abstract class WorkflowMessage implements Message
{
    protected String id;

    public WorkflowMessage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract void getBytes(byte[] msg, int offset);

    public abstract int getSize();
}

/**
 * This is the immutable representation of a workflow to send over the network
 * 
 * @author ristin
 * 
 */
class WorkflowRequest extends WorkflowMessage
{
    private int    version;
    private char[] services;
    private int[]  inputs;
    private int    nextIndexToExecute;

    public WorkflowRequest(String id, int version, char[] services, int[] inputs, int nextIndexToExecute) {
        super(id);
        this.version = version;
        this.services = services;
        this.inputs = inputs;
        this.nextIndexToExecute = nextIndexToExecute;
    }

    public int getVersion() {
        return version;
    }

    public char[] getServicesCopy() {
        char[] copy = new char[services.length];
        System.arraycopy(services, 0, copy, 0, services.length);
        return copy;
    }

    public int[] getInputCopy() {
        int[] copy = new int[inputs.length];
        System.arraycopy(inputs, 0, copy, 0, inputs.length);
        return copy;
    }

    public int getNextIndexToExecute() {
        return nextIndexToExecute;
    }

    public void getBytes(byte[] msg, int offset) {
        throw new RuntimeException("not implemented");
    }

    public int getSize() {
        return Sizeof.inst(id.toCharArray()) + Sizeof.inst(version) + Sizeof.inst(services) + Sizeof.inst(inputs)
                + Sizeof.inst(nextIndexToExecute);
    }
}

class ServiceAd extends WorkflowMessage
{
    private char   service;
    private int    advertiser;
    private double connectivity;

    public ServiceAd(String id, int advertiser, char service, double connectivity) {
        super(id);
        this.advertiser = advertiser;
        this.service = service;
        this.connectivity = connectivity;
    }

    public char getService() {
        return service;
    }

    public int getAdvertiser() {
        return advertiser;
    }

    public double getConnectivity() {
        return connectivity;
    }

    @Override
    public void getBytes(byte[] msg, int offset) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getSize() {
        return Sizeof.inst(id.toCharArray()) + Sizeof.inst(advertiser) + Sizeof.inst(service)
                + Sizeof.inst(connectivity);
    }
}

class Token extends WorkflowMessage
{
    private char service;
    private int  input;
    private int  provider;

    public Token(String id, char nextService, int input, int provider) {
        super(id);
        this.service = nextService;
        this.input = input;
        this.provider = provider;
    }

    public char getService() {
        return service;
    }

    public int getInput() {
        return input;
    }

    public int getProvider() {
        return provider;
    }

    @Override
    public void getBytes(byte[] msg, int offset) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getSize() {
        return Sizeof.inst(id.toCharArray()) + Sizeof.inst(service) + Sizeof.inst(input) + Sizeof.inst(provider);
    }
}
