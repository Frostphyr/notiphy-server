package com.frostphyr.notiphy;

public class TwitterEntry implements Entry {
	
	private String username;
	private MediaType mediaType;
	private String[] phrases;
	
	public TwitterEntry(String username, MediaType mediaType, String[] phrases) {
		this.username = username;
		this.mediaType = mediaType;
		this.phrases = phrases;
	}
	
	public String getUsername() {
		return username;
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
