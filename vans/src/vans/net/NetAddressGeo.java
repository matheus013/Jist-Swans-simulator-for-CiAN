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

package vans.net;

import java.net.InetAddress;

import jist.swans.net.NetAddress;
import ext.util.Region;

/**
 * A geographic network address.
 * 
 * Geographic addresses consist of a Region elemtent that specifies the
 * destination area and an InetAddress that indicated addressing modes. A real
 * implementation might store this additional data in an IPv4 option field.
 * Using this interface is simply a matter of convenience.
 * 
 * NetAddressGeo(r, NetAddress.NULL) -> Geobroadcast, Geocast NetAddressGeo(r,
 * NetAddress.ANY) -> Geoanycast NetAddressGeo(r, NetAddress(ip)) -> Geounicast
 * NetAddressGeo(r, NetAddress(ip)) -> Geomulticast (not implemented yet)
 * 
 * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
 */

public class NetAddressGeo extends NetAddress {

	/** geo address data */
	private final Region region;

	/**
	 * Create a new network address object which defaults to GEOANYCAST and does
	 * not include an IP address object
	 */
	public NetAddressGeo(Region r, NetAddress n) {
		super(n.getIP());
		this.region = r;
	}

	public NetAddressGeo(Region r, InetAddress ip) {
		super(ip);
		this.region = r;
	}

	public NetAddressGeo(Region r, byte[] addr) {
		super(addr);
		this.region = r;
	}

	public NetAddressGeo(Region r, int i) {
		super(i);
		this.region = r;
	}

	/** {@inheritDoc} */
	public int hashCode() {
		return region.hashCode() ^ getIP().hashCode();
	}

	/** {@inheritDoc} */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof NetAddressGeo))
			return false;
		NetAddressGeo nag = (NetAddressGeo) o;
		if (!this.getIP().equals(nag.getIP()))
			return false;
		if (!this.region.equals(nag.region))
			return false;
		return true;
	}

	/** {@inheritDoc} */
	public String toString() {
		return new String("IP:" + super.toString() + " Geo:"
				+ region.toString());
	}

	/** {@inheritDoc} */
	public int getSize() {
		return super.getSize() + region.getSize();
	}

	/**
	 * Return region information.
	 * 
	 * @return region information
	 */
	public Region getRegion() {
		return region;
	}

}
