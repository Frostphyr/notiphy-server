package com.frostphyr.notiphy.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {
	
	public static boolean contains(String text, List<String> phrases) {
		if (text == null) {
			return false;
		} else if (phrases != null) {
			for (String s : phrases) {
				if (!StringUtils.containsIgnoreCase(text, s)) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean contains(String[] text, List<String> phrases) {
		if (text == null) {
			return false;
		} else if (phrases != null) {
			for (String p : phrases) {
				boolean contains = false;
				for (String t : text) {
					if (t != null && StringUtils.containsIgnoreCase(t, p)) {
						contains = true;
					}
				}
				
				if (!contains) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean inRanges(char[][] ranges, String text) {
		for (int i = 0; i < text.length(); i++) {
			if (!inRanges(ranges, text.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean inRanges(char[][] ranges, char c) {
        for (char[] range : ranges) {
            if (range.length == 1) {
                if (c == range[0]) {
                    return true;
                }
            } else if (range.length == 2) {
                if (c >= range[0] && c <= range[1]) {
                    return true;
                }
            }
        }
        return false;
    }

}
