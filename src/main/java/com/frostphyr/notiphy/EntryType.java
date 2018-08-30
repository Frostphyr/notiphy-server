package com.frostphyr.notiphy;

import com.frostphyr.notiphy.twitter.TwitterDecoder;
import com.frostphyr.notiphy.twitter.TwitterRelay;

public enum EntryType {
	
	TWITTER(new TwitterDecoder(), new TwitterRelay());
	
	private final EntryDecoder decoder;
	private final EntryRelay relay;
	
	private EntryType(EntryDecoder decoder, EntryRelay relay) {
		this.decoder = decoder;
		this.relay = relay;
	}
	
	public EntryDecoder getDecoder() {
		return decoder;
	}
	
	public EntryRelay getRelay() {
		return relay;
	}

}
