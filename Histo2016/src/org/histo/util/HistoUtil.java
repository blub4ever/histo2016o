package org.histo.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoUtil {

	/**
	 * Task a HashMap with key value pairs and replaces all entries within a string.
	 * 
	 * @param text
	 * @param replace
	 * @return
	 */
	public static final String replaceWildcardsInString(String text, HashMap<String, String> replace) {
		for (Map.Entry<String, String> entry : replace.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			text = text.replace(key, value);
		}
		return text;
	}

	/**
	 * Adds chars at the beginning of a string.
	 * 
	 * @param value
	 * @param len
	 * @param fitChar
	 * @return
	 */
	public final static String fitString(int value, int len, char fitChar) {
		return fitString(String.valueOf(value), len, fitChar);
	}

	/**
	 * Adds chars at the beginning of a string.
	 * 
	 * @param value
	 * @param len
	 * @param fitChar
	 * @return
	 */
	public final static String fitString(String value, int len, char fitChar) {
		StringBuilder str = new StringBuilder(value);
		while (str.length() < len) {
			str.insert(0, fitChar);
		}
		return str.toString();
	}

	public final static boolean isNullOrEmpty(String str) {
		return !isNotNullOrEmpty(str);
	}

	public final static boolean isNotNullOrEmpty(String str) {
		if (str != null && !str.isEmpty())
			return true;
		return false;
	}

	public final static boolean isNotNullOrEmpty(List<?> list) {
		if (list != null && !list.isEmpty())
			return true;
		return false;
	}

	public final static boolean isNotNullOrEmpty(Object[] arr) {
		if (arr != null && arr.length != 0)
			return true;
		return false;
	}

	public final static boolean isStringDifferent(Object arg1, Object arg2) {
		if (arg1 == arg2)
			return false;
		if (arg1 == null || arg2 == null)
			return true;
		if (arg1.equals(arg2))
			return false;
		return true;
	}
}
