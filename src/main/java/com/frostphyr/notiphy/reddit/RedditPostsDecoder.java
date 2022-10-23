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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.text.StringEscapeUtils;

import com.frostphyr.notiphy.Media;
import com.frostphyr.notiphy.MediaType;
import com.frostphyr.notiphy.Transformer;

public class RedditPostsDecoder implements Transformer<List<RedditPost>, String> {
	
	private static final Logger logger = LoggerFactory.getLogger(RedditPostsDecoder.class);
	
	private static final int TEXT_TRIM_START = "<!-- SC_OFF --><div class=\"md\">".length();
	private static final int TEXT_TRIM_END = "\n</div><!-- SC_ON -->".length();
	
	@Override
	public List<RedditPost> transform(String encodedMessage) {
		try {
			JsonArray children = Json.createReader(new StringReader(encodedMessage))
					.readObject().getJsonObject("data").getJsonArray("children");
			List<RedditPost> posts = new ArrayList<>(children.size());
			for (int i = 0; i < children.size(); i++) {
				JsonObject data = children.getJsonObject(i).getJsonObject("data");
				RedditPost.Builder builder = new RedditPost.Builder()
						.setIdentifier(new RedditPostIdentifier(data.getString("subreddit"), data.getString("name")))
						.setTitle(data.getString("title"))
						.setUser(data.getString("author"))
						.setUrl("https://reddit.com" + data.getString("permalink"))
						.setMature(data.getBoolean("over_18"))
						.setPinned(data.getBoolean("pinned"))
						.setRobotIndexable(data.getBoolean("is_robot_indexable"))
						.setTimestamp(Long.toString(data.getJsonNumber("created_utc").longValue() * 1000));
				JsonValue textElement = data.get("selftext_html");
				if (textElement != JsonValue.NULL) {
					String text = StringEscapeUtils.unescapeHtml4(((JsonString) textElement).getString());
					builder.setText(text.substring(TEXT_TRIM_START, text.length() - TEXT_TRIM_END));
				}
				if (!data.getBoolean("is_self")) {
					builder.setMedia(parseMedia(data));
				}
				posts.add(builder.build());
			}
			return posts;
		} catch (JsonException | NullPointerException | ClassCastException e) {
			logger.error("Error decoding posts", e);
			logger.error(encodedMessage);
		}
		return null;
	}
	
	private Media parseMedia(JsonObject data) {
		if (data.containsKey("preview") && data.get("preview") != JsonValue.NULL) {
			JsonObject thumbnail = data.getJsonObject("preview")
					.getJsonArray("images")
					.getJsonObject(0)
					.getJsonObject("source");
			return new Media(data.getBoolean("is_video") ? MediaType.VIDEO : MediaType.IMAGE,
					StringEscapeUtils.unescapeHtml4(thumbnail.getString("url")),
					thumbnail.getInt("width"),
					thumbnail.getInt("height"));
		} else if (data.containsKey("gallery_data") && data.get("gallery_data") != JsonValue.NULL) {
			JsonArray galleryItems = data.getJsonObject("gallery_data")
					.getJsonArray("items");
			for (JsonValue v : galleryItems) {
				String id = ((JsonObject) v).getString("media_id");
				JsonObject metadata = data.getJsonObject("media_metadata").getJsonObject(id);
				if (metadata.getString("status").equals("valid")) {
					if (metadata.getString("e").equals("Image")) {
						JsonArray previews = metadata.getJsonArray("p");
						int width = 0;
						int height = 0;
						String url = null;
						for (JsonValue o : previews) {
							JsonObject preview = (JsonObject) o;
							int currentWidth = preview.getInt("x");
							if (Media.isPreferredWidth(width, currentWidth)) {
								width = currentWidth;
								height = preview.getInt("y");
								url = preview.getString("u");
							}
						}
						
						JsonObject source = metadata.getJsonObject("s");
						int sourceWidth = source.getInt("x");
						if (Media.isPreferredWidth(width, sourceWidth)) {
							width = sourceWidth;
							height = source.getInt("y");
							url = source.getString("u");
						}
						
						return new Media(MediaType.IMAGE, StringEscapeUtils.unescapeHtml4(url),
								width, height, galleryItems.size());
					
					} else {
						logger.error("Metadata not image: " + metadata.getString("e"));
						logger.error(data.toString());
					}
				}
			}
		}
		return null;
	}

}
