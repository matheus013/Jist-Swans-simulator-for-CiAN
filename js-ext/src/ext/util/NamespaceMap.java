package ext.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The NamespaceMap provides a hashmap to store named objects or
 * objects together with namespaces.
 * An exemplary usage would be to add certain objects under a
 * specific namespace. Later on, these objects can be retrieved
 * according to their class name and the namespace again.
 * 
 * The namespace is an arbitrary string. It can be used to file
 * objects of different classes in the same namespace, or even to
 * file single objects with a unique string identifier.
 * 
 * Caution: Do not add objects with the same class under the same
 * namespace, as they won't be accessible any more using the methods
 * of the NamespaceMap
 * 
 * @author eschoch
 *
 */
public class NamespaceMap extends HashMap {
	
	/**
	 * Put object with default namespace (= "")
	 * @param o Object to put
	 */
	public void put(Object o) {
		putWithNamespace("", o);
	}
	
	/**
	 * Put object with a specific namespace
	 * @param namespace
	 * @param o
	 */
	public void putWithNamespace(String namespace, Object o) {
		this.put(namespace, o);
	}
	
	/**
	 * Retrieve object of a certain class, using the default namespace 
	 * @param className
	 * @return
	 */
	public Object getByClassname(String className) {
		return this.getByClassname(className, "");
	}

	/**
	 * Retrieve object of a certain class, using a specific namespace.
	 * Note that the first object will be returned with the given
	 * parameters. 
	 * Caution: If more than 1 object of the same class is included
	 * under the same namespace, the return value will not be deterministic!
	 * 
	 * @param classname the class to look for
	 * @param namespace selected namespace
	 * @return corresponding object, or null if not found
	 */
	public Object getByClassname(String classname, String namespace) {
		Set s = this.entrySet();
		Iterator it = s.iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			Class objClass = entry.getValue().getClass();
			if (classname.equals( objClass.getSimpleName() ) &&
					namespace.equals(entry.getKey()) ) {
				return entry.getValue();
			}
		}
		
		return null; 
	}
}
