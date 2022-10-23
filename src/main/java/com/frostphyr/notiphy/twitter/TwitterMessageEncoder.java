package com.frostphyr.notiphy.twitter;

import com.google.firebase.messaging.Message;

import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.Transformer;

public class TwitterMessageEncoder implements Transformer<Message.Builder, Tweet> {
	
	@Override
	public Message.Builder transform(Tweet tweet) {
		Message.Builder builder = Message.builder()
				.putData("type", EntryType.TWITTER.toString())
				.putData("id", tweet.getId())
				.putData("timestamp", tweet.getTimestamp())
				.putData("username", tweet.getUsername())
				.putData("text", tweet.getText())
				.putData("mature", Boolean.toString(tweet.isMature()));
		if (tweet.getMedia() != null) {
			builder.putData("media_type", tweet.getMedia().getType().toString())
					.putData("media_count", Integer.toString(tweet.getMedia().getCount()))
					.putData("thumbnail_url", tweet.getMedia().getThumbnailUrl())
					.putData("thumbnail_width", Integer.toString(tweet.getMedia().getWidth()))
					.putData("thumbnail_height", Integer.toString(tweet.getMedia().getHeight()));
		}
		return builder;
		/*JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("type", EntryType.TWITTER.toString())
				.add("id", message.getId())
				.add("createdAt", message.getCreatedAt())
				.add("username", message.getUsername())
				.add("text", message.getText())
				.add("nsfw", message.isNsfw());
				
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
		
		return builder.build().toString();*/
	}

}
