package com.frostphyr.notiphy.twitter;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryDecoder;
import com.frostphyr.notiphy.MediaType;

public class TwitterDecoder implements EntryDecoder {

	@Override
	public Entry decode(JsonObject o) {
		long userId = o.getJsonNumber("user_id").longValue();
		MediaType mediaType = MediaType.values()[o.getInt("mediaType")];
		JsonArray arr = o.getJsonArray("phrases");
		String[] phrases = new String[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			phrases[i] = arr.getString(i);
		}
		return new TwitterEntry(userId, mediaType, phrases);
	}

}
