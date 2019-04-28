package com.frostphyr.notiphy.reddit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryCollection;
import com.frostphyr.notiphy.EntryOperation;
import com.frostphyr.notiphy.SessionEntry;
import com.frostphyr.notiphy.util.CollectionUtils;
import com.frostphyr.notiphy.util.FixedLinkedStack;
import com.frostphyr.notiphy.util.TextUtils;

public class RedditEntryCollection extends EntryCollection<RedditEntry, RedditMessage.Post> {
	
	private static final int LATEST_POSTS_CAPACITY = 100;

	private Map<String, Set<RedditEntry>> sessionEntries = new HashMap<>();
	private Map<String, Container> userEntries = new HashMap<>();
	private Map<String, Container> subredditEntries = new HashMap<>();
	private int count;

	@Override
	public synchronized void performOperations(Session session, EntryOperation[] operations) {
		boolean modified = false;
		if (operations[EntryOperation.ADD] != null) {
			if (addEntries(session, operations[EntryOperation.ADD].getEntries())) {
				modified = true;
			}
		}
		if (operations[EntryOperation.REMOVE] != null) {
			if (removeEntries(session, operations[EntryOperation.REMOVE].getEntries(), true)) {
				modified = true;
			}
		}
		
		if (modified) {
			notifyListeners();
		}
	}

	@Override
	public synchronized void removeAll(Session session) {
		Set<RedditEntry> entries = sessionEntries.remove(session.getId());
		if (entries != null) {
			if (removeEntries(session, entries, false)) {
				notifyListeners();
			}
		}
	}

	@Override
	public synchronized List<SessionEntry<RedditEntry>> getMatches(RedditMessage.Post post) {
		List<SessionEntry<RedditEntry>> entries = new ArrayList<>();
		getMatches(entries, post, userEntries, post.getUser());
		getMatches(entries, post, subredditEntries, post.getIdentifier().getSubreddit());
		return entries;
	}
	
	@Override
	protected boolean isMatch(RedditEntry entry, RedditMessage.Post post) {
		return isPostTypeValid(post, entry) && 
				TextUtils.contains(new String[] {post.getTitle(), post.getText()}, entry.getPhrases());
	}
	
	public int getCount() {
		return count;
	}
	
	public void forEachUser(Callback callback) {
		userEntries.forEach((k, v) -> callCallback(callback, k, v));
	}
	
	public void forEachSubreddit(Callback callback) {
		subredditEntries.forEach((k, v) -> callCallback(callback, k, v));
	}
	
	private void callCallback(Callback callback, String value, Container container) {
		if (callback.onCall(value, container.latestPosts, container.noResultCount)) {
			container.noResultCount = 0;
		} else {
			container.noResultCount++;
		}
	}
	
	private boolean addEntries(Session session, List<Entry> entries) {
		boolean modified = false;
		for (Entry e : entries) {
			SessionEntry<RedditEntry> entry = new SessionEntry<>(session, (RedditEntry) e);
			if (entry.getEntry().getUser() != null) {
				if (addToMap(userEntries, entry, entry.getEntry().getUser())) {
					addSessionEntry(session.getId(), entry.getEntry());
					count++;
					modified = true;
				}
			} else if (entry.getEntry().getSubreddit() != null) {
				if (addToMap(subredditEntries, entry, entry.getEntry().getSubreddit())) {
					addSessionEntry(session.getId(), entry.getEntry());
					count++;
					modified = true;
				}
			}
		}
		return modified;
	}
	
	private boolean addToMap(Map<String, Container> map, SessionEntry<RedditEntry> entry, String key) {
		Container container = map.get(key);
		if (container == null) {
			container = new Container();
			map.put(key, container);
		}
		return container.entries.add(entry);
	}
	
	private void addSessionEntry(String sessionId, RedditEntry entry) {
		CollectionUtils.getOrCreate(sessionEntries, sessionId).add(entry);
	}
	
	private boolean removeEntries(Session session, Collection<? extends Entry> entries, boolean removeFromSession) {
		boolean modified = false;
		for (Entry e : entries) {
			SessionEntry<RedditEntry> entry = new SessionEntry<>(session, (RedditEntry) e);
			if (entry.getEntry().getUser() != null) {
				if (removeFromMap(userEntries, entry, entry.getEntry().getUser())) {
					if (removeFromSession) {
						removeSessionEntry(session.getId(), entry.getEntry());
					}
					count--;
					modified = true;
				}
			} else if (entry.getEntry().getSubreddit() != null) {
				if (removeFromMap(subredditEntries, entry, entry.getEntry().getSubreddit())) {
					if (removeFromSession) {
						removeSessionEntry(session.getId(), entry.getEntry());
					}
					count--;
					modified = true;
				}
			}
		}
		return modified;
	}
	
	private boolean removeFromMap(Map<String, Container> map, SessionEntry<RedditEntry> entry, String key) {
		Container container = map.get(key);
		if (container != null) {
			boolean result = container.entries.remove(entry);
			if (container.entries.size() == 0) {
				map.remove(key);
			}
			return result;
		}
		return false;
	}
	
	private void removeSessionEntry(String sessionId, RedditEntry entry) {
		Set<RedditEntry> entries = sessionEntries.get(sessionId);
		if (entries != null) {
			entries.remove(entry);
		}
	}
	
	private void getMatches(List<SessionEntry<RedditEntry>> entries, RedditMessage.Post post, Map<String, Container> map, String key) {
		Container container = map.get(key.toLowerCase());
		if (container != null) {
			getMatches(entries, container.entries, post);
		}
	}
	
	private boolean isPostTypeValid(RedditMessage.Post post, RedditEntry entry) {
		return entry.getPostType() == RedditPostType.ANY || 
				(entry.getPostType() == RedditPostType.TEXT && post.getText() != null) ||
				(entry.getPostType() == RedditPostType.LINK && post.getLink() != null);
	}
	
	public interface Callback {
		
		boolean onCall(String value, FixedLinkedStack<RedditPostIdentifier> latestPosts, int noResultCount);
		
	}
	
	private static class Container {
		
		private Set<SessionEntry<RedditEntry>> entries = new HashSet<>();
		private FixedLinkedStack<RedditPostIdentifier> latestPosts = new FixedLinkedStack<>(LATEST_POSTS_CAPACITY);
		private int noResultCount;
		
	}

}
