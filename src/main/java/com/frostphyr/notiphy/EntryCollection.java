package com.frostphyr.notiphy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.websocket.Session;

public abstract class EntryCollection<E extends Entry, M extends Message> {
	
	private List<Listener> listeners;
	
	public abstract void performOperations(Session session, EntryOperation[] operations);
	
	public abstract void removeAll(Session session);
	
	protected abstract List<SessionEntry<E>> getMatches(M message);
	
	protected abstract boolean isMatch(E entry, M message);
	
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
	
	protected void getMatches(List<SessionEntry<E>> matchedEntries, Collection<SessionEntry<E>> entries, M message) {
		if (entries != null) {
			for (SessionEntry<E> e : entries) {
				if (isMatch(e.getEntry(), message)) {
					matchedEntries.add(e);
				}
			}
		}
	}
	
	public interface Listener {
		
		void entriesModified();
		
	}

}
