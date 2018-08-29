package com.frostphyr.notiphy;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class EntryOperationDecoder implements Decoder.Text<EntryOperation> {
	
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
	public EntryOperation decode(String s) throws DecodeException {
		JsonReader reader = Json.createReader(new StringReader(s));
		JsonObject obj = reader.readObject();
		int operation = obj.getInt("op");
		JsonArray arr = obj.getJsonArray("entries");
		Entry[] entries = new Entry[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			JsonObject o = arr.getJsonObject(i);
			entries[i] = DECODERS[o.getInt("type")].decode(o);
		}
		return new EntryOperation(operation, entries);
	}

	@Override
	public boolean willDecode(String s) {
		return true;
	}

}
