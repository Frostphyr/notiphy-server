package com.frostphyr.notiphy;

import java.util.List;

public class EntryOperation {
	
	public static final int ADD = 0;
	public static final int REMOVE = 1;
	public static final int UPDATE = 2;
	
	private int operation;
	private List<Entry>[] entries;
	
	public EntryOperation(int operation, List<Entry>[] entries) {
		this.operation = operation;
		this.entries = entries;
	}
	
	public int getOperation() {
		return operation;
	}
	
	public List<Entry>[] getEntries() {
		return entries;
	}

}
