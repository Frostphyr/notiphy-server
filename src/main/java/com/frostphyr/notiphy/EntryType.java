package com.frostphyr.notiphy;

import com.frostphyr.notiphy.twitter.TwitterClient;
import com.frostphyr.notiphy.twitter.TwitterEntryDecoder;
import com.frostphyr.notiphy.twitter.TwitterProcessor;

public enum EntryType {
	
	TWITTER(new TwitterEntryDecoder(), new TwitterClient(), new TwitterProcessor());
	
	private final EntryDecoder decoder;
	private final EntryClient<?> client;
	private final Processor<?> processor;
	
	private EntryType(EntryDecoder decoder, EntryClient<?> client, Processor<?> processor) {
		this.decoder = decoder;
		this.client = client;
		this.processor = processor;
	}
	
	public EntryDecoder getDecoder() {
		return decoder;
	}
	
	public EntryClient<?> getClient() {
		return client;
	}
	
	public Processor<?> getProcessor() {
		return processor;
	}

}
