package com.frostphyr.notiphy;

import java.util.ArrayList;
import java.util.List;

public class EntryOperation {
	
	public static final int ADD = 0;
	public static final int REMOVE = 1;
	
	private List<Entry> entries = new ArrayList<Entry>();
	private int operation;
	
	
	public EntryOperation(int operation) {
		this.operation = operation;
	}
	
	public int getOperation() {
		return operation;
	}
	
	public List<Entry> getEntries() {
		return entries;
	}

}
