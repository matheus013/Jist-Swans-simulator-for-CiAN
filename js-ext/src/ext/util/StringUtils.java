/*
 * Ulm University DUCKS project
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
package ext.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import jist.runtime.JistAPI;
import jist.swans.Constants;

public class StringUtils {

// Date/time related .............................................
	
	private static SimpleDateFormat sdf = new SimpleDateFormat();
	
	public static String formatDateTime(Date date) {
		sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}
	
	public static String getDotted(long number) {
		String result = "";
		boolean addDot = false;
		while (number >= 1000) {
			if (addDot) result = "."+ result;
			result = (number % 1000) + result;
			number = number / 1000;
			
			addDot = true;
		}
		if (addDot) result = "." + result;
		result = number + result;
		return result;
	}
	
// JiST API related ...........................................
	
	public static String timeSeconds() {
		return timeSeconds(JistAPI.getTime());
	}
	
	public static String timeSeconds(long time) {
		return (time / Constants.SECOND) + "." + (time % Constants.SECOND);
	}
	
// String handling related ......................................
	
	public static List<String> getListFromString(String s, String separator) {
		if (s == null || separator == null) return null;
		
		List<String> l = new LinkedList<String>();
		String[] elements = s.split(separator);
		for(String el: elements) {
			l.add(el);
		}
		return l;
	}
	
	public static String getStringFromArray(String[] strings, String separator) {
		if(strings == null || separator == null) return null;
		
		String result = "";
		String sep = "";
		for(String s: strings) {
			System.out.println(s);
			result += sep + s;
			sep = separator;
		}
		return result;
	}
	
	/**
	 * Retrieve a string from a string array, with SPACE as default separator!
	 */
	public static String getStringFromArray(String[] s) {
		return getStringFromArray(s, " ");
	}

	/**
	 * Tests whether the given string starts with one of the strings in the
	 * array
	 * @param toTest the string to test
	 * @param possibilities the strings to test against
	 * @return
	 */
	public static boolean startsWith(String toTest, String[] possibilities) {
		for (String p: possibilities) {
			if (toTest.startsWith(p)) {
				return true;
			}
		}
		return false;
	}
}
