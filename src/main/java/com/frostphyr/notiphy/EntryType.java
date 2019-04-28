package com.frostphyr.notiphy;

import com.frostphyr.notiphy.reddit.RedditClient;
import com.frostphyr.notiphy.reddit.RedditEntryCollection;
import com.frostphyr.notiphy.reddit.RedditEntryDecoder;
import com.frostphyr.notiphy.reddit.RedditMessageDecoder;
import com.frostphyr.notiphy.reddit.RedditMessageEncoder;
import com.frostphyr.notiphy.twitter.TwitterClient;
import com.frostphyr.notiphy.twitter.TwitterEntryCollection;
import com.frostphyr.notiphy.twitter.TwitterEntryDecoder;
import com.frostphyr.notiphy.twitter.TwitterMessageDecoder;
import com.frostphyr.notiphy.twitter.TwitterMessageEncoder;

public enum EntryType {
	
	TWITTER(new TwitterEntryDecoder(), new TwitterClient(new TwitterMessageDecoder(), new TwitterMessageEncoder(), new TwitterEntryCollection())),
	REDDIT(new RedditEntryDecoder(), new RedditClient(new RedditMessageDecoder(), new RedditMessageEncoder(), new RedditEntryCollection()));
	
	private final EntryDecoder decoder;
	private final EntryClient client;
	
	private EntryType(EntryDecoder decoder, EntryClient client) {
		this.decoder = decoder;
		this.client = client;
	}
	
	public EntryDecoder getDecoder() {
		return decoder;
	}
	
	public EntryClient getClient() {
		return client;
	}

}
