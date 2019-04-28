package com.frostphyr.notiphy.reddit;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryDecoder;

public class RedditEntryDecoder implements EntryDecoder {

	@Override
	public Entry decode(JsonObject o) {
		String user = o.containsKey("user") ? o.getString("user").toLowerCase() : null;
		String subreddit = o.containsKey("subreddit") ? o.getString("subreddit").toLowerCase() : null;
		JsonArray arr = o.getJsonArray("phrases");
		String[] phrases = new String[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			phrases[i] = arr.getString(i);
		}
		RedditPostType postType = RedditPostType.valueOf(o.getString("postType"));
		return new RedditEntry(user, subreddit, phrases, postType);
	}

}
