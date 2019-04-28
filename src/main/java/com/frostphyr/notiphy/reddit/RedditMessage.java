package com.frostphyr.notiphy.reddit;

import java.util.List;

import com.frostphyr.notiphy.Message;

public class RedditMessage implements Message {
	
	private List<Post> posts;
	
	public RedditMessage(List<Post> posts) {
		this.posts = posts;
	}
	
	public List<Post> getPosts() {
		return posts;
	}

	public static class Post implements Message {
		
		private RedditPostIdentifier identifier;
		private String user;
		private String title;
		private String createdAt;
		private String text;
		private String url;
		private String thumbnailUrl;
		private String link;
		private boolean video;
		private boolean nsfw;
		private boolean pinned;
		
		private Post() {
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
		
		public String getCreatedAt() {
			return createdAt;
		}
	
		public String getText() {
			return text;
		}
		
		public String getUrl() {
			return url;
		}
		
		public String getThumbnailUrl() {
			return thumbnailUrl;
		}
		
		public String getLink() {
			return link;
		}
		
		public boolean isVideo() {
			return video;
		}
	
		public boolean isNsfw() {
			return nsfw;
		}
		
		public boolean isPinned() {
			return pinned;
		}
	
		public static class Builder {
			
			private Post post = new Post();
			
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
			
			public Builder setCreatedAt(String createdAt) {
				post.createdAt = createdAt;
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
			
			public Builder setThumbnailUrl(String thumbnailUrl) {
				post.thumbnailUrl = thumbnailUrl;
				return this;
			}
		
			public Builder setLink(String link) {
				post.link = link;
				return this;
			}
			
			public Builder setVideo(boolean video) {
				post.video = video;
				return this;
			}
			
			public Builder setNsfw(boolean nsfw) {
				post.nsfw = nsfw;
				return this;
			}
			
			public Builder setPinned(boolean pinned) {
				post.pinned = pinned;
				return this;
			}
			
			public Post build() {
				Post p = post;
				post = null;
				return p;
			}
			
		}
	
	}

}
