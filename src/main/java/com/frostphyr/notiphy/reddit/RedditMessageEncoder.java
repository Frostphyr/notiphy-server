package com.frostphyr.notiphy.reddit;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.MessageEncoder;

public class RedditMessageEncoder implements MessageEncoder<RedditMessage.Post> {

	@Override
	public String encode(RedditMessage.Post message) {
		JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("type", EntryType.REDDIT.toString())
				.add("subreddit", message.getIdentifier().getSubreddit())
				.add("user", message.getUser())
				.add("title", message.getTitle())
				.add("createdAt", message.getCreatedAt())
				.add("url", message.getUrl())
				.add("nsfw", message.isNsfw());
		if (message.getLink() == null) {
			builder.add("text", message.getText());
		} else {
			builder.add("thumbnailUrl", message.getThumbnailUrl())
					.add("link", message.getLink())
					.add("video", message.isVideo());
		}
		return builder.build().toString();
	}

}
