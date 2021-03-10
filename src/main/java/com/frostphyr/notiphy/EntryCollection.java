package com.frostphyr.notiphy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.frostphyr.notiphy.manager.StatTracker;
import com.frostphyr.notiphy.util.CollectionUtils;

public abstract class EntryCollection<E extends Entry, M extends Message> {
	
	private Map<String, Set<E>> sessionEntries = new HashMap<>();
	private List<Listener> listeners;
	private boolean modified;
	
	public synchronized boolean add(Session session, E entry) {
		Set<E> entries = CollectionUtils.getOrCreate(sessionEntries, session.getId());
		if (entries.add(entry)) {
			modified = true;
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void addAll(Session session, List<Entry> entries) {
		for (Entry e : entries) {
			add(session, (E) e);
		}
	}
	
	public synchronized boolean remove(Session session, E entry) {
		Set<E> entries = sessionEntries.get(session.getId());
		if (entries != null && entries.remove(entry)) {
			if (entries.size() == 0) {
				sessionEntries.remove(session.getId());
			}
			
			modified = true;
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void removeAll(Session session, List<Entry> entries) {
		for (Entry e : entries) {
			remove(session, (E) e);
		}
	}
	
	public synchronized Set<E> removeAll(Session session) {
		Set<E> entries = sessionEntries.remove(session.getId());
		if (entries != null) {
			modified = true;
		}
		return entries;
	}
	
	public synchronized void finishModifications() {
		if (modified) {
			notifyListeners();
			modified = false;
		}
	}
	
	public synchronized void forEachEntry(String sessionId, EntryCallback<E> callback) {
		Set<E> entries = sessionEntries.get(sessionId);
		if (entries != null) {
			entries.forEach((e) -> callback.onCall(e));
		}
	}
	
	public synchronized int getCount(Session session) {
		Set<E> entries = sessionEntries.get(session.getId());
		return entries != null ? entries.size() : 0;
	}
	
	public synchronized int getCount() {
		return sessionEntries.size();
	}
	
	public abstract StatTracker[] getTrackers();
	
	protected abstract List<SessionEntry<E>> getMatches(M message);
	
	protected abstract boolean isMatch(E entry, M message);
	
	public synchronized void addListener(Listener listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
	}
	
	public synchronized boolean removeListener(Listener listener) {
		return listeners != null ? listeners.remove(listener) : false;
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
	
	private void notifyListeners() {
		if (listeners != null) {
			for (Listener l : listeners) {
				l.entriesModified();
			}
		}
	}
	
	public interface Listener {
		
		void entriesModified();
		
	}
	
	public interface EntryCallback<E> {
		
		void onCall(E entry);
		
	}

}
