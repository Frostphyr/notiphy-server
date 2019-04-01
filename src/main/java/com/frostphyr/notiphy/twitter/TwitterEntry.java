package com.frostphyr.notiphy.twitter;

import java.util.Arrays;
import java.util.Objects;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.MediaType;

public class TwitterEntry implements Entry {
	
	private String userId;
	private MediaType mediaType;
	private String[] phrases;
	
	public TwitterEntry(String userId, MediaType mediaType, String[] phrases) {
		this.userId = userId;
		this.mediaType = mediaType;
		this.phrases = phrases;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public MediaType getMediaType() {
		return mediaType;
	}
	
	public String[] getPhrases() {
		return phrases;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TwitterEntry) {
			TwitterEntry e = (TwitterEntry) o;
			return e.userId.equals(e.userId) &&
					e.mediaType == mediaType &&
					Arrays.deepEquals(e.phrases, phrases);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(userId, mediaType, Arrays.hashCode(phrases));
	}

}
