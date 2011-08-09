/*
 * Ulm University DUCKS project
 * 
 * Author: Elmar Schoch <elmar.schoch@uni-ulm.de>
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
package ext.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ReflectionUtils
{

    // Reflection utility methods ...................................

    public static Class getClass(String className) throws Exception {
        Class result = null;
        try {
            result = Class.forName(className);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        return result;
    }

    public static Object createObject(String className) throws Exception {
        Object result = null;
        try {
            result = Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new Exception("Class " + className + " could not be instantiated!");
        }
        return result;
    }

    public static Object createObject(String className, Class[] paramTypes, Object[] params) throws Exception {
        Object result = null;
        try {
            Class cls = Class.forName(className);
            Constructor con = cls.getConstructor(paramTypes);
            result = con.newInstance(params);
        } catch (Exception e) {
            throw new Exception("Class " + className + " could not be instantiated!");
        }
        return result;
    }

    public static Object invokeMethod(String className, String method, Class[] paramTypes, Object obj, Object[] args)
            throws Exception {

        Object result = null;
        try {
            Class cls = Class.forName(className);
            Method m = cls.getMethod(method, paramTypes);
            result = m.invoke(obj, args);
        } catch (Exception e) {
            throw new Exception("Method invoke failed: Class: " + className + " Method: " + method);
        }
        return result;
    }

}
