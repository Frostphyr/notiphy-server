package com.frostphyr.notiphy;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class EntriesDecoder implements Decoder.Text<Entry[]> {
	
	private static final EntryDecoder[] DECODERS = {
		new TwitterDecoder()
	};

	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig config) {
	}

	@Override
	public Entry[] decode(String s) throws DecodeException {
		JsonReader reader = Json.createReader(new StringReader(s));
		JsonArray arr = reader.readArray();
		Entry[] entries = new Entry[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			JsonObject o = arr.getJsonObject(i);
			entries[i] = DECODERS[o.getInt("id")].decode(o);
		}
		return entries;
	}

	@Override
	public boolean willDecode(String s) {
		return true;
	}

}
