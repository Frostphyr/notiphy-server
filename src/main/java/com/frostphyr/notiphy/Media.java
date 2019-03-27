package com.frostphyr.notiphy;

public class Media {
	
	private MediaType type;
	private String url;
	private String thumbnailUrl;
	
	public Media(MediaType type, String url, String thumbnailUrl) {
		this.type = type;
		this.url = url;
		this.thumbnailUrl = thumbnailUrl;
	}
	
	public MediaType getType() {
		return type;
	}

	public String getUrl() {
		return url;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

}
