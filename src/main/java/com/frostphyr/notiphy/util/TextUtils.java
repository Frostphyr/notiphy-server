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

}
