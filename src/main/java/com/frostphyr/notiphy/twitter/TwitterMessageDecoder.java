package com.frostphyr.notiphy.twitter;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.frostphyr.notiphy.Media;
import com.frostphyr.notiphy.MediaType;
import com.frostphyr.notiphy.MessageDecoder;

public class TwitterMessageDecoder implements MessageDecoder<TwitterMessage> {
	
	private static final Logger logger = LogManager.getLogger(TwitterMessageDecoder.class);
	
	public TwitterMessage decode(String encodedMessage) {
		try {
			TwitterMessage.Builder builder = new TwitterMessage.Builder();
			JsonReader reader = Json.createReader(new StringReader(encodedMessage));
			JsonObject obj = reader.readObject();
			builder.setId(obj.getString("id_str"));
			JsonObject user = obj.getJsonObject("user");
			builder.setUserId(user.getString("id_str"))
					.setCreatedAt(obj.getString("created_at"))
					.setUsername(user.getString("screen_name"))
					.setText(obj.containsKey("extended_tweet") ? 
							getDisplayText("full_text", obj.getJsonObject("extended_tweet")) : 
							getDisplayText("text", obj));
			
			MediaType mediaType = MediaType.NONE;
			if (obj.containsKey("extended_entities")) {
				JsonObject entities = obj.getJsonObject("extended_entities");
				if (entities.containsKey("media")) {
					JsonArray mediaArray = entities.getJsonArray("media");
					Media[] media = new Media[mediaArray.size()];
					for (int i = 0; i < mediaArray.size(); i++) {
						JsonObject mediaObj = mediaArray.getJsonObject(i);
						String thumbnailUrl = mediaObj.getString("media_url");
						MediaType type = getMediaType(mediaObj.getString("type"));
						String url = null;
						if (type == MediaType.IMAGE) {
							if (mediaType == MediaType.NONE) {
								mediaType = MediaType.IMAGE;
							} else if (mediaType == MediaType.VIDEO) {
								mediaType = MediaType.ANY;
							}
						} else if (type == MediaType.VIDEO) {
							if (mediaType == MediaType.NONE) {
								mediaType = MediaType.VIDEO;
							} else if (mediaType == MediaType.IMAGE) {
								mediaType = MediaType.ANY;
							}
							
							JsonArray variants = mediaObj.getJsonObject("video_info").getJsonArray("variants");
							int maxBitrate = -1;
							for (int j = 0; j < variants.size(); j++) {
								JsonObject variant = variants.getJsonObject(j);
								if (variant.containsKey("bitrate")) {
									int bitrate = variant.getInt("bitrate");
									if (bitrate > maxBitrate) {
										url = variant.getString("url");
									}
								}
							}
						}
						media[i] = new Media(type, url, thumbnailUrl);
					}
					builder.setMedia(media);
				}
			}
			return builder.setMediaType(mediaType).build();
		} catch (JsonException e) {
			logger.error(e);
			return null;
		}
	}
	
	private static String getDisplayText(String textName, JsonObject obj) {
		String text = obj.getString(textName);
		if (obj.containsKey("display_text_range")) {
			JsonArray range = obj.getJsonArray("display_text_range");
			text.substring(range.getInt(0), range.getInt(1));
		}
		return text;
	}
	
	private static MediaType getMediaType(String type) {
		switch (type) {
			case "photo":
				return MediaType.IMAGE;
			case "video":
			case "animated_gif":
				return MediaType.VIDEO;
			default:
				throw new IllegalArgumentException();
		}
	}

}
