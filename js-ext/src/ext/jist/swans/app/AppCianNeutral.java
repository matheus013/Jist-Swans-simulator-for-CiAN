package ext.jist.swans.app;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ducks.driver.SimParams;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import ext.util.stats.DucksCompositionStats;

public class AppCianNeutral extends AppCianBase {
	private static final int STATE_LISTENING = 0;
	private static final int STATE_APPLYING = 1;
	private static final int STATE_OBSERVING = 2;
	private static final int STATE_EXECUTING = 3;
	private static final int STATE_AWAITING_TIMER = 4;
	private static final int STATE_SEARCHING = 5;
	private static final int CODE_CONNECT_TO_INITIATOR = 1;
	private static final int CODE_CONNECT_TO_CONTROLLER = 2;

	private int state;
	private boolean timeUp;
	private int initiatorConnectivity;
	private int controllerConnectivity;
	private HashMap<Character, Integer> repository;
	private HashMap<String, Workflow> pendingWf;
	private String compoRestrict;
	private boolean alligible;

	private List<Message> completedMessages;

	public AppCianNeutral(int nodeId, DucksCompositionStats stats, String mode,
			HashMap<Character, Integer> repository, String compoRestrict) {
		super(nodeId, stats);
		this.state = STATE_LISTENING;
		this.timeUp = false;
		this.initiatorConnectivity = 0;
		this.controllerConnectivity = 0;
		this.repository = repository;
		// if (this.nodeId == 4) {
		// this.repository = new HashMap<Character, Integer>();
		// this.repository.put('B', 23);
		// this.repository.put('C', 23);
		// }
		// // else{
		// // this.repository = new HashMap<Character, Integer>();
		// // }
		this.pendingWf = new HashMap<String, Workflow>();
		this.compoRestrict = compoRestrict;
		this.completedMessages = new ArrayList<Message>();
		this.alligible = true;
	}

	@Override
	public void run(String[] args) {
	}

	@Override
	public void receive(Message msg, NetAddress src, MacAddress lastHop,
			byte macId, NetAddress dst, byte priority, byte ttl) {

		// avoid duplicate messages
		if (completedMessages.contains(msg)) {
			return;
		} else {
			completedMessages.add(msg);
		}

		// handle message
		if (msg.getClass().equals(WorkflowRequest.class)) {
			handleWorkflowRequest((WorkflowRequest) msg, src);
		} else if (msg.getClass().equals(ServiceAd.class)) {
			handleServiceAd((ServiceAd) msg, src);
		} else if (msg.getClass().equals(Token.class)) {
			handleToken((Token) msg, src);
		} else {
			return;
		}
	}

	protected void handleWorkflowRequest(WorkflowRequest req, NetAddress src) {
		int pos_wf = -1;
		int pos_s = -1;
		Workflow wf = null;
		long now = JistAPI.getTime();
		String reqId = req.getId();

		switch (state) {
		case STATE_LISTENING:
			if (!alligible)
				return;
			wf = new Workflow(reqId, req.getVersion(), req.getServicesCopy(),
					req.getInputCopy(), req.getNextIndexToExecute(), now);
			pos_wf = wf.getNextIndexToExecute();
			pos_s = nextMatchingPosition(wf, pos_wf);
			if (pos_s == -1)
				return;
			pendingWf.put(wf.getId(), wf);
			if (wf.getVersion() == Workflow.INITIAL_WF_VERSION) {
				initiatorConnectivity = CODE_CONNECT_TO_INITIATOR;
			}
			controllerConnectivity = CODE_CONNECT_TO_CONTROLLER;
			if (pos_s == pos_wf) {
				if (now < wf.getArrival() + TIMEOUT_SEND_SERVICE_AD) {
					state = STATE_APPLYING;
					apply(wf, pos_s);
				}
			} else {
				state = STATE_OBSERVING;
			}
			break;
		case STATE_OBSERVING:
			wf = pendingWf.get(reqId);
			if (wf == null)
				return;
			if (wf.getVersion() > req.getVersion())
				return;
			pos_wf = req.getNextIndexToExecute();
			wf = new Workflow(reqId, req.getVersion(), req.getServicesCopy(),
					req.getInputCopy(), pos_wf, now);
			break;
		default:
			break; // commit to one workflow at a time
		}
	}

	protected void handleServiceAd(ServiceAd ad, NetAddress src) {
		Workflow wf_pending = pendingWf.get(ad.getId());
		if (wf_pending == null)
			return;

		int index_pending = wf_pending.getNextIndexToExecute();
		char s_pending = wf_pending.getServiceForIndex(index_pending);

		switch (state) {
		case STATE_LISTENING:
			break;
		case STATE_SEARCHING:
			if (ad.getService() == s_pending && timeUp == false) {
				wf_pending.updateProviderFor(ad.getService(),
						new Provider(ad.getAdvertiser(), ad.getConnectivity()));
			}
			break;
		case STATE_EXECUTING:
			if (ad.getService() == s_pending && timeUp == false) {
				wf_pending.updateProviderFor(ad.getService(),
						new Provider(ad.getAdvertiser(), ad.getConnectivity()));
			}
			break;
		case STATE_AWAITING_TIMER:
			if (ad.getService() == s_pending && timeUp == false) {
				wf_pending.updateProviderFor(ad.getService(),
						new Provider(ad.getAdvertiser(), ad.getConnectivity()));
			}
			break;
		default: // STATE_APPLYING, STATE_OBSERVING
			if (ad.getService() == s_pending) {
				wf_pending.updateProviderFor(ad.getService(),
						new Provider(ad.getAdvertiser(), ad.getConnectivity()));
			}
			break;
		}
	}

	protected void handleToken(Token t, NetAddress src) {
		String wfId = t.getId();
		Workflow wf_pending = pendingWf.get(wfId);
		if (wf_pending == null)
			return;

		int pos_wf = -1;
		int pos_s = -1;
		char service = t.getService();
		int in = t.getInput();
		int bestCandidate = t.getProvider();

		controllerConnectivity = 0; // set to default

		switch (state) {
		case STATE_APPLYING:
			wf_pending.updateInputFor(service, in);
			wf_pending.advanceIndexToExecute(service);
			// TODO: set decision
			if (this.nodeId == bestCandidate) {
				state = STATE_EXECUTING;
				execute(wf_pending);
			} else {
				if (wf_pending.isPartOfProviderList(service, bestCandidate)) {
					controllerConnectivity = CODE_CONNECT_TO_CONTROLLER;
					wf_pending.advanceIndexToExecute();
					pos_wf = wf_pending.getNextIndexToExecute();
					pos_s = nextMatchingPosition(wf_pending, pos_wf);
					if (pos_s == pos_wf) {
						state = STATE_APPLYING;
						apply(wf_pending, pos_s);
					} else {
						cleanup(wfId);
					}
				} else {
					cleanup(wfId);
				}
			}
			break;
		case STATE_OBSERVING:
			wf_pending.updateInputFor(service, in);
			wf_pending.advanceIndexToExecute(service);
			// TODO: set decision
			if (wf_pending.isPartOfProviderList(service, bestCandidate)) {
				controllerConnectivity = CODE_CONNECT_TO_CONTROLLER;
				wf_pending.advanceIndexToExecute();
				pos_wf = wf_pending.getNextIndexToExecute();
				pos_s = nextMatchingPosition(wf_pending, pos_wf);
				if (pos_s == pos_wf) {
					state = STATE_APPLYING;
					apply(wf_pending, pos_s);
				}
			} else {
				cleanup(wfId);
			}

			// TODO: stay in observing only if still connected to
			// controller: service ad received twice and one src is
			// controller
			break;
		default:
			break;
		}
	}

	protected void timeout(String wfId) {
		Workflow wf_pending = pendingWf.get(wfId);
		if (wf_pending == null)
			return;

		timeUp = true;
		int pos_wf = wf_pending.getNextIndexToExecute();
		char service = wf_pending.getServices()[pos_wf];
		int in = wf_pending.getInputs()[pos_wf];

		switch (state) {
		case STATE_AWAITING_TIMER:
			if (wf_pending.isServiceProvided(pos_wf)) {
				int provider = select(wf_pending.getProvidersFor(pos_wf));
				handOff(wfId, service, in, provider, false);
			} else {
				state = STATE_SEARCHING;
				JistAPI.sleep(SLEEP_BEFORE_RETRY);
				search(wf_pending);
			}
			break;
		case STATE_SEARCHING:
			if (wf_pending.isServiceProvided(pos_wf)) {
				int provider = select(wf_pending.getProvidersFor(pos_wf));
				handOff(wfId, service, in, provider, false);
			} else {
				JistAPI.sleep(SLEEP_BEFORE_RETRY);
				search(wf_pending);
			}
			break;
		default: // STATE_LISTENING, STATE_EXECUTING STATE_AWAITING_TOKEN
			break;
		}
	}

	private void scheduleTimeoutFor(String wfId, Long at) {
		try {
			timeUp = false;
			Method m = ext.jist.swans.app.AppCianNeutral.class.getMethod(
					"timeoutProxyMethod", new Class[] { java.lang.String.class,
							ext.jist.swans.app.AppCianNeutral.class });
			Object[] parameters = new Object[] { wfId, this };
			JistAPI.callStaticAt(m, parameters, at);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	public static void timeoutProxyMethod(String wfId, AppCianNeutral provider) {
		provider.timeout(wfId);
	}

	private void apply(Workflow wf, int pos) {
		char service = wf.getServiceForIndex(pos);
		String wfId = wf.getId();
		double connectivity = initiatorConnectivity + controllerConnectivity;
		ServiceAd ad = new ServiceAd(wfId, this.nodeId, service, connectivity);
		JistAPI.sleep(nodeId * Constants.MILLI_SECOND); // sleep otherwise nodes
		// send at the same time
		// and miss each others
		// ads
		send(ad);
		wf.updateProviderFor(service, new Provider(this.nodeId, connectivity));
	}

	private int nextMatchingPosition(Workflow wf, int offset) {
		int pos = -1;
		char[] services = wf.getServices();

		for (int i = offset; i < services.length; i++) {
			if (repository.containsKey(services[i])) {
				return pos = i;
			}
		}

		return pos;
	}

	private void execute(Workflow wf) {
		String wfId = wf.getId();
		int pos_wf = wf.getNextIndexToExecute();

		// start timeout for next service
		wf.advanceIndexToExecute();
		if (!wf.isLastService(wf.getServiceForIndex(wf.getNextIndexToExecute()))) {
			scheduleTimeoutFor(wfId, JistAPI.getTime()
					+ TIMEOUT_SEND_SERVICE_AD + TIMEOUT_COOL_OFF);
		}

		if (compoRestrict.equals(SimParams.COMPOSITION_RESTRICTION_NO_REPEAT)) {
			alligible = false;
		}

		char service = wf.getServiceForIndex(pos_wf);
		int in = wf.getInputForService(service);
		if (in == -2) {
			cleanup(wfId);
			return;
		}

		in = serviceImpl(in, repository.get(service));
		compositionStats.incrementInvokeSuccess(String.valueOf(service));
		compositionStats.setLastServiceExecuted(String.valueOf(service));
		char nextService = wf.getSuccessorService(service);
		wf.updateInputFor(nextService, in);
		if (timeUp) {
			if (wf.isServiceProvided(pos_wf++)) {
				handOff(wfId, nextService, in,
						select(wf.getProvidersFor(pos_wf)), false);
			} else {
				state = STATE_SEARCHING;
				JistAPI.sleep(SLEEP_BEFORE_RETRY);
				search(wf);
			}
		} else {
			if (wf.isLastService(nextService) && wf.isServiceProvided(pos_wf++)) {
				handOff(wfId, nextService, in,
						select(wf.getProvidersFor(pos_wf)), true);
			} else {
				state = STATE_AWAITING_TIMER;
			}
		}
	}

	private void handOff(String wfId, char service, int in, int provider,
			boolean isLast) {
		Token t = new Token(wfId, service, in, provider);
		if (!isLast) {
			send(t);
			compositionStats.addServiceProvider(wfId,
					new Integer(provider).toString());
			compositionStats.incrementSearchSuccess(String.valueOf(service));
			compositionStats.setLastServiceBound(String.valueOf(service));
		} else {
			// unicast workflow result
			NetAddress dst = new NetAddress(new byte[] { 0, 0, 0,
					(byte) provider });
			netEntity.send(t, dst, NET_PROTOCOL_NUMBER,
					Constants.NET_PRIORITY_NORMAL, Constants.TTL_DEFAULT);
			compositionStats
					.setLastServiceBound(CompositionMessage.STRING_DESTINATION);
			// needed only to use DuckStats as is and to have dst entry
			// registered, otherwise end time is not stored
			compositionStats.registerForwardToExecStartTime(
					CompositionMessage.STRING_DESTINATION, wfId,
					JistAPI.getTime());

		}
		cleanup(wfId);
	}

	private int select(List<Provider> providers) {
		if (providers.size() > 0) {
			return providers.get(0).getNodeId();
		}

		return -1;

	}

	private void search(Workflow wf) {
		WorkflowRequest wfreq = new WorkflowRequest(wf.getId(),
				wf.getVersion() + 1, wf.getServices(), wf.getInputs(),
				wf.getNextIndexToExecute());
		send(wfreq);
		scheduleTimeoutFor(wf.getId(), JistAPI.getTime()
				+ TIMEOUT_SEND_SERVICE_AD + TIMEOUT_COOL_OFF);
	}

	private int serviceImpl(int in, int waitTime) {
		int out = 0;
		out = in + 1;
		JistAPI.sleep(waitTime * Constants.MILLI_SECOND);
		return out;
	}

	private void cleanup(String wfId) {
		pendingWf.remove(wfId);
		timeUp = false;
		state = STATE_LISTENING;
		initiatorConnectivity = 0;
		controllerConnectivity = 0;
	}
}
