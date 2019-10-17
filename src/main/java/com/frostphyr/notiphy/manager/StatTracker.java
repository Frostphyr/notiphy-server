package com.frostphyr.notiphy.manager;

public abstract class StatTracker {
	
	private String name;
	private int current;
	private int highest;
	
	public StatTracker(String name) {
		this.name = name;
	}
	
	public void update() {
		current = get();
		highest = Math.max(highest, current);
	}
	
	public String getName() {
		return name;
	}
	
	public int getCurrent() {
		return current;
	}
	
	public int getHighest() {
		return highest;
	}
	
	protected abstract int get();

}
