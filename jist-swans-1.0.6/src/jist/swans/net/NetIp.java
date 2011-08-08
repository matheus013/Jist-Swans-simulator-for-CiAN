/////////////////////////////////////////////////////////////////
// JiST/SWANS extensions by UULM
//
// Note: This "new" NetIp class replaces the original NetIp class from JiST/SWANS,
// which was renamed to "NetIpBase" and made abstract to be able to derive
// new types of network layers from this base class.
//
// 
// Copyright (C) 2006 by Michael Feiri
//

package jist.swans.net;

import jist.swans.misc.Mapper;

/**
 * Concrete implementation of plain NetIP with Proxiable marker (through NetInterface)
   *
 * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
   */

public class NetIp extends NetIpBase implements NetInterface {
  public NetIp(NetAddress addr, Mapper protocolMap, PacketLoss in, PacketLoss out) {
    super(addr, protocolMap, in, out);
  }
}

