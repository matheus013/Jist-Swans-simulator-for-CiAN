/*
 * Ulm University JiST/SWANS project
 * 
 * Author:		Elmar Schoch <elmar.schoch@uni-ulm.de>
 * 
 * (C) Copyright 2006, Ulm University, all rights reserved.
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
package ext.jist.swans.net;

import java.util.Properties;

import ext.util.ExtendedProperties;
import ext.util.stats.StatsCollector;


import jist.swans.net.MessageQueue.NoDropMessageQueue;
import jist.swans.net.QueuedMessage;

/**
 * DropTailMessageQueue is an extension to SWANS which does not come with a
 * message queue that allows dropping messages in case of overload. Instead,
 * the SWANS NoDropMessageQueue throws an exception halting the complete simulation
 * which is not acceptable in many cases.
 * 
 * @author Elmar Schoch
 *
 */
public class DropTailMessageQueue extends NoDropMessageQueue implements StatsCollector {

	/**
	 * Count number of dropped messages
	 */
	private static long droppedMessages; 
	
	public DropTailMessageQueue(byte priorities, byte capacity) {
		super(priorities, capacity);
		
		droppedMessages = 0;
	}

	
	/**
	 * Insert message, and drop last message with lowest priority
	 */
	public void insert(QueuedMessage msg, int pri) {
      
		if(size==capacity) {
			// queue is full -> remove latest message (= head) of lowest
			// available prio
			int loprio = 0;
			while (loprio < heads.length && heads[loprio] == null) {loprio++; }
			
			QueuedMessage latest = heads[loprio];
			heads[loprio] = latest.next;
			
			droppedMessages++;
			
		} else {
			size++;
		}
		
		// insert new message
		topPri = (byte)StrictMath.min(pri, topPri);
		QueuedMessage tail = tails[pri];
		if(tail==null) {
			heads[pri] = msg;
			tails[pri] = msg;
		} else {
			tail.next = msg;
			tails[pri] = msg;
		}
	}
	
	
	public static long getDroppedMessages() {
		return droppedMessages;
	}


	
	public ExtendedProperties getStats() {
		ExtendedProperties stats = new ExtendedProperties();
		stats.put("ducks.mac.ifqueue.drops", Long.toString(droppedMessages));
		return stats;
	}


	public String[] getStatParams() {
		String[] stats = new String[1];
		stats[0] = "ducks.net.ifqueue.drops";
		return stats;
	}
	
}
