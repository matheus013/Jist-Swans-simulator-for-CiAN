//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoiseAdditive.java Tue 2004/04/13 18:16:53 barr glenlivet.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

// Includes extensions by Ulm University
// - Bugfix: mode was set to "receive" in endTransmit, if a signal was pending

package jist.swans.radio;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.Main;
import jist.swans.mac.MacMessage.Data;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.net.NetMessage.Ip;
import ext.util.stats.DucksRadioNoiseAdditiveStats;

/**
 * <code>RadioNoiseAdditive</code> implements a radio with an additive noise
 * model.
 * 
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoiseAdditive.java,v 1.26 2004-11-19 15:55:34 barr Exp $
 * @since SWANS1.0
 */

public class RadioNoiseAdditive extends RadioNoise {
	// ////////////////////////////////////////////////
	// constants
	//

	/** signal-to-noise error model constant. */
	public static final byte SNR = 0;

	/** bit-error-rate error model constant. */
	public static final byte BER = 1;

	// ////////////////////////////////////////////////
	// locals
	//

	//
	// properties
	//

	/**
	 * radio type: SNR or BER.
	 */
	protected byte type;

	/**
	 * threshold signal-to-noise ratio (mW)
	 * <code>SNR = signal power / noise power</code>
	 */
	protected float thresholdSNR;

	/**
	 * bit-error-rate table.
	 */
	protected BERTable ber;

	//
	// state
	//

	/**
	 * total signal power.
	 */
	protected double totalPower_mW;

	/**
	 * stats
	 */
	DucksRadioNoiseAdditiveStats stats;

	// ////////////////////////////////////////////////
	// initialize
	//

	/**
	 * Create new radio with additive noise model.
	 * 
	 * @param id
	 *            radio identifier
	 * @param shared
	 *            shared radio properties
	 */
	public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared) {
		this(id, shared, (float) Constants.SNR_THRESHOLD_DEFAULT);
	}

	/**
	 * Create a new radio with additive noise model.
	 * 
	 * @param id
	 *            radio identifier
	 * @param shared
	 *            shared radio properties
	 * @param snrThreshold_mW
	 *            threshold signal-to-noise ratio
	 */
	public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared,
			float snrThreshold_mW) {
		super(id, shared);
		this.type = SNR;
		this.thresholdSNR = snrThreshold_mW;
		totalPower_mW = radioInfo.shared.background_mW;
		if (totalPower_mW > radioInfo.shared.sensitivity_mW)
			mode = Constants.RADIO_MODE_SENSING;
	}

	/**
	 * Create a new radio with additive noise model.
	 * 
	 * @param id
	 *            radio identifier
	 * @param shared
	 *            shared radio properties
	 * @param ber
	 *            bit-error-rate table
	 */
	public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared,
			BERTable ber) {
		super(id, shared);
		this.type = BER;
		this.ber = ber;
		totalPower_mW = radioInfo.shared.background_mW;
		if (totalPower_mW > radioInfo.shared.sensitivity_mW)
			mode = Constants.RADIO_MODE_SENSING;
	}

	// ////////////////////////////////////////////////
	// accessors
	//

	/**
	 * Register a bit-error-rate table.
	 * 
	 * @param ber
	 *            bit-error-rate table
	 */
	public void setBERTable(BERTable ber) {
		this.ber = ber;
	}

	/**
	 * Sets ducks stats collector for this radio noise
	 * 
	 * @param stats
	 */
	public void setStats(DucksRadioNoiseAdditiveStats stats) {
		this.stats = stats;
	}

	// ////////////////////////////////////////////////
	// reception
	//

	// RadioInterface interface
	/** {@inheritDoc} */
	public void receive(final Message msg, final Double powerObj_mW,
			final Long durationObj) {
		final double power_mW = powerObj_mW.doubleValue();
		final long duration = durationObj.longValue();

		// DEBUG: Christin added this for remote debugging and for logging
		if (msg != null) {
			if (msg.getClass().getName()
					.equals("jist.swans.mac.MacMessage$Data")) {
				Data datamsg = (Data) msg;
				if (datamsg.getBody().getClass().getName()
						.equals("jist.swans.net.NetMessage$Ip")) {
					Ip ipmsg = (Ip) datamsg.getBody();
					if (ipmsg.getPayload().getClass().getName()
							.equals("ext.jist.swans.app.DiscoveryRequest")) {
						stats.incAppDiscReqSensed();
					}
				}
			}
		}

		switch (mode) {
		case Constants.RADIO_MODE_IDLE:
			// if power greater than general reception threshold and signal
			// noise gap is big enough
			// try to recevie the packet ( SNR = Signalpower / Noisepower )
			if (power_mW >= radioInfo.shared.threshold_mW
					&& power_mW >= totalPower_mW * thresholdSNR) {
				lockSignal(msg, power_mW, duration);
				setMode(Constants.RADIO_MODE_RECEIVING);
			}
			// otherwise if new noise level is bigger than sensing threshold set
			// channel busy
			else if (totalPower_mW + power_mW > radioInfo.shared.sensitivity_mW) {
				setMode(Constants.RADIO_MODE_SENSING);
				// DEBUG: Christin added this for remote debugging and logging
				if (msg != null) {
					if (msg.getClass().getName()
							.equals("jist.swans.mac.MacMessage$Data")) {
						Data datamsg = (Data) msg;
						if (datamsg.getBody().getClass().getName()
								.equals("jist.swans.net.NetMessage$Ip")) {
							Ip ipmsg = (Ip) datamsg.getBody();
							if (ipmsg
									.getPayload()
									.getClass()
									.getName()
									.equals("ext.jist.swans.app.DiscoveryRequest")) {
								stats.incAppDiscReqLostWeak();
							}
						}
					}
				}
			}
			break;
		case Constants.RADIO_MODE_SENSING:
			if (power_mW >= radioInfo.shared.threshold_mW
					&& power_mW >= totalPower_mW * thresholdSNR) {
				lockSignal(msg, power_mW, duration);
				setMode(Constants.RADIO_MODE_RECEIVING);
			} else {
				// DEBUG: Christin added this for remote debugging and logging
				if (msg != null) {
					if (msg.getClass().getName()
							.equals("jist.swans.mac.MacMessage$Data")) {
						Data datamsg = (Data) msg;
						if (datamsg.getBody().getClass().getName()
								.equals("jist.swans.net.NetMessage$Ip")) {
							Ip ipmsg = (Ip) datamsg.getBody();
							if (ipmsg
									.getPayload()
									.getClass()
									.getName()
									.equals("ext.jist.swans.app.DiscoveryRequest")) {
								stats.incAppDiscReqLostWeak();
							}
						}
					}
				}
			}
			break;
		case Constants.RADIO_MODE_RECEIVING:
			// if the new signal is strong enough to be received ...
			if (power_mW > signalPower_mW
					&& power_mW >= totalPower_mW * thresholdSNR) {
				// DEBUG: Christin added this for remote debugging and logging
				if (signalBuffer != null) {
					if (signalBuffer.getClass().getName()
							.equals("jist.swans.mac.MacMessage$Data")) {
						Data datamsg = (Data) signalBuffer;
						if (datamsg.getBody().getClass().getName()
								.equals("jist.swans.net.NetMessage$Ip")) {
							Ip ipmsg = (Ip) datamsg.getBody();
							if (ipmsg
									.getPayload()
									.getClass()
									.getName()
									.equals("ext.jist.swans.app.DiscoveryRequest")) {
								stats.incAppDiscReqLostColl();
							}
						}
					}
				}

				// ... check if we should recognize the signal as a packet ...
				if (radioInfo.shared.captureStrongerLast) { // ... yes ->
					// receive it
					lockSignal(msg, power_mW, duration);
					setMode(Constants.RADIO_MODE_RECEIVING);
				} else { // ... no -> through away both
					unlockSignal();
					setMode(Constants.RADIO_MODE_SENSING);
				}
			} else {
				// probably "type == SNR &&" should be removed here because if a
				// collision occures a correct
				// packet reception is impossible in any case - Manuel Schoch
				// TODO Manuel Schoch: check if we should use a capture
				// threshold instead of SNR here
				if (type == SNR
						&& signalPower_mW < (totalPower_mW - signalPower_mW + power_mW)
								* thresholdSNR) {
					// DEBUG: Christin added this for remote debugging and
					// logging
					if (signalBuffer != null) {
						if (signalBuffer.getClass().getName()
								.equals("jist.swans.mac.MacMessage$Data")) {
							Data datamsg = (Data) signalBuffer;
							if (datamsg.getBody().getClass().getName()
									.equals("jist.swans.net.NetMessage$Ip")) {
								Ip ipmsg = (Ip) datamsg.getBody();
								if (ipmsg
										.getPayload()
										.getClass()
										.getName()
										.equals("ext.jist.swans.app.DiscoveryRequest")) {
									stats.incAppDiscReqLostColl();
								}
							}
						}
					}
					// DEBUG: Christin added this for remote debugging and
					// logging
					if (msg != null) {
						if (msg.getClass().getName()
								.equals("jist.swans.mac.MacMessage$Data")) {
							Data datamsg = (Data) msg;
							if (datamsg.getBody().getClass().getName()
									.equals("jist.swans.net.NetMessage$Ip")) {
								Ip ipmsg = (Ip) datamsg.getBody();
								if (ipmsg
										.getPayload()
										.getClass()
										.getName()
										.equals("ext.jist.swans.app.DiscoveryRequest")) {
									stats.incAppDiscReqLostColl();
								}
							}
						}
					}
					unlockSignal();
					setMode(Constants.RADIO_MODE_SENSING);
				}
			}
			break;
		case Constants.RADIO_MODE_TRANSMITTING:
			// DEBUG: Christin added this for remote debugging and logging
			if (msg != null) {
				if (msg.getClass().getName()
						.equals("jist.swans.mac.MacMessage$Data")) {
					Data datamsg = (Data) msg;
					if (datamsg.getBody().getClass().getName()
							.equals("jist.swans.net.NetMessage$Ip")) {
						Ip ipmsg = (Ip) datamsg.getBody();
						if (ipmsg.getPayload().getClass().getName()
								.equals("ext.jist.swans.app.DiscoveryRequest")) {
							stats.incAppDiscReqLostTran();
						}
					}
				}
			}
			break;
		case Constants.RADIO_MODE_SLEEP:
			// DEBUG: Christin added this for remote debugging and logging
			if (msg != null) {
				if (msg.getClass().getName()
						.equals("jist.swans.mac.MacMessage$Data")) {
					Data datamsg = (Data) msg;
					if (datamsg.getBody().getClass().getName()
							.equals("jist.swans.net.NetMessage$Ip")) {
						Ip ipmsg = (Ip) datamsg.getBody();
						if (ipmsg.getPayload().getClass().getName()
								.equals("ext.jist.swans.app.DiscoveryRequest")) {
							stats.incAppDiscReqLostSlee();
						}
					}
				}
			}
			break;
		default:
			throw new RuntimeException("unknown radio mode");
		}
		// cumulative signal
		signals++;
		totalPower_mW += power_mW;
		// schedule an endReceive
		JistAPI.sleep(duration);
		self.endReceive(powerObj_mW);
	} // function: receive

	// RadioInterface interface
	/** {@inheritDoc} */
	public void endReceive(Double powerObj_mW) {
		final double power_mW = powerObj_mW.doubleValue();
		// cumulative signal
		signals--;
		if (Main.ASSERT)
			Util.assertion(signals >= 0);
		totalPower_mW = signals == 0 ? radioInfo.shared.background_mW
				: totalPower_mW - power_mW;
		switch (mode) {
		case Constants.RADIO_MODE_RECEIVING:
			if (JistAPI.getTime() == signalFinish) {
				boolean dropped = false;
				dropped |= type == BER
						&& totalPower_mW > 0
						&& ber.shouldDrop(signalPower_mW / totalPower_mW,
								8 * signalBuffer.getSize());

				if (!dropped && signalBuffer != null) {
					this.macEntity.receive(signalBuffer);
				}
				unlockSignal();
				setMode(totalPower_mW >= radioInfo.shared.sensitivity_mW ? Constants.RADIO_MODE_SENSING
						: Constants.RADIO_MODE_IDLE);
			}
			break;
		case Constants.RADIO_MODE_SENSING:
			if (totalPower_mW < radioInfo.shared.sensitivity_mW)
				setMode(Constants.RADIO_MODE_IDLE);
			break;
		case Constants.RADIO_MODE_TRANSMITTING:
			break;
		case Constants.RADIO_MODE_IDLE:
			break;
		case Constants.RADIO_MODE_SLEEP:
			break;
		default:
			throw new RuntimeException("unknown radio mode");
		}
	} // function: endReceive

	// Elmar Schoch
	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	// overwrite inherited method, since there seems to be an error in the
	// original implementation:
	// When the radio has signals after transmitting, it should not go into
	// receiving mode, but
	// into sensing mode.
	//
	// RadioInterface interface
	/** {@inheritDoc} */
	public void endTransmit() {
		// radio in sleep mode
		if (mode == Constants.RADIO_MODE_SLEEP)
			return;
		// check that we are currently transmitting
		if (mode != Constants.RADIO_MODE_TRANSMITTING)
			throw new RuntimeException("radio is not transmitting");
		// set mode
		setMode(totalPower_mW >= radioInfo.shared.sensitivity_mW ? Constants.RADIO_MODE_SENSING
				: Constants.RADIO_MODE_IDLE);
	}

	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

} // class: RadioNoiseAdditive

