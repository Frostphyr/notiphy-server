package com.frostphyr.notiphy.reddit;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.frostphyr.notiphy.Media;

public class RedditPost {
	
	private RedditPostIdentifier identifier;
	private String user;
	private String title;
	private String timestamp;
	private String text;
	private String url;
	private Media media;
	private boolean mature;
	private boolean pinned;
	private boolean robotIndexable;
	
	private RedditPost() {
	}
	
	public RedditPostIdentifier getIdentifier() {
		return identifier;
	}

	public String getUser() {
		return user;
	}

	public String getTitle() {
		return title;
	}
	
	public String getTimestamp() {
		return timestamp;
	}

	public String getText() {
		return text;
	}
	
	public String getUrl() {
		return url;
	}
	
	public Media getMedia() {
		return media;
	}

	public boolean isMature() {
		return mature;
	}
	
	public boolean isPinned() {
		return pinned;
	}
	
	public boolean isRobotIndexable() {
		return robotIndexable;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("subreddit", identifier.getSubreddit())
				.append("fullname", identifier.getFullname())
				.append("user", user)
				.append("title", title)
				.append("timestamp", timestamp)
				.append("text", text)
				.append("url", url)
				.append("media", media)
				.append("mature", mature)
				.append("pinned", pinned)
				.append("robotIndexable", robotIndexable)
				.build();
	}

	public static class Builder {
		
		private RedditPost post = new RedditPost();
		
		public Builder setIdentifier(RedditPostIdentifier identifier) {
			post.identifier = identifier;
			return this;
		}
	
		public Builder setUser(String user) {
			post.user = user;
			return this;
		}
	
		public Builder setTitle(String title) {
			post.title = title;
			return this;
		}
		
		public Builder setTimestamp(String timestamp) {
			post.timestamp = timestamp;
			return this;
		}
		
		public Builder setText(String text) {
			post.text = text;
			return this;
		}
		
		public Builder setUrl(String url) {
			post.url = url;
			return this;
		}
		
		public Builder setMedia(Media media) {
			post.media = media;
			return this;
		}
		
		public Builder setMature(boolean mature) {
			post.mature = mature;
			return this;
		}
		
		public Builder setPinned(boolean pinned) {
			post.pinned = pinned;
			return this;
		}
		
		public Builder setRobotIndexable(boolean robotIndexable) {
			post.robotIndexable = robotIndexable;
			return this;
		}
		
		public RedditPost build() {
			RedditPost temp = post;
			post = null;
			return temp;
		}
		
	}

}
