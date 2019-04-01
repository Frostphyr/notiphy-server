package com.frostphyr.notiphy.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {
	
	public static <K, T> Set<T> getOrCreate(Map<K, Set<T>> map, K key) {
		Set<T> set = map.get(key);
		if (set == null) {
			set = new HashSet<>();
			map.put(key, set);
		}
		return set;
	}

}
