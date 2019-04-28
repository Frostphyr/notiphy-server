package com.frostphyr.notiphy.util;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {
	
	public static boolean contains(String text, String[] phrases) {
		for (String s : phrases) {
			if (!StringUtils.containsIgnoreCase(text, s)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean contains(String[] text, String[] phrases) {
		for (String p : phrases) {
			boolean contains = false;
			for (String t : text) {
				if (t != null && StringUtils.containsIgnoreCase(t, p)) {
					contains = true;
				}
			}
			
			if (contains == false) {
				return false;
			}
		}
		return true;
	}

}
