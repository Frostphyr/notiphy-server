package com.frostphyr.notiphy.reddit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;

import com.frostphyr.notiphy.EntryCollection;
import com.frostphyr.notiphy.SessionEntry;
import com.frostphyr.notiphy.util.FixedLinkedStack;
import com.frostphyr.notiphy.util.TextUtils;

public class RedditEntryCollection extends EntryCollection<RedditEntry, RedditMessage.Post> {
	
	private static final int LATEST_POSTS_CAPACITY = 100;

	private Map<String, Container> userEntries = new HashMap<>();
	private Map<String, Container> subredditEntries = new HashMap<>();
	
	@Override
	public synchronized boolean add(Session session, RedditEntry entry) {
		if (super.add(session, entry)) {
			SessionEntry<RedditEntry> sessionEntry = new SessionEntry<>(session, entry);
			if (entry.getUser() != null) {
				addToMap(userEntries, sessionEntry, entry.getUser());
			} else if (entry.getSubreddit() != null) {
				addToMap(subredditEntries, sessionEntry, entry.getSubreddit());
			}
			return true;
		}
		return false;
	}
	
	@Override
	public synchronized boolean remove(Session session, RedditEntry entry) {
		if (super.remove(session, entry)) {
			removeEntry(session, entry);
			return true;
		}
		return false;
	}

	@Override
	public synchronized Set<RedditEntry> removeAll(Session session) {
		Set<RedditEntry> entries = super.removeAll(session);
		if (entries != null) {
			for (RedditEntry e : entries) {
				removeEntry(session, e);
			}
		}
		return entries;
	}
	
	private void removeEntry(Session session, RedditEntry entry) {
		SessionEntry<RedditEntry> sessionEntry = new SessionEntry<>(session, entry);
		if (entry.getUser() != null) {
			removeFromMap(userEntries, sessionEntry, entry.getUser());
		} else if (entry.getSubreddit() != null) {
			removeFromMap(subredditEntries, sessionEntry, entry.getSubreddit());
		}
	}
	
	private boolean addToMap(Map<String, Container> map, SessionEntry<RedditEntry> entry, String key) {
		Container container = map.get(key);
		if (container == null) {
			container = new Container();
			map.put(key, container);
		}
		return container.entries.add(entry);
	}
	
	private void removeFromMap(Map<String, Container> map, SessionEntry<RedditEntry> entry, String key) {
		Container container = map.get(key);
		if (container != null) {
			container.entries.remove(entry);
			if (container.entries.size() == 0) {
				map.remove(key);
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
	
	private void getMatches(List<SessionEntry<RedditEntry>> entries, RedditMessage.Post post, Map<String, Container> map, String key) {
		Container container = map.get(key.toLowerCase());
		if (container != null) {
			getMatches(entries, container.entries, post);
		}
	}
	
	@Override
	protected boolean isMatch(RedditEntry entry, RedditMessage.Post post) {
		return isPostTypeValid(post, entry) && 
				TextUtils.contains(new String[] {post.getTitle(), post.getText()}, entry.getPhrases());
	}
	
	private boolean isPostTypeValid(RedditMessage.Post post, RedditEntry entry) {
		return entry.getPostType() == RedditPostType.ANY || 
				(entry.getPostType() == RedditPostType.TEXT && post.getText() != null) ||
				(entry.getPostType() == RedditPostType.LINK && post.getLink() != null);
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
	
	public interface Callback {
		
		boolean onCall(String value, FixedLinkedStack<RedditPostIdentifier> latestPosts, int noResultCount);
		
	}
	
	private static class Container {
		
		private Set<SessionEntry<RedditEntry>> entries = new HashSet<>();
		private FixedLinkedStack<RedditPostIdentifier> latestPosts = new FixedLinkedStack<>(LATEST_POSTS_CAPACITY);
		private int noResultCount;
		
	}

}
