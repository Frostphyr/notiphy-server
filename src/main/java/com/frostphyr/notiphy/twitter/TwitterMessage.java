package com.frostphyr.notiphy.twitter;

import com.frostphyr.notiphy.MediaType;
import com.frostphyr.notiphy.Message;
import com.frostphyr.notiphy.Media;

public class TwitterMessage implements Message {

	private String id;
	private String userId;
	private String createdAt;
	private String username;
	private String text;
	private MediaType mediaType;
	private Media[] media;
	
	private TwitterMessage() {
	}
	
	public String getId() {
		return id;
	}

	public String getUserId() {
		return userId;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public String getUsername() {
		return username;
	}

	public String getText() {
		return text;
	}

	public MediaType getMediaType() {
		return mediaType;
	}
	
	public Media[] getMedia() {
		return media;
	}

	public static class Builder {
		
		private TwitterMessage message = new TwitterMessage();
		
		public Builder setId(String id) {
			message.id = id;
			return this;
		}
	
		public Builder setUserId(String userId) {
			message.userId = userId;
			return this;
		}
	
		public Builder setCreatedAt(String createdAt) {
			message.createdAt = createdAt;
			return this;
		}
	
		public Builder setUsername(String username) {
			message.username = username;
			return this;
		}
	
		public Builder setText(String text) {
			message.text = text;
			return this;
		}
	
		public Builder setMediaType(MediaType mediaType) {
			message.mediaType = mediaType;
			return this;
		}
	
		public Builder setMedia(Media[] media) {
			message.media = media;
			return this;
		}
		
		public TwitterMessage build() {
			TwitterMessage m = message;
			message = null;
			return m;
		}
		
	}

}
