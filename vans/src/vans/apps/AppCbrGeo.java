/*
 * Ulm University JiST/SWANS project
 * 
 * Author:		Michael Feiri <michael.feiri@uni-ulm.de>
 * 
 * (C) Copyright 2006, Ulm University, all rights reserved.
 * 
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 */
package vans.apps;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Field.RadioData;
import jist.swans.field.Placement;
import jist.swans.misc.Location;
import jist.swans.net.NetAddress;
import vans.net.NetAddressGeo;
import vans.straw.StreetMobility;
import vans.straw.streets.RoadSegment;
import ext.jist.swans.app.AppCbrBase;
import ext.util.ExtendedProperties;
import ext.util.Region;

public class AppCbrGeo extends AppCbrBase {

	private static final String CBR_STAT_DIST = "ducks.app.cbr.distance";
	private static float distanceSum;

	private StreetMobility sm = null;
	private Placement place = null;
	private RadioData localRadio = null;
	private Location fieldSize = null;

	public AppCbrGeo(int sendRate, int waitTimeBetween, int waitTimeStart,
			int waitTimeEnd, int packetsPerConnection, int nodeId,
			int nodeCount, int duration, RadioData radio, Placement place,
			Location f) {

		super(sendRate, waitTimeBetween, waitTimeStart, waitTimeEnd,
				packetsPerConnection, nodeId, nodeCount, duration);
		localRadio = radio;
		this.place = place;
		this.fieldSize = f;
	}

	public AppCbrGeo(int sendRate, int waitTimeBetween, int waitTimeStart,
			int waitTimeEnd, int packetsPerConnection, int nodeId,
			int nodeCount, int duration, RadioData radio, StreetMobility sm) {

		super(sendRate, waitTimeBetween, waitTimeStart, waitTimeEnd,
				packetsPerConnection, nodeId, nodeCount, duration);
		localRadio = radio;
		this.sm = sm;
	}

	/**
	 * 
	 * 
	 */
	protected void beginMessageSequence() {

		Region dest = null;

		int minSqm;
		int maxSqm;

		if (place != null) {

			/*
			 * float minf = (float)StrictMath.sqrt(100*100*StrictMath.PI); //
			 * minimum radius float deltaf =
			 * (float)StrictMath.sqrt(300*300*StrictMath.PI)-minf; float f =
			 * ((Constants.random.nextFloat()*deltaf)/2 + minf)/2;
			 * 
			 * Location.Location2D target =
			 * (Location.Location2D)place.getNextLocation(); Location.Location2D
			 * target_bl = new
			 * Location.Location2D(target.getX()-f,target.getY()-f);
			 * Location.Location2D target_tr = new
			 * Location.Location2D(target.getX()+f,target.getY()+f); dest = new
			 * Region.Rectangle(target_bl,target_tr);
			 */

			Location.Location2D target = (Location.Location2D) place
					.getNextLocation();
			float radius = Constants.random.nextInt(200) + 100;
			dest = new Region.Circle(target, radius);
			distanceSum += (dest.distance(localRadio.getLoc()) - radius);

			/*
			 * Location.Location2D target; float radius =
			 * Constants.random.nextInt(200)+100; do{ target =
			 * (Location.Location2D)place.getNextLocation(); } while (
			 * (target.getX()<radius) || (target.getY()<radius) ||
			 * (target.getX() > (fieldSize.getX()-radius)) || (target.getY() >
			 * (fieldSize.getY()-radius)) ); dest = new
			 * Region.Circle(target,radius);
			 */

		} else if (sm != null) {
			// TODO vans: allow fine grained constraints e.g.: minimum distance
			// and regions beyond RoadSegments. It would be best to refactor
			// StreetPlacementRandom and use it here!
			RoadSegment rs = (RoadSegment) sm.getSegments().get(
					Constants.random.nextInt(sm.getSegments().size()));
			// dest = new Region.Rectangle((Location.Location2D) rs
			// .getStartPoint(), (Location.Location2D) rs.getEndPoint());

			float radius = Constants.random.nextInt(200) + 100;
			dest = new Region.Circle((Location.Location2D) rs.getStartPoint(),
					radius);

			distanceSum += dest.distance(localRadio.getLoc()) - radius;

		}

		// GEOANYCAST
		NetAddress addr = new NetAddressGeo(dest, NetAddress.ANY);

		// GEOBROADCAST
		// NetAddress addr = new NetAddressGeo(dest, NetAddress.NULL);

		// GEO[UNI|MULTI]CAST
		// int dst = this.nodeId;
		// while (dst == this.nodeId) {
		// dst = Constants.random.nextInt(this.nodeCount)+1;
		// }
		// NetAddress addr = new NetAddressGeo(dest, dst);

		// calc wait time between messages
		long delay = Constants.MINUTE / this.sendRate;

		for (int i = 0; i < this.packetsPerConnection; i++) {
			// create message
			AppCbrBase.MessageCbr msg = new AppCbrBase.MessageCbr();
			msg.sentTime = JistAPI.getTime();
			msg.msgID = sentPackets + 1;

			// System.out.println(JistAPI.getTime() + ": " + this.nodeId
			// + " about to send " + this.packetsPerConnection + " to " + dest);

			// send message
			netEntity.send(msg, addr, Constants.NET_PROTOCOL_UDP,
					Constants.NET_PRIORITY_NORMAL, CBR_MESSAGE_TTL);

			// update stats
			sentPackets++;
			// distanceSum += dest.distance(localRadio.getLoc());

			// wait delay
			JistAPI.sleep(delay);
		}

	}

	// implementations of StatsCollector interface ....................
	public ExtendedProperties getStats() {

		ExtendedProperties stats = super.getStats();
		float avgDistance = sentPackets == 0 ? 0 : distanceSum / sentPackets;
		stats.put(CBR_STAT_DIST, Float.toString(avgDistance));

		return stats;
	}

	public String[] getStatParams() {

		String[] supi = super.getStatParams();
		String[] arra = new String[supi.length + 1];

		System.arraycopy(supi, 0, arra, 0, supi.length - 1);
		arra[supi.length] = CBR_STAT_DIST;
		return arra;
	}

}
