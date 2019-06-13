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
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwitterEntry[userId=");
		builder.append(userId);
		builder.append(", mediaType=");
		builder.append(mediaType);
		if (phrases != null && phrases.length > 0) {
			builder.append(", phrases=String[");
			for (int i = 0; i < phrases.length; i++) {
				builder.append(phrases[i]);
				if (i != phrases.length - 1) {
					builder.append(", ");
				}
			}
			builder.append("]");
		}
		builder.append("]");
		return builder.toString();
	}

}
