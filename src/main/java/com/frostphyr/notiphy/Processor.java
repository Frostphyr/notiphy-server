package com.frostphyr.notiphy;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

public abstract class Processor<T> {
	
	private List<Listener> listeners;
	
	public abstract void performOperations(Session session, EntryOperation[] operations);
	
	public abstract void removeAll(Session session);
	
	public abstract void processMessage(T message);
	
	public void addListener(Listener listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
	}
	
	public boolean removeListener(Listener listener) {
		return listeners != null ? listeners.remove(listener) : false;
	}
	
	protected void notifyListeners() {
		if (listeners != null) {
			for (Listener l : listeners) {
				l.entriesModified();
			}
		}
	}
	
	public interface Listener {
		
		void entriesModified();
		
	}

}
