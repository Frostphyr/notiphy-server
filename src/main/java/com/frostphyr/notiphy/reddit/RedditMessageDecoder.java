package com.frostphyr.notiphy.reddit;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.frostphyr.notiphy.MessageDecoder;

public class RedditMessageDecoder implements MessageDecoder<RedditMessage> {
	
	private static final Logger logger = LogManager.getLogger(RedditMessageDecoder.class);
	
	private static final String TEXT_TRIM_START = "&lt;!-- SC_OFF --&gt;&lt;div class=\"md\"&gt;";
	private static final String TEXT_TRIM_END = "\n&lt;/div&gt;&lt;!-- SC_ON --&gt;";
	
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
						.setUrl("https://reddit.com" + data.getString("permalink"))
						.setNsfw(data.getBoolean("over_18"))
						.setPinned(data.getBoolean("pinned"))
						.setCreatedAt(Long.toString(data.getJsonNumber("created_utc").longValue() * 1000));
				JsonValue textElement = data.get("selftext_html");
				if (textElement != JsonValue.NULL) {
					String text = ((JsonString) textElement).getString();
					builder.setText(text.substring(TEXT_TRIM_START.length(), text.length() - TEXT_TRIM_END.length()));
				}
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
