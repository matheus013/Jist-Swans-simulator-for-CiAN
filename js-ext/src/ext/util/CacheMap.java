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

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * A CacheMap is a bounded map data structure that automatically replaces the
 * eldest element in insertion order (or access order) when the addition of a
 * new element exceeds the maximum capacity of the map.
 * One could also call this a CircularMap
 *
 * @author 	Michael Feiri
 */
public class CacheMap extends LinkedHashMap {
    
    private static final float defaultLoadFactor = 0.75f; //same as LinkedHashMap
    private int maxEntries;
    
    public CacheMap(int capacity) {
        super((int)(capacity/defaultLoadFactor)+1,defaultLoadFactor);
        maxEntries=capacity;
    }
    public CacheMap(int capacity, float loadFactor) {
        super((int)(capacity/loadFactor)+1,loadFactor);
        maxEntries=capacity;
    }
    public CacheMap(int capacity, float loadFactor, boolean accessOrder) {
        super((int)(capacity/loadFactor)+1,loadFactor,accessOrder);
        maxEntries=capacity;
    }
    public CacheMap(Map m) {
        super(m);
        maxEntries=m.size();
    }
    
    public Object put(Object key, Object value) {
        // overwriting an existing entry must be a structural operation in order
        // to preserve insertion order! IMHO this is a bug in LinkedHashMap.
        if (containsKey(key)) {
            Object previousEntry = remove(key);
            super.put(key, value);
            return previousEntry;
        } else {
            return super.put(key, value);
        }
    }
    
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxEntries;
    }
    
    public int capacity() {
        return maxEntries;
    }
    
    public boolean isFull() {
        return size() == maxEntries;
    }
    
}
