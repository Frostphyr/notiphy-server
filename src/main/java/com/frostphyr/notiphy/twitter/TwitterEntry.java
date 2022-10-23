package com.frostphyr.notiphy.twitter;

import java.util.List;
import java.util.Objects;

import com.google.cloud.firestore.annotation.IgnoreExtraProperties;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.MediaType;
import com.frostphyr.notiphy.util.TextUtils;

@IgnoreExtraProperties
public class TwitterEntry implements Entry {
	
	private static final int USER_ID_MAX_LENGTH = 20;
	private static final char[][] USER_ID_CHAR_RANGES = {
			{'0', '9'},
	};
	
	private String userId;
	private MediaType mediaType;
	private List<String> phrases;
	
	public TwitterEntry() {
	}
	
	public String getUserId() {
		return userId;
	}
	
	public MediaType getMediaType() {
		return mediaType;
	}
	
	public List<String> getPhrases() {
		return phrases;
	}

	@Override
	public EntryType getType() {
		return EntryType.TWITTER;
	}

	@Override
	public boolean validate() {
		return userId != null && mediaType != null && userId.length() <= USER_ID_MAX_LENGTH && TextUtils.inRanges(USER_ID_CHAR_RANGES, userId);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TwitterEntry) {
			TwitterEntry e = (TwitterEntry) o;
			return e.userId.equals(userId) &&
					e.mediaType == mediaType &&
					Objects.equals(e.phrases, phrases);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(userId, mediaType, phrases);
	}
	
	/*@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwitterEntry[userId=");
		builder.append(userId);
		builder.append(", mediaType=");
		builder.append(mediaType);
		builder.append(", phrases=String[");
		builder.append(String.join(", ", phrases));
		builder.append("]");
		if (phrases != null && phrases.length > 0) {

			for (int i = 0; i < phrases.length; i++) {
				builder.append(phrases[i]);
				if (i != phrases.length - 1) {
					builder.append(", ");
				}
			}

		}
		builder.append("]");
		return builder.toString();
	}*/

}
