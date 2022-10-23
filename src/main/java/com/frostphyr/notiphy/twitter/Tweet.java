package com.frostphyr.notiphy.twitter;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.frostphyr.notiphy.Media;

public class Tweet {

	private String id;
	private String userId;
	private String timestamp;
	private String username;
	private String text;
	private Media media;
	private boolean mature;
	
	private Tweet() {
	}
	
	public String getId() {
		return id;
	}

	public String getUserId() {
		return userId;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getUsername() {
		return username;
	}

	public String getText() {
		return text;
	}
	
	public Media getMedia() {
		return media;
	}
	
	public boolean isMature() {
		return mature;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("id", id)
				.append("userId", userId)
				.append("timestamp", timestamp)
				.append("username", username)
				.append("text", text)
				.append("media", media)
				.append("mature", mature)
				.build();
	}

	public static class Builder {
		
		private Tweet tweet = new Tweet();
		
		public Builder setId(String id) {
			tweet.id = id;
			return this;
		}
	
		public Builder setUserId(String userId) {
			tweet.userId = userId;
			return this;
		}
	
		public Builder setTimestamp(String timestamp) {
			tweet.timestamp = timestamp;
			return this;
		}
	
		public Builder setUsername(String username) {
			tweet.username = username;
			return this;
		}
	
		public Builder setText(String text) {
			tweet.text = text;
			return this;
		}
	
		public Builder setMedia(Media media) {
			tweet.media = media;
			return this;
		}
		
		public Builder setMature(boolean mature) {
			tweet.mature = mature;
			return this;
		}
		
		public Tweet build() {
			Tweet temp = tweet;
			tweet = null;
			return temp;
		}
		
	}

}
