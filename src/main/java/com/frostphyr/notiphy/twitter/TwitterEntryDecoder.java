package com.frostphyr.notiphy.twitter;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryDecoder;
import com.frostphyr.notiphy.MediaType;

public class TwitterEntryDecoder implements EntryDecoder {

	@Override
	public Entry decode(JsonObject o) {
		String userId = o.getString("userId");
		MediaType mediaType = MediaType.valueOf(o.getString("mediaType"));
		JsonArray arr = o.getJsonArray("phrases");
		String[] phrases = new String[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			phrases[i] = arr.getString(i);
		}
		return new TwitterEntry(userId, mediaType, phrases);
	}

}
