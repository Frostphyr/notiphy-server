package com.frostphyr.notiphy;

public class EntryOperation {
	
	public static final int ADD = 0;
	public static final int REMOVE = 1;
	public static final int UPDATE = 2;
	
	private int operation;
	private Entry[] entries;
	
	public EntryOperation(int operation, Entry[] entries) {
		this.operation = operation;
		this.entries = entries;
	}
	
	public int getOperation() {
		return operation;
	}
	
	public Entry[] getEntries() {
		return entries;
	}

}
