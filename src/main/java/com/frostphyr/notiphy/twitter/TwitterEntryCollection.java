package com.frostphyr.notiphy.twitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryCollection;
import com.frostphyr.notiphy.EntryOperation;
import com.frostphyr.notiphy.SessionEntry;
import com.frostphyr.notiphy.util.CollectionUtils;
import com.frostphyr.notiphy.util.TextUtils;

public class TwitterEntryCollection extends EntryCollection<TwitterEntry, TwitterMessage> {

	private Map<String, Set<TwitterEntry>> sessionEntries = new HashMap<>();
	private Map<String, Set<SessionEntry<TwitterEntry>>> userEntries = new HashMap<>();
	private int count;

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
	public synchronized void removeAll(Session session) {
		Set<TwitterEntry> entries = sessionEntries.remove(session.getId());
		if (entries != null) {
			boolean modified = false;
			for (TwitterEntry e : entries) {
				TwitterEntry te = (TwitterEntry) e;
				if (userEntries.get(te.getUserId()).remove(new SessionEntry<TwitterEntry>(session, te))) {
					modified = true;
				}
			}
			
			if (modified) {
				notifyListeners();
			}
		}
	}
	
	@Override
	public synchronized List<SessionEntry<TwitterEntry>> getMatches(TwitterMessage message) {
		List<SessionEntry<TwitterEntry>> entries = new ArrayList<>();
		getMatches(entries, userEntries.get(message.getUserId()), message);
		return entries;
	}
	
	@Override
	protected boolean isMatch(TwitterEntry entry, TwitterMessage message) {
		return TextUtils.contains(message.getText(), entry.getPhrases()) 
					&& entry.getMediaType().isValidFor(message.getMediaType());
	}
	
	public Set<String> getUsers() {
		return userEntries.keySet();
	}
	
	public int getCount() {
		return count;
	}
	
	private boolean add(Session session, List<Entry> entries) {
		Set<TwitterEntry> se = CollectionUtils.getOrCreate(sessionEntries, session.getId());
		boolean modified = false;
		for (Entry e : entries) {
			TwitterEntry entry = (TwitterEntry) e;
			if (se.add(entry)) {
				Set<SessionEntry<TwitterEntry>> sessionEntries = CollectionUtils.getOrCreate(userEntries, entry.getUserId());
				sessionEntries.add(new SessionEntry<TwitterEntry>(session, entry));
				count++;
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
					userEntries.get(te.getUserId()).remove(new SessionEntry<TwitterEntry>(session, te));
					count--;
					modified = true;
				}
			}
			return modified;
		}
		return false;
	}

}
