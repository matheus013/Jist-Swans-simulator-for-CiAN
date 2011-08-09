/*
 * Ulm University JiST/SWANS project
 * 
 * Author: Michael Feiri <michael.feiri@uni-ulm.de>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package ext.jist.runtime;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.TreeSet;

import jist.runtime.Event;
import jist.runtime.Scheduler;

/**
 * Alternate schedulers for JiST. In the literature you can find information
 * about this topic described as the "Pending Event Set" problem.
 * 
 * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
 */
public abstract class SchedulingQueue
{

    /**
     * An implementation of a Scheduler based on the default PriorityQueue in
     * Java 5.
     * 
     * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
     */
    public static final class PQScheduler extends Scheduler
    {

        private final PriorityQueue<Event> datastore;

        public PQScheduler() {
            datastore = new PriorityQueue<Event>(/*
                                                  * 11, new
                                                  * Event.EventComparator()
                                                  */);
        }

        public int size() {
            return datastore.size();
        }

        public boolean isEmpty() {
            return datastore.isEmpty();
        }

        public void insert(Event ev) {
            datastore.offer(ev);
        }

        public Event removeFirst() {
            return datastore.poll();
        }

        public Event peekFirst() {
            return datastore.peek();
        }

        public void clear() {
            datastore.clear();
        }

    }

    /**
     * A simple implementation of a Scheduler based on the TreeSet Collection
     * Class.
     * 
     * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
     */
    public static final class TMScheduler extends Scheduler
    {

        private final TreeSet<Event> datastore;

        public TMScheduler() {
            datastore = new TreeSet<Event>();
        }

        public int size() {
            return datastore.size();
        }

        public boolean isEmpty() {
            return datastore.isEmpty();
        }

        public void insert(Event ev) {
            datastore.add(ev);
        }

        public Event removeFirst() {
            // This would not return the exact same results as other
            // implementations
            // because we dont have a strict "total order" for Events
            // Event e = datastore.first();
            // datastore.remove(e);
            // return e;

            Iterator it = datastore.iterator();
            Event e = (Event) it.next();
            it.remove();
            return e;
        }

        public Event peekFirst() {
            // return datastore.first();
            return datastore.iterator().next();
        }

        public void clear() {
            datastore.clear();
        }

    }

    /*
     * public static final class PBScheduler extends Scheduler {
     * 
     * private final org.apache.commons.collections.buffer.PriorityBuffer
     * datastore;
     * 
     * public PBScheduler() { datastore = new
     * org.apache.commons.collections.buffer.PriorityBuffer(); }
     * 
     * public int size() { return datastore.size(); }
     * 
     * public boolean isEmpty() { return datastore.isEmpty(); }
     * 
     * public void insert(Event ev) { datastore.add(ev); }
     * 
     * public Event removeFirst() { return (Event)datastore.remove(); }
     * 
     * public Event peekFirst() { return (Event)datastore.get(); }
     * 
     * public void clear() { datastore.clear(); }
     * 
     * }
     */

    /**
     * An implementation of a Scheduler based on the public domain
     * implementation of a top-down splay tree by Daniel Sleator from
     * http://www.link.cs.cmu.edu/splay/
     * 
     * @author Michael Feiri &lt;michael.feiri@uni-ulm.de&gt;
     */
    static class BinaryNode
    {
        BinaryNode(Comparable theKey) {
            key = theKey;
            left = right = null;
        }

        Comparable key;  // The data in the node
        BinaryNode left; // Left child
        BinaryNode right; // Right child
    }

    public static class STScheduler extends Scheduler
    {
        private BinaryNode root;
        private int        size = 0;

        public STScheduler() {
            root = null;
        }

        public int size() {
            return size;
        }

        public void clear() {
            root = null;
        }

        public boolean isEmpty() {
            return root == null;
        }

        public void insert(Event key) {

            BinaryNode n;

            if (root == null) {
                root = new BinaryNode(key);
                return;
            }
            splay(key);
            n = new BinaryNode(key);
            if (key.compareTo(root.key) < 0) {
                n.left = root.left;
                n.right = root;
                root.left = null;
            } else {
                n.right = root.right;
                n.left = root;
                root.right = null;
            }
            root = n;
            size++;
        }

        /**
         * Remove from the tree.
         * 
         * @param x
         *            the item to remove.
         * @throws ItemNotFoundException
         *             if x is not found.
         */
        public Event removeFirst() {

            // find (and moves to root)
            Comparable y = peekFirst();
            if (y == null)
                return null;

            // remove it
            root = root.right;
            size--;

            return (Event) y;
        }

        /**
         * Find the smallest item in the tree.
         */
        public Event peekFirst() {
            BinaryNode x = root;
            if (root == null)
                return null;
            while (x.left != null)
                x = x.left;
            splay(x.key);
            return (Event) x.key;
        }

        private static BinaryNode header = new BinaryNode(null); // For splay

        /**
         * Internal method to perform a top-down splay.
         * 
         * splay(key) does the splay operation on the given key. If key is in
         * the tree, then the BinaryNode containing that key becomes the root.
         * If key is not in the tree, then after the splay, key.root is either
         * the greatest key < key in the tree, or the lest key > key in the
         * tree.
         * 
         * This means, among other things, that if you splay with a key that's
         * larger than any in the tree, the rightmost node of the tree becomes
         * the root. This property is used in the delete() method.
         */

        private void splay(Comparable key) {
            BinaryNode l, r, t, y;
            l = r = header;
            t = root;
            header.left = header.right = null;
            for (;;) {
                if (key.compareTo(t.key) < 0) {
                    if (t.left == null)
                        break;
                    if (key.compareTo(t.left.key) < 0) {
                        y = t.left; /* rotate right */
                        t.left = y.right;
                        y.right = t;
                        t = y;
                        if (t.left == null)
                            break;
                    }
                    r.left = t; /* link right */
                    r = t;
                    t = t.left;
                } else if (key.compareTo(t.key) >= 0) {
                    if (t.right == null)
                        break;
                    if (key.compareTo(t.right.key) >= 0) { // this may be left
                                                           // as ">"
                        y = t.right; /* rotate left */
                        t.right = y.left;
                        y.left = t;
                        t = y;
                        if (t.right == null)
                            break;
                    }
                    l.right = t; /* link left */
                    l = t;
                    t = t.right;
                } else {
                    break;
                }
            }
            l.right = t.left; /* assemble */
            r.left = t.right;
            t.left = header.right;
            t.right = header.left;
            root = t;
        }

    }

}
