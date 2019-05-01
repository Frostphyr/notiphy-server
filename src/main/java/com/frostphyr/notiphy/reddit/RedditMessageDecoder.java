package com.frostphyr.notiphy.reddit;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.frostphyr.notiphy.MessageDecoder;

public class RedditMessageDecoder implements MessageDecoder<RedditMessage> {
	
	private static final Logger logger = LogManager.getLogger(RedditMessageDecoder.class);
	
	@Override
	public RedditMessage decode(String encodedMessage) {
		try {
			JsonArray children = Json.createReader(new StringReader(encodedMessage))
					.readObject().getJsonObject("data").getJsonArray("children");
			List<RedditMessage.Post> posts = new ArrayList<>(children.size());
			for (int i = 0; i < children.size(); i++) {
				JsonObject data = children.getJsonObject(i).getJsonObject("data");
				RedditMessage.Post.Builder builder = new RedditMessage.Post.Builder()
						.setIdentifier(new RedditPostIdentifier(data.getString("subreddit"), data.getString("name")))
						.setTitle(data.getString("title"))
						.setUser(data.getString("author"))
						.setText(data.getString("selftext"))
						.setUrl("https://reddit.com" + data.getString("permalink"))
						.setNsfw(data.getBoolean("over_18"))
						.setPinned(data.getBoolean("pinned"))
						.setCreatedAt(Long.toString(data.getJsonNumber("created_utc").longValue() * 1000));
				if (!data.getBoolean("is_self")) {
					builder.setLink(data.getString("url"));
					builder.setVideo(data.getBoolean("is_video"));
					builder.setThumbnailUrl(StringEscapeUtils.unescapeHtml4(data.containsKey("preview") ? 
							data.getJsonObject("preview")
								.getJsonArray("images")
								.getJsonObject(0)
								.getJsonObject("source")
								.getString("url") : 
							data.getString("thumbnail")));
				}
				posts.add(builder.build());
			}
			return new RedditMessage(posts);
		} catch (JsonException e) {
			logger.error(e);
		}
		return null;
	}

}
