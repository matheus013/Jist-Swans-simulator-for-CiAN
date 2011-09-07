package ext.jist.swans.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.misc.Sizeof;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetInterface.NetHandler;
import ext.util.stats.DucksCompositionStats;

public abstract class AppCiANBase implements AppInterface, NetHandler
{
    /**
     * Defines the IP protocol number since we directly use the network layer,
     * surpassing e.g. transport layer (UDP,TCP) Note: this protocol number
     * should be defined in jist.swans.Constants but is done here for modularity
     * reasons
     */
    public static final short       NET_PROTOCOL_NUMBER     = 524;

    /**
     * Time window to send a service ad
     */
    public static final long        TIMEOUT_SEND_SERVICE_AD = 80 * Constants.MILLI_SECOND;
    /**
     * Cool-off time after sending in which messages can only be received
     */
    public static final long        TIMEOUT_COOL_OFF        = 20 * Constants.MILLI_SECOND;
    /**
     * sleep before sending the retry wf-req
     */
    public static final long        SLEEP_BEFORE_RETRY      = 2 * Constants.SECOND;

    // network entity.
    protected NetInterface          netEntity;

    // self-referencing proxy entity.
    protected Object                self;

    protected int                   nodeId;

    // composition stats accumulator
    protected DucksCompositionStats compositionStats;

    public AppCiANBase(int nodeId, DucksCompositionStats compositionStats) {
        this.nodeId = nodeId;
        this.compositionStats = compositionStats;
        // init self reference
        this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class, NetInterface.NetHandler.class });
    }

    public void run() {
        run(null);
    }

    public abstract void run(String[] args);

    public abstract void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst,
            byte priority, byte ttl);

    /**
     * Set network entity.
     * 
     * @param netEntity
     *            network entity
     */
    public void setNetEntity(NetInterface netEntity) {
        this.netEntity = netEntity;
    }

    /**
     * Return self-referencing NETWORK proxy entity.
     * 
     * @return self-referencing NETWORK proxy entity
     */
    public NetInterface.NetHandler getNetProxy() {
        return (NetInterface.NetHandler) self;
    }

    /**
     * Return self-referencing APPLICATION proxy entity.
     * 
     * @return self-referencing APPLICATION proxy entity
     */
    public AppInterface getAppProxy() {
        return (AppInterface) self;
    }

    protected void send(Message msg) {
        netEntity.send(msg, NetAddress.ANY, NET_PROTOCOL_NUMBER, Constants.NET_PRIORITY_NORMAL, new Byte("0"));
    }

    protected void send(Message msg, NetAddress addr) {
        netEntity.send(msg, addr, NET_PROTOCOL_NUMBER, Constants.NET_PRIORITY_NORMAL, new Byte("0"));
    }
}

class CiANWorkflow
{
    public final static char         SYMBOL_DESTINATION = '\r';
    public final static String       STRING_DESTINATION = "dst";
    public final static int          INITIAL_WF_VERSION = 1;

    private int                      initiatorId;
    private String                   id;
    private int                      version;                   // currently not
                                                                 // used, but later
                                                                 // when wf is
                                                                 // transformed
    private char[]                   services;
    private int[]                    inputs;
    private List<List<CiANProvider>> providers;
    private long                     arrival;
    private int                      nextIndexToExecute;

    public CiANWorkflow(int nodeId, int msgId, int reqSize) {
        this.id = (new Integer(nodeId)).toString() + "-" + (new Integer(msgId)).toString();
        this.version = INITIAL_WF_VERSION;
        this.services = createServices(reqSize);
        this.inputs = createInputs(services.length);
        this.providers = createProviders(services.length);
        this.nextIndexToExecute = 0;
    }

    public CiANWorkflow(String id, int version, char[] services, int[] inputs, int nextIndexToExecute, long arrival) {
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

    private List<List<CiANProvider>> createProviders(int size) {
        List<List<CiANProvider>> providers = new ArrayList<List<CiANProvider>>();

        for (int i = 0; i < size; i++) {
            List<CiANProvider> tmp = new ArrayList<CiANProvider>();
            // tmp.add(new NetAddress(new byte[] { 0, 0, 0, (byte) initiatorId
            // }))
            CiANProvider p = new CiANProvider(initiatorId, 0);
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

    public List<List<CiANProvider>> getProviders() {
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

    public void updateProviderFor(char service, CiANProvider provider) {
        int index = getIndexForService(service);
        boolean isAdded = false;
        if (index == -1)
            return;

        List<CiANProvider> l = providers.get(index);
        for (int i = 0; i < l.size(); i++) {
            CiANProvider cur = l.get(i);
            if (provider.getConnectivity() > cur.getConnectivity()) {
                l.add(i, provider);
                isAdded = true;
                break;
            }
        }
        if (!isAdded)
            l.add(provider);
    }

    public List<CiANProvider> getProvidersFor(int index) {
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
        List<CiANProvider> providers = getProvidersFor(getIndexForService(service));
        Iterator<CiANProvider> it = providers.iterator();
        CiANProvider p;
        while (it.hasNext()) {
            p = it.next();
            if (p.getNodeId() == providerId) {
                return true;
            }
        }
        return false;
    }
}

class CiANProvider
{
    int    nodeId;
    double connectivity;

    public CiANProvider(int nodeId, double connectivity) {
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

abstract class CiANWorkflowMessage implements Message
{
    protected String id;

    public CiANWorkflowMessage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract void getBytes(byte[] msg, int offset);

    public abstract int getSize();
}

class CiANWorkflowRequest extends CiANWorkflowMessage
{
    private int    version;
    private char[] services;
    private int[]  inputs;
    private int    nextIndexToExecute;

    public CiANWorkflowRequest(String id, int version, char[] services, int[] inputs, int nextIndexToExecute) {
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

class CiANServiceAd extends CiANWorkflowMessage
{
    private char   service;
    private int    advertiser;
    private double connectivity;

    public CiANServiceAd(String id, int advertiser, char service, double connectivity) {
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

class CiANToken extends CiANWorkflowMessage
{
    private char service;
    private int  input;
    private int  provider;

    public CiANToken(String id, char nextService, int input, int provider) {
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

abstract class CiANServiceMessage implements Message
{
    protected String id;

    public CiANServiceMessage(int nodeId, int msgId) {
        this.id = (new Integer(nodeId)).toString() + "-" + (new Integer(msgId)).toString();
    }

    public CiANServiceMessage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract void getBytes(byte[] msg, int offset);

    public abstract int getSize();
}

class CiANDiscoveryRequest extends CiANServiceMessage
{
    private char   serviceName;
    private String compositionRequestId;

    public CiANDiscoveryRequest(int nodeId, int msgId, char serviceName) {
        super(nodeId, msgId);
        this.serviceName = serviceName;
        this.compositionRequestId = this.getId();
    }

    public CiANDiscoveryRequest(int nodeId, int msgId, char serviceName, String compositionRequestId) {
        super(nodeId, msgId);
        this.serviceName = serviceName;
        this.compositionRequestId = compositionRequestId;
    }

    public char getServiceName() {
        return serviceName;
    }

    public String getCompositionRequestId() {
        return compositionRequestId;
    }

    public void getBytes(byte[] msg, int offset) {
        throw new RuntimeException("not implemented");
    }

    public int getSize() {
        return Sizeof.inst(id.toCharArray()) + Sizeof.inst(serviceName);
    }
}

class CiANDiscoveryResponse extends CiANServiceMessage
{
    private String service;

    public CiANDiscoveryResponse(String id, String service) {
        super(id);
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public void getBytes(byte[] msg, int offset) {
        throw new RuntimeException("not implemented");
    }

    public int getSize() {
        return Sizeof.inst(id.toCharArray()) + Sizeof.inst(service);
    }
}

class CiANCompositionMessage extends CiANServiceMessage
{
    public final static char   SYMBOL_DESTINATION = '\r';
    public final static String STRING_DESTINATION = "dst";
    private char[]             services;
    private int[]              providers;
    private int[]              input;
    private int                lastModifiedBy;

    public CiANCompositionMessage(String id, char[] services, int[] providers, int[] input, int lastModifiedBy) {
        super(id);
        this.services = services;
        this.providers = providers;
        this.input = input;
        this.lastModifiedBy = lastModifiedBy;
    }

    public int[] getProvidersCopy() {
        int[] copy = new int[providers.length];
        System.arraycopy(providers, 0, copy, 0, providers.length);
        return copy;
    }

    public char[] getServicesCopy() {
        char[] copy = new char[services.length];
        System.arraycopy(services, 0, copy, 0, services.length);
        return copy;
    }

    public int[] getInputCopy() {
        int[] copy = new int[input.length];
        System.arraycopy(input, 0, copy, 0, input.length);
        return copy;
    }

    public int getLastModifiedBy() {
        return lastModifiedBy;
    }

    public int getIndexNextEmptyProvider() {
        for (int i = 0; i < providers.length; i++) {
            if (providers[i] == -1)
                return i;
        }
        return -1;
    }

    public int getIndexNextInput() {
        for (int i = 0; i < input.length; i++) {
            if (input[i] == -1)
                return i - 1;
        }
        return -1;
    }

    public String getServiceAt(int index) {
        if (index == services.length - 1)
            return STRING_DESTINATION;
        try {
            char s = services[index];
            return Character.toString(s);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("index = " + index);
            throw e;
        }
    }

    public int getIndexLastService() {
        return services.length - 2;
    }

    public int getDestinationId() {
        return providers[providers.length - 1];
    }

    public int getDestinationInput() {
        return input[providers.length - 1];
    }

    public void getBytes(byte[] msg, int offset) {
        throw new RuntimeException("not implemented");
    }

    public int getSize() {
        return Sizeof.inst(id.toCharArray()) + Sizeof.inst(services) + Sizeof.inst(providers) + Sizeof.inst(input);
    }
}
