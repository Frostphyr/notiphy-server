package com.frostphyr.notiphy;

public enum EntryType {
	
	TWITTER(new TwitterDecoder());
	
	private final EntryDecoder decoder;
	
	private EntryType(EntryDecoder decoder) {
		this.decoder = decoder;
	}
	
	public EntryDecoder getDecoder() {
		return decoder;
	}

}
