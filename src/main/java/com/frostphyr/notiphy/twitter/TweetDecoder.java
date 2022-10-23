package com.frostphyr.notiphy.twitter;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.text.StringEscapeUtils;

import com.frostphyr.notiphy.Media;
import com.frostphyr.notiphy.MediaType;
import com.frostphyr.notiphy.Transformer;

public class TweetDecoder implements Transformer<Tweet, String> {
	
	private static final Logger logger = LoggerFactory.getLogger(TweetDecoder.class);
	
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
	private static final String[] SIZES = {"thumb", "large", "medium", "small"};
	
	public Tweet transform(String encodedMessage) {
		try {
			JsonObject obj = Json.createReader(new StringReader(encodedMessage)).readObject();
			if (obj.containsKey("delete") || obj.containsKey("error")) {
				return null;
			}
			
			Tweet.Builder builder = new Tweet.Builder();
			JsonObject user = obj.getJsonObject("user");
			builder.setId(obj.getString("id_str"))
					.setUserId(user.getString("id_str"))
					.setTimestamp(Long.toString(DATE_FORMAT.parse(obj.getString("created_at")).getTime()))
					.setUsername(user.getString("screen_name"))
					.setText(getDisplayText(obj))
					.setMature(obj.containsKey("possibly_sensitive") && obj.getBoolean("possibly_sensitive"));

			if (obj.containsKey("extended_entities")) {
				JsonObject entities = obj.getJsonObject("extended_entities");
				if (entities.containsKey("media")) {
					JsonArray mediaArray = entities.getJsonArray("media");
					JsonObject firstMedia = mediaArray.getJsonObject(0);
					String url = firstMedia.getString("media_url_https");
					JsonObject sizes = firstMedia.getJsonObject("sizes");
					String name = null;
					int width = 0;
					int height = 0;
					for (String s : SIZES) {
						JsonObject size = sizes.getJsonObject(s);
						int currentWidth = size.getInt("w");
						if (Media.isPreferredWidth(width, currentWidth)) {
							name = s;
							width = currentWidth;
							height = size.getInt("h");
						}
					}
					builder.setMedia(new Media(getMediaType(firstMedia.getString("type")),
							url + "?name=" + name, width, height, mediaArray.size()));
				}
			}
			return builder.build();
		} catch (JsonException | ParseException | NullPointerException | ClassCastException | IllegalArgumentException e) {
			logger.error("Error decoding Tweet", e);
			return null;
		}
	}
	
	private static String getDisplayText(JsonObject obj) {
		String text = null;
		if (obj.containsKey("extended_tweet")) {
			obj = obj.getJsonObject("extended_tweet");
			text = obj.getString("full_text");
		} else {
			text = obj.getString("text");
		}
		if (obj.containsKey("display_text_range")) {
			JsonArray range = obj.getJsonArray("display_text_range");
			text = text.substring(range.getInt(0), range.getInt(1));
		}
		return StringEscapeUtils.unescapeHtml4(text);
	}
	
	private static MediaType getMediaType(String type) {
		switch (type) {
			case "photo":
				return MediaType.IMAGE;
			case "video":
			case "animated_gif":
				return MediaType.VIDEO;
			default:
				throw new IllegalArgumentException("Invalid media type: " + type);
		}
	}

}
