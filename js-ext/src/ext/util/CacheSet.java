/*
 * Ulm University JiST/SWANS Extension Project
 * 
 * Author:		Michael Feiri <michael.feiri@uni-ulm.de>
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
package ext.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A CacheSet is a bounded set data structure that automatically
 * replaces the eldest element in insertion order when the addition
 * of a new element exceeds the maximum capacity of the map.
 * One could also call this a CacheQueue or a CircularList
 *
 * @author 	Michael Feiri
 */

public class CacheSet {

	private LinkedList ll = new LinkedList();
	private int capacity;
	
	public CacheSet(int cap) {
		capacity = cap;
	}
	public boolean offer(Object o) {
		if (ll.size()==capacity) ll.removeFirst();
		return ll.add(o);
	}
	public int capacity() {
		return capacity;
	}
	public Iterator iterator() {
		return new BackItr();
	}
	
    private class BackItr implements Iterator {
    	ListIterator it = ll.listIterator(ll.size());
    	public boolean hasNext() {
    		return it.hasPrevious();
    	}
    	public Object next() {
    		return it.previous();
    	}
    	public void remove() {
    		it.remove();
    	}
    }
    
}
