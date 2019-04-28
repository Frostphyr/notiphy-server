package com.frostphyr.notiphy.reddit;

public class RedditPostIdentifier {
	
	private String subreddit;
	private String fullname;
	
	public RedditPostIdentifier(String subreddit, String fullname) {
		this.subreddit = subreddit;
		this.fullname = fullname;
	}
	
	public String getSubreddit() {
		return subreddit;
	}
	
	public String getFullname() {
		return fullname;
	}

}
