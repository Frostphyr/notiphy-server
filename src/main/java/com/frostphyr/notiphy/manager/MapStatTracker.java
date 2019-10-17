package com.frostphyr.notiphy.manager;

import java.util.Map;

public class MapStatTracker extends StatTracker {
	
	private Map<?, ?> Map;
	
	public MapStatTracker(String name, Map<?, ?> Map) {
		super(name);
		
		this.Map = Map;
	}

	@Override
	protected int get() {
		return Map.size();
	}

}
