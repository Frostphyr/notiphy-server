package com.frostphyr.notiphy.reddit;

import java.util.List;
import java.util.Objects;

import com.google.cloud.firestore.annotation.IgnoreExtraProperties;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.util.TextUtils;

@IgnoreExtraProperties
public class RedditEntry implements Entry {
	
	private static final String[] BLOCKED_SUBREDDITS = {
			"all",
			"popular",
			"frontpage"
	};

	private RedditPostType postType;
	private RedditEntryType entryType;
	private String value;
	private List<String> phrases;
	
	public RedditEntry() {
	}
	
	public RedditPostType getPostType() {
		return postType;
	}
	
	public RedditEntryType getEntryType() {
		return entryType;
	}

	public String getValue() {
		return value;
	}
	
	public List<String> getPhrases() {
		return phrases;
	}

	@Override
	public EntryType getType() {
		return EntryType.REDDIT;
	}

	@Override
	public boolean validate() {
		if (value == null || postType == null || entryType == null ||
				!TextUtils.inRanges(entryType.getCharRanges(), value) ||
				value.length() < 3 || value.length() > entryType.getCharLimit()) {
			return false;
		}
		
		value = value.toLowerCase();
		if (entryType == RedditEntryType.SUBREDDIT) {
			for (String s : BLOCKED_SUBREDDITS) {
				if (s.equals(value)) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof RedditEntry) {
			RedditEntry e = (RedditEntry) o;
			return e.postType == postType &&
					e.entryType == entryType &&
					Objects.equals(e.value, value) &&
					Objects.equals(e.phrases, phrases);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(postType.ordinal(), entryType.ordinal(), value, phrases);
	}
	
	/*@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RedditEntry[");
		builder.append(user != null ? "user=" + user : "subreddit=" + subreddit);
		builder.append(", postType=");
		builder.append(postType);
		if (phrases != null && phrases.length > 0) {
			builder.append(", phrases=String[");
			for (int i = 0; i < phrases.length; i++) {
				builder.append(phrases[i]);
				if (i != phrases.length - 1) {
					builder.append(", ");
				}
			}
			builder.append("]");
		}
		builder.append("]");
		return builder.toString();
	}*/

}
