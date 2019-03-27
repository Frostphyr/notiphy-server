package com.frostphyr.notiphy.twitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.websocket.Session;

import org.apache.commons.lang3.StringUtils;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryOperation;
import com.frostphyr.notiphy.Processor;

public class TwitterProcessor extends Processor<TwitterMessage> {
	
	private Map<String, Set<TwitterEntry>> sessionEntries = new HashMap<>();
	private Map<String, Set<SessionEntry>> userEntries = new HashMap<>();

	@Override
	public synchronized void performOperations(Session session, EntryOperation[] operations) {
		boolean addModified = false;
		boolean removeModified = false;
		if (operations[EntryOperation.REMOVE] != null) {
			removeModified = remove(session, operations[EntryOperation.REMOVE].getEntries());
		}
		if (operations[EntryOperation.ADD] != null) {
			addModified = add(session, operations[EntryOperation.ADD].getEntries());
		}
		
		if (addModified || removeModified) {
			notifyListeners();
		}
	}

	@Override
	public void removeAll(Session session) {
		Set<TwitterEntry> entries = sessionEntries.get(session.getId());
		if (entries != null) {
			boolean modified = false;
			for (TwitterEntry e : entries) {
				TwitterEntry te = (TwitterEntry) e;
				if (userEntries.get(te.getUserId()).remove(new SessionEntry(session, te))) {
					modified = true;
				}
			}
			sessionEntries.remove(session.getId());
			
			if (modified) {
				notifyListeners();
			}
		}
	}
	
	@Override
	public synchronized void processMessage(TwitterMessage message) {
		Set<SessionEntry> entries = userEntries.get(message.getUserId());
		String forward = null;
		for (SessionEntry e : entries) {
			if (contains(message.getText(), e.entry.getPhrases()) 
					&& e.entry.getMediaType().isValidFor(message.getMediaType())) {
				if (forward == null) {
					forward = TwitterForwardEncoder.createForwardMessage(message);
				}
				
				try {
					e.session.getBasicRemote().sendText(forward);
				} catch (IOException ex) {
				}
			}
		}
	}
	
	public Set<String> getUsers() {
		return userEntries.keySet();
	}
	
	public synchronized int getEntryCount() {
		int count = 0;
		for (Map.Entry<String, Set<SessionEntry>> s : userEntries.entrySet()) {
			count += s.getValue().size();
		}
		return count;
	}
	
	private boolean add(Session session, List<Entry> entries) {
		Set<TwitterEntry> se = getOrCreate(sessionEntries, session.getId());
		boolean modified = false;
		for (Entry e : entries) {
			TwitterEntry te = (TwitterEntry) e;
			if (se.add(te)) {
				Set<SessionEntry> ue = getOrCreate(userEntries, te.getUserId());
				ue.add(new SessionEntry(session, te));
				modified = true;
			}
		}
		return modified;
	}

	private boolean remove(Session session, List<Entry> entries) {
		Set<TwitterEntry> entrySet = sessionEntries.get(session.getId());
		if (entrySet != null) {
			boolean modified = false;
			for (Entry e : entries) {
				if (entrySet.remove(e)) {
					TwitterEntry te = (TwitterEntry) e;
					if (userEntries.get(te.getUserId()).remove(new SessionEntry(session, te))) {
						modified = true;
					}
				}
			}
			return modified;
		}
		return false;
	}
	
	private static boolean contains(String text, String[] phrases) {
		for (String s : phrases) {
			if (!StringUtils.containsIgnoreCase(text, s)) {
				return false;
			}
		}
		return true;
	}
	
	private static <K, T> Set<T> getOrCreate(Map<K, Set<T>> map, K key) {
		Set<T> set = map.get(key);
		if (set == null) {
			set = new HashSet<>();
			map.put(key, set);
		}
		return set;
	}
	
	private static class SessionEntry {
		
		private Session session;
		private TwitterEntry entry;
		
		public SessionEntry(Session session, TwitterEntry entry) {
			this.session = session;
			this.entry = entry;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof SessionEntry) {
				SessionEntry e = (SessionEntry) o;
				return e.session.getId().equals(session.getId())
						&& e.entry.equals(entry);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(session.getId(), entry);
		}
		
	}

}
