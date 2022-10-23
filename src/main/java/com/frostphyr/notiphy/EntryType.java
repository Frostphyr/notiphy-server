package com.frostphyr.notiphy;

import com.frostphyr.notiphy.reddit.RedditClient;
import com.frostphyr.notiphy.reddit.RedditEntry;
import com.frostphyr.notiphy.twitter.TwitterClient;
import com.frostphyr.notiphy.twitter.TwitterEntry;

public enum EntryType {
	
	TWITTER(TwitterEntry.class, new TwitterClient()),
	REDDIT(RedditEntry.class, new RedditClient());
	
	private final Class<? extends Entry> entryClass;
	private final EntryClient<?> client;
	
	private EntryType(Class<? extends Entry> entryClass, EntryClient<?> client) {
		this.entryClass = entryClass;
		this.client = client;
	}
	
	public Class<? extends Entry> getEntryClass() {
		return entryClass;
	}
	
	public EntryClient<?> getClient() {
		return client;
	}

}
