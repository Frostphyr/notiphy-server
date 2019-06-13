package com.frostphyr.notiphy.reddit;

import java.util.Arrays;
import java.util.Objects;

import com.frostphyr.notiphy.Entry;

public class RedditEntry implements Entry {

	private String user;
	private String subreddit;
	private String[] phrases;
	private RedditPostType postType;
	
	public RedditEntry(String user, String subreddit, String[] phrases, RedditPostType postType) {
		this.user = user;
		this.subreddit = subreddit;
		this.phrases = phrases;
		this.postType = postType;
	}

	public String getUser() {
		return user;
	}

	public String getSubreddit() {
		return subreddit;
	}
	
	public String[] getPhrases() {
		return phrases;
	}
	
	public RedditPostType getPostType() {
		return postType;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof RedditEntry) {
			RedditEntry e = (RedditEntry) o;
			return Objects.equals(e.user, user) &&
					Objects.equals(e.subreddit, subreddit) &&
					Objects.deepEquals(e.phrases, phrases) &&
					e.postType == postType;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(user, subreddit, Arrays.hashCode(phrases), postType);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RedditEntry[");
		builder.append(user != null ? "user=" + user : "subreddit=" + subreddit);
		builder.append(", postType=");
		builder.append(postType);
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
