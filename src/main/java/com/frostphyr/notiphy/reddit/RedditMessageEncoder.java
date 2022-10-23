package com.frostphyr.notiphy.reddit;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.Message;

import com.frostphyr.avail.text.SurrogatePairPolicy;
import com.frostphyr.avail.text.TextUtils;
import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.Transformer;

public class RedditMessageEncoder implements Transformer<Message.Builder, RedditPost> {

	private static final int MAX_TEXT_LENGTH = 1000;

	@Override
	public Message.Builder transform(RedditPost post) {
		Message.Builder builder = Message.builder()
						.setAndroidConfig(AndroidConfig.builder()
								.setPriority(AndroidConfig.Priority.HIGH).build())
				.putData("type", EntryType.REDDIT.toString())
				.putData("subreddit", post.getIdentifier().getSubreddit())
				.putData("user", post.getUser())
				.putData("title", post.getTitle())
				.putData("timestamp", post.getTimestamp())
				.putData("url", post.getUrl())
				.putData("mature", Boolean.toString(post.isMature()));
		
		String text = post.getText();
		if (text != null) {
			if (text.length() > MAX_TEXT_LENGTH) {
				text = TextUtils.substring(text, 0, MAX_TEXT_LENGTH, SurrogatePairPolicy.KEEP);
			}
			builder.putData("text", text);
		} else if (post.getMedia() != null) {
			builder.putData("media_type", post.getMedia().getType().toString())
					.putData("thumbnail_url", post.getMedia().getThumbnailUrl())
					.putData("thumbnail_width", Integer.toString(post.getMedia().getWidth()))
					.putData("thumbnail_height", Integer.toString(post.getMedia().getHeight()));
		}
		return builder;
	}

}
