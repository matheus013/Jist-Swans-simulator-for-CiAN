package ext.util.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jist.swans.Constants;
import ext.util.ExtendedProperties;

public class DucksCompositionStats implements StatsCollector {
	private static final String COMPO_STAT_PROVIDERS = "ducks.app.compo.providers";
	// private static final String COMPO_STAT_PROVIDERS_REPEAT =
	// "ducks.app.compo.providers.repeat";
	private static final String COMPO_STAT_REQ = "ducks.app.compo.requests";
	private static final String COMPO_STAT_SUC_SEARCH = "ducks.app.compo.success.search";
	private static final String COMPO_STAT_SUC_INVOKE = "ducks.app.compo.success.invoke";

	private static final String WF_STAT_LAST_BOUND = "ducks.app.compo.last.bound";
	private static final String WF_STAT_LAST_EXEC = "ducks.app.compo.last.exec";
	private static final String WF_STAT_I_KNOWS_LAST_BOUND = "ducks.app.compo.last.bound.initiator.knows";
	private static final String WF_STAT_I_KNOWS_LAST_EXEC = "ducks.app.compo.last.exec.initiator.knows";

	private static final String COMPO_STAT_DUR_BIND = "ducks.app.compo.duration.service.binding";
	private static final String COMPO_STAT_DUR_FWD_TO_BIND = "ducks.app.compo.duration.service.forward.to.bind.on";
	private static final String COMPO_STAT_DUR_FWD_TO_EXEC = "ducks.app.compo.duration.service.forward.to.exec.on";
	private static final String COMPO_STAT_DUR_COMPO = "ducks.app.compo.duration.composition.total";

	private static final long DUR_UNIT = Constants.NANO_SECOND;

	private String lastService;

	/**
	 * log discover response condition
	 */
	// private static String providersRepeat;

	/**
	 * log number of issued requests
	 */
	private static int numReq;

	/**
	 * count search=binding success
	 */
	private static HashMap<String, Integer> searchSuccess;

	/**
	 * count invoke success
	 */
	private static HashMap<String, Integer> invokeSuccess;

	/**
	 * log service providers
	 */
	private static HashMap<String, List<String>> serviceProviders;

	/**
	 * start and end time of binding=searching a service for a particular
	 * message
	 */
	private static HashMap<String, HashMap<String, List<Long>>> bindTimes;
	/**
	 * start and end time of forwarding to execute a service for a particular
	 * message
	 */
	private static HashMap<String, HashMap<String, List<Long>>> forwardToExecTimes;

	/**
	 * duration for binding a service in a particular message (service, msgId,
	 * duration)
	 */
	private static HashMap<String, HashMap<String, Long>> bindDurations;

	/**
	 * duration of binding end of the this service and binding start of next
	 * service
	 */
	private static HashMap<String, HashMap<String, Long>> forwardToBindDurations;

	/**
	 * duration of starting to forward to a service and the service having
	 * received the message so it can now start to execute
	 */
	private static HashMap<String, HashMap<String, Long>> forwardToExecDurations;

	/**
	 * for each message the duration between the initiator sending the first
	 * discovery request and receiving the completed composition message
	 */
	private static HashMap<String, Long> compositionDurations;

	/*
	 * For workflow stats
	 */
	private static String lastServiceBound;
	private static String lastServiceExecuted;
	private static String iKnowsLastServiceBound;
	private static String iKnowsLastServiceExecuted;

	public DucksCompositionStats() {
		clear();
	}

	public void clear() {
		// providersRepeat ="";
		numReq = 0;
		serviceProviders = new HashMap<String, List<String>>();
		searchSuccess = new HashMap<String, Integer>();
		invokeSuccess = new HashMap<String, Integer>();

		bindTimes = new HashMap<String, HashMap<String, List<Long>>>();
		forwardToExecTimes = new HashMap<String, HashMap<String, List<Long>>>();
		bindDurations = new HashMap<String, HashMap<String, Long>>();
		forwardToBindDurations = new HashMap<String, HashMap<String, Long>>();
		forwardToExecDurations = new HashMap<String, HashMap<String, Long>>();
		compositionDurations = new HashMap<String, Long>();

		lastServiceBound = "empty";
		lastServiceExecuted = "empty";
		iKnowsLastServiceBound = "empty";
		iKnowsLastServiceExecuted = "empty";
	}

	public String[] getStatParams() {
		// determineCompositionDurations();
		// determineBindDurations();
		// determineForwardToBindDurations();
		// determineForwardToExecDurations();
		String[] params = new String[1 + 2 + 2
				+ serviceProviders.keySet().size()
				+ searchSuccess.keySet().size() + invokeSuccess.keySet().size()
				+ countMapEntries(bindDurations)
				+ countMapEntries(forwardToBindDurations)
				+ countMapEntries(forwardToExecDurations)
				+ compositionDurations.keySet().size()];
		params[0] = COMPO_STAT_REQ;
		// params[1] = COMPO_STAT_PROVIDERS_REPEAT;
		params[1] = WF_STAT_LAST_BOUND;
		params[2] = WF_STAT_LAST_EXEC;
		params[3] = WF_STAT_I_KNOWS_LAST_BOUND;
		params[4] = WF_STAT_I_KNOWS_LAST_EXEC;

		int next = 5;
		next = getServiceStatsParam(params, next, serviceProviders.keySet(),
				COMPO_STAT_PROVIDERS);
		next = getServiceStatsParam(params, next, searchSuccess.keySet(),
				COMPO_STAT_SUC_SEARCH);
		next = getServiceStatsParam(params, next, invokeSuccess.keySet(),
				COMPO_STAT_SUC_INVOKE);

		next = getMapParams(params, next, bindDurations, COMPO_STAT_DUR_BIND);
		next = getMapParams(params, next, forwardToBindDurations,
				COMPO_STAT_DUR_FWD_TO_BIND);
		next = getMapParams(params, next, forwardToExecDurations,
				COMPO_STAT_DUR_FWD_TO_EXEC);

		next = getServiceStatsParam(params, next,
				compositionDurations.keySet(), COMPO_STAT_DUR_COMPO);

		return params;
	}

	public ExtendedProperties getStats() {
		determineCompositionDurations(bindTimes, forwardToExecTimes);
		determineBindDurations(bindTimes);
		determineForwardToBindDurations(bindTimes);
		determineForwardToExecDurations(forwardToExecTimes);

		ExtendedProperties s = new ExtendedProperties();

		s.put(COMPO_STAT_REQ, Integer.toString(numReq));
		// s.put(COMPO_STAT_PROVIDERS_REPEAT, providersRepeat);

		s.put(WF_STAT_LAST_BOUND, lastServiceBound);
		s.put(WF_STAT_LAST_EXEC, lastServiceExecuted);
		s.put(WF_STAT_I_KNOWS_LAST_BOUND, iKnowsLastServiceBound);
		s.put(WF_STAT_I_KNOWS_LAST_EXEC, iKnowsLastServiceExecuted);

		getServiceProviderStats(s, COMPO_STAT_PROVIDERS);
		getServiceCountStats(searchSuccess, s, COMPO_STAT_SUC_SEARCH);
		getServiceCountStats(invokeSuccess, s, COMPO_STAT_SUC_INVOKE);

		getMapStats(s, COMPO_STAT_DUR_BIND, bindDurations);
		getMapStats(s, COMPO_STAT_DUR_FWD_TO_BIND, forwardToBindDurations);
		getMapStats(s, COMPO_STAT_DUR_FWD_TO_EXEC, forwardToExecDurations);

		getCompositionDurations(s, COMPO_STAT_DUR_COMPO);

		return s;
	}

	private int countMapEntries(HashMap<String, HashMap<String, Long>> map) {
		int count = 0;

		Set<String> entries = map.keySet();
		Iterator<String> it = entries.iterator();
		while (it.hasNext()) {
			String entryKey = it.next();
			HashMap<String, Long> submap = map.get(entryKey);
			count = count + submap.size();
		}

		return count;
	}

	private int getMapParams(String[] params, int index,
			HashMap<String, HashMap<String, Long>> map, String prefix) {
		Set<String> mapKeys = map.keySet();
		Iterator<String> it = mapKeys.iterator();
		while (it.hasNext()) {
			String mapKey = it.next();
			HashMap<String, Long> submap = map.get(mapKey);
			Set<String> submapKeys = submap.keySet();
			Iterator<String> sit = submapKeys.iterator();
			while (sit.hasNext()) {
				String submapKey = sit.next();
				params[index] = prefix + "." + mapKey + "." + submapKey;
				index++;
			}
		}

		return index;
	}

	private int getServiceStatsParam(String[] params, int index,
			Set<String> keys, String prefix) {
		Iterator<String> iit = keys.iterator();
		while (iit.hasNext()) {
			params[index] = prefix + "." + iit.next();
			index++;
		}
		return index;
	}

	private void getCompositionDurations(ExtendedProperties properties,
			String prefix) {
		Set<String> keys = compositionDurations.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String key = it.next();
			properties.put(prefix + "." + key,
					Long.toString(compositionDurations.get(key)));
		}
	}

	private void getMapStats(ExtendedProperties properties, String prefix,
			HashMap<String, HashMap<String, Long>> map) {
		Set<String> mapKeys = map.keySet();
		Iterator<String> it = mapKeys.iterator();
		while (it.hasNext()) {
			String mapKey = it.next();
			HashMap<String, Long> submap = map.get(mapKey);
			Set<String> submapKeys = submap.keySet();
			Iterator<String> sit = submapKeys.iterator();
			while (sit.hasNext()) {
				String submapKey = sit.next();
				properties.put(prefix + "." + mapKey + "." + submapKey,
						Long.toString(submap.get(submapKey)));

			}
		}
	}

	private void determineCompositionDurations(
			HashMap<String, HashMap<String, List<Long>>> mapBindTimes,
			HashMap<String, HashMap<String, List<Long>>> mapFwdToExecTimes) {
		// for A and every message in bindTimes, get start
		// for dst and every message in forwardToExec get end
		Long compositionStart = null;
		Long compositionEnd = null;
		Long compositionDuration = null;

		HashMap<String, List<Long>> startEntry = mapBindTimes.get("A");
		if (startEntry != null) {
			Set<String> msgKeys = startEntry.keySet();
			Iterator<String> it = msgKeys.iterator();
			while (it.hasNext()) {
				String msdId = it.next();
				List<Long> bindStartEndTime = startEntry.get(msdId);
				if (bindStartEndTime != null && bindStartEndTime.get(0) != null) {
					compositionStart = bindStartEndTime.get(0);
					HashMap<String, List<Long>> endEntry = mapFwdToExecTimes
							.get("dst");
					if (endEntry != null) {
						List<Long> forwToExecStartEndTime = endEntry.get(msdId);
						if (forwToExecStartEndTime != null
								&& forwToExecStartEndTime.get(1) != null) {
							compositionEnd = forwToExecStartEndTime.get(1);
							compositionDuration = (compositionEnd - compositionStart)
									/ DUR_UNIT;
							compositionDurations
									.put(msdId, compositionDuration);
						}
					}
				}

				compositionStart = null;
				compositionEnd = null;
				compositionDuration = null;
			}
		}
	}

	private void determineBindDurations(
			HashMap<String, HashMap<String, List<Long>>> mapBindTimes) {
		Long bindDuration = null;

		Set<String> serviceKeys = mapBindTimes.keySet();
		Iterator<String> it = serviceKeys.iterator();
		while (it.hasNext()) {
			String service = it.next();
			HashMap<String, List<Long>> bindEntries = mapBindTimes.get(service);
			Set<String> msgKeys = bindEntries.keySet();
			Iterator<String> mit = msgKeys.iterator();
			while (mit.hasNext()) {
				String message = mit.next();
				List<Long> msgBindTime = bindEntries.get(message);
				if (msgBindTime != null && msgBindTime.get(0) != null
						&& msgBindTime.get(1) != null) {
					bindDuration = (msgBindTime.get(1) - msgBindTime.get(0))
							/ DUR_UNIT;
					HashMap<String, Long> msgBindDuration = new HashMap<String, Long>();
					msgBindDuration.put(message, bindDuration);
					bindDurations.put(service, msgBindDuration);
				}
				bindDuration = null;
			}
		}
	}

	private void determineForwardToBindDurations(
			HashMap<String, HashMap<String, List<Long>>> mapBindTimes) {
		Long bindEndThis = null;
		Long bindStartNext = null;
		Long forwardDuration = null;

		Set<String> serviceKeys = mapBindTimes.keySet();
		Iterator<String> it = serviceKeys.iterator();
		while (it.hasNext()) {
			String thisService = it.next();
			HashMap<String, List<Long>> thisBindEntries = mapBindTimes
					.get(thisService);
			Set<String> msgKeys = thisBindEntries.keySet();
			Iterator<String> mit = msgKeys.iterator();
			while (mit.hasNext()) {
				String message = mit.next();
				List<Long> thisMsgBindTime = thisBindEntries.get(message);
				if (thisMsgBindTime != null && thisMsgBindTime.get(0) != null
						&& thisMsgBindTime.get(1) != null) {
					bindEndThis = thisMsgBindTime.get(1);
					// get bindStartNext
					String nextService = getNextService(thisService);
					HashMap<String, List<Long>> nextBindEntries = mapBindTimes
							.get(nextService);
					if (nextBindEntries != null) {
						List<Long> nextMsgBindTime = nextBindEntries
								.get(message);
						if (nextMsgBindTime != null
								&& nextMsgBindTime.get(0) != null) {
							bindStartNext = nextMsgBindTime.get(0);
							forwardDuration = (bindStartNext - bindEndThis)
									/ DUR_UNIT;
							HashMap<String, Long> msgForwardDuration = new HashMap<String, Long>();
							msgForwardDuration.put(message, forwardDuration);
							forwardToBindDurations.put(thisService,
									msgForwardDuration);
						}
					}
				}
				bindEndThis = null;
				bindStartNext = null;
				forwardDuration = null;
			}
		}
	}

	private void determineForwardToExecDurations(
			HashMap<String, HashMap<String, List<Long>>> mapFwdToExecTimes) {
		Long forwardDuration = null;

		Set<String> serviceKeys = mapFwdToExecTimes.keySet();
		Iterator<String> it = serviceKeys.iterator();
		while (it.hasNext()) {
			String service = it.next();
			HashMap<String, List<Long>> forwardEntries = mapFwdToExecTimes
					.get(service);
			Set<String> msgKeys = forwardEntries.keySet();
			Iterator<String> mit = msgKeys.iterator();
			while (mit.hasNext()) {
				String message = mit.next();
				List<Long> msgFwdTime = forwardEntries.get(message);
				if (msgFwdTime != null && msgFwdTime.get(0) != null
						&& msgFwdTime.get(1) != null) {
					forwardDuration = (msgFwdTime.get(1) - msgFwdTime.get(0))
							/ DUR_UNIT;
					HashMap<String, Long> msgFwdDuration = new HashMap<String, Long>();
					msgFwdDuration.put(message, forwardDuration);
					forwardToExecDurations.put(service, msgFwdDuration);
				}
				forwardDuration = null;
			}
		}
	}

	public String getNextService(String thisService) {
		if (thisService.equals(lastService)) {
			return "dst";
		} else if (thisService.equals("dst")) {
			return "dst";
		} else {
			char next = thisService.charAt(0);
			next++;
			return Character.toString(next);
		}
	}

	public String getLastService() {
		return lastService;
	}

	public void setLastService(String lastService) {
		this.lastService = lastService;
	}

	// public synchronized void setProvidersRepeat(String providersRepeat) {
	// DucksCompositionStats.providersRepeat = providersRepeat;
	// }

	private void getServiceCountStats(HashMap<String, Integer> map,
			ExtendedProperties properties, String prefix) {
		Set<String> keys = map.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String key = it.next();
			properties.put(prefix + "." + key, Integer.toString(map.get(key)));
		}
	}

	private void getServiceProviderStats(ExtendedProperties properties,
			String prefix) {
		Set<String> msgIds = serviceProviders.keySet();
		Iterator<String> it = msgIds.iterator();
		String msgId;
		List<String> providers;
		String providerString = "";
		while (it.hasNext()) {
			msgId = it.next();
			providers = serviceProviders.get(msgId);
			Iterator<String> pit = providers.iterator();
			while (pit.hasNext()) {
				if (providerString.equals("")) {
					providerString = (String) pit.next();
				} else {
					providerString = providerString + "#" + (String) pit.next();
				}
			}
			properties.put(prefix + "." + msgId, providerString);
			providerString = "";
		}
	}

	public synchronized void incrementNumReq() {
		numReq++;
	}

	public synchronized void incrementSearchSuccess(String service) {
		incrementHashVal(service, searchSuccess);
	}

	public synchronized void incrementInvokeSuccess(String service) {
		incrementHashVal(service, invokeSuccess);
	}

	private synchronized void incrementHashVal(String key,
			HashMap<String, Integer> map) {
		if (map.containsKey(key)) {
			Object val = map.get(key);
			if (val == null) {
				map.remove(key);
				map.put(key, new Integer(1));
			} else {
				map.remove(key);
				map.put(key, ((Integer) val) + 1);
			}
		} else {
			map.put(key, new Integer(1));
		}
	}

	public void registerBindStartTime(String service, String msgId, Long time) {
		HashMap<String, List<Long>> entry = bindTimes.get(service);
		if (entry == null) {
			List<Long> list = new ArrayList<Long>();
			list.add(null);
			list.add(null);
			list.set(0, time);
			HashMap<String, List<Long>> newEntry = new HashMap<String, List<Long>>();
			newEntry.put(msgId, list);
			bindTimes.put(service, newEntry);
		}
	}

	public void registerBindEndTime(String service, String msgId, Long time) {
		HashMap<String, List<Long>> entry = bindTimes.get(service);
		if (entry != null) {
			List<Long> list = entry.get(msgId);
			if (list != null) {
				list.set(1, time);
			}
		}
	}

	public void registerForwardToExecStartTime(String service, String msgId,
			Long time) {
		HashMap<String, List<Long>> entry = forwardToExecTimes.get(service);
		if (entry == null) {
			List<Long> list = new ArrayList<Long>();
			list.add(null);
			list.add(null);
			list.set(0, time);
			HashMap<String, List<Long>> newEntry = new HashMap<String, List<Long>>();
			newEntry.put(msgId, list);
			forwardToExecTimes.put(service, newEntry);
		}
	}

	public void registerForwardToExecEndTime(String service, String msgId,
			Long time) {
		HashMap<String, List<Long>> entry = forwardToExecTimes.get(service);
		if (entry != null) {
			List<Long> list = entry.get(msgId);
			if (list != null) {
				list.set(1, time);
			}
		}
	}

	public synchronized void addServiceProvider(String msgId, String providerId) {
		List<String> providers = serviceProviders.get(msgId);
		if (providers == null) {
			providers = new ArrayList<String>();
			serviceProviders.put(msgId, providers);
		}
		providers.add(providerId);
	}

	public synchronized List<String> getServiceProvidersForMessage(String msgId) {
		return serviceProviders.get(msgId);
	}

	public synchronized boolean providerBoundToCompositionRequest(String msgId,
			String providerId) {
		List<String> providers = serviceProviders.get(msgId);
		if (providers == null) {
			return false;
		} else {
			return providers.contains(providerId);
		}
	}

	public synchronized String getLastServiceBound() {
		return lastServiceBound;
	}

	public synchronized void setLastServiceBound(String lastServiceBound) {
		DucksCompositionStats.lastServiceBound = lastServiceBound;
	}

	public synchronized String getLastServiceExecuted() {
		return lastServiceExecuted;
	}

	public synchronized void setLastServiceExecuted(String lastServiceExecuted) {
		DucksCompositionStats.lastServiceExecuted = lastServiceExecuted;
	}

	public synchronized String getiKnowsLastServiceBound() {
		return iKnowsLastServiceBound;
	}

	public synchronized void setiKnowsLastServiceBound(
			String iKnowsLastServiceBound) {
		DucksCompositionStats.iKnowsLastServiceBound = iKnowsLastServiceBound;
	}

	public synchronized String getiKnowsLastServiceExecuted() {
		return iKnowsLastServiceExecuted;
	}

	public synchronized void setiKnowsLastServiceExecuted(
			String iKnowsLastServiceExecuted) {
		DucksCompositionStats.iKnowsLastServiceExecuted = iKnowsLastServiceExecuted;
	}

}
