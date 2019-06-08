package com.frostphyr.notiphy.twitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.frostphyr.notiphy.EntryCollection;
import com.frostphyr.notiphy.SessionEntry;
import com.frostphyr.notiphy.util.CollectionUtils;
import com.frostphyr.notiphy.util.TextUtils;

public class TwitterEntryCollection extends EntryCollection<TwitterEntry, TwitterMessage> {

	private Map<String, Set<SessionEntry<TwitterEntry>>> userEntries = new HashMap<>();
	
	@Override
	public synchronized boolean add(Session session, TwitterEntry entry) {
		if (super.add(session, entry)) {
			Set<SessionEntry<TwitterEntry>> sessionEntries = CollectionUtils.getOrCreate(userEntries, entry.getUserId());
			sessionEntries.add(new SessionEntry<TwitterEntry>(session, entry));
			return true;
		}
		return false;
	}
	
	@Override
	public synchronized boolean remove(Session session, TwitterEntry entry) {
		return super.remove(session, entry) ? removeUserEntry(session, entry) : false;
	}
	
	@Override
	public synchronized Set<TwitterEntry> removeAll(Session session) {
		Set<TwitterEntry> entries = super.removeAll(session);
		if (entries != null) {
			for (TwitterEntry e : entries) {
				removeUserEntry(session, e);
			}
		}
		return entries;
	}
	
	private boolean removeUserEntry(Session session, TwitterEntry entry) {
		Set<SessionEntry<TwitterEntry>> entries = userEntries.get(entry.getUserId());
		if (entries != null) {
			entries.remove(new SessionEntry<TwitterEntry>(session, entry));
			return true;
		}
		return false;
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

}
