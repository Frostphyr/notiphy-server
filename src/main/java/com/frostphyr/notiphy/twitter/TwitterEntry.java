package com.frostphyr.notiphy.twitter;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.MediaType;

public class TwitterEntry implements Entry {
	
	private long userId;
	private MediaType mediaType;
	private String[] phrases;
	
	public TwitterEntry(long userId, MediaType mediaType, String[] phrases) {
		this.userId = userId;
		this.mediaType = mediaType;
		this.phrases = phrases;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public MediaType getMediaType() {
		return mediaType;
	}
	
	public String[] getPhrases() {
		return phrases;
	}

	@Override
	public int getType() {
		return 0;
	}

}
