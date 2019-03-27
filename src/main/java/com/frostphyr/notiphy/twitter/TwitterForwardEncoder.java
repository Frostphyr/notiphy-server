package com.frostphyr.notiphy.twitter;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.Media;

public class TwitterForwardEncoder {
	
	public static String createForwardMessage(TwitterMessage message) {
		JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("type", EntryType.TWITTER.toString())
				.add("id", message.getId())
				.add("createdAt", message.getCreatedAt())
				.add("username", message.getUsername())
				.add("text", message.getText());
				
		if (message.getMedia() != null) {
			JsonArrayBuilder mediaArrayBuilder = Json.createArrayBuilder();
			for (Media m : message.getMedia()) {
				JsonObjectBuilder mediaBuilder = Json.createObjectBuilder()
						.add("url", m.getUrl())
						.add("type", m.getType().toString());
				if (m.getThumbnailUrl() != null) {
					mediaBuilder.add("thumbnailUrl", m.getThumbnailUrl());
				}
				
				mediaArrayBuilder.add(mediaBuilder);
			}
			builder.add("media", mediaArrayBuilder);
		}
		
		return builder.build().toString();
	}

}
