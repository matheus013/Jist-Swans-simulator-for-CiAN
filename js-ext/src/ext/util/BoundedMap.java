/*
 * Ulm University JiST/SWANS Extension Project
 * 
 * Author:		Michael Feiri <michael.feiri@uni-ulm.de>
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
package ext.util;

import java.util.Map;
import java.util.HashMap;

/**
 * A CacheMap is a bounded map data structure that silently drops put() requests
 * when the addition of a new element exceeds the maximum capacity of the map.
 * 
 * @author Michael Feiri
 */
public class BoundedMap extends HashMap {

	private static final float defaultLoadFactor = 0.75f; // same as HashMap
	private int maxEntries;

	public BoundedMap(int capacity) {
		super((int) (capacity / defaultLoadFactor) + 1, defaultLoadFactor);
		maxEntries = capacity;
	}

	public BoundedMap(int capacity, float loadFactor) {
		super((int) (capacity / loadFactor) + 1, loadFactor);
		maxEntries = capacity;
	}

	public BoundedMap(Map m) {
		super(m);
		maxEntries = m.size();
	}

	public Object put(Object key, Object value) {
		if (size() >= maxEntries) {
			return null;
		} else {
			return super.put(key, value);
		}
	}

	public int capacity() {
		return maxEntries;
	}

	public boolean isFull() {
		return size() == maxEntries;
	}

}
