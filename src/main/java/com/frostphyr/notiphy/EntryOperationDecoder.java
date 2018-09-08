package com.frostphyr.notiphy;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.google.common.base.Throwables;

public class EntryOperationDecoder implements Decoder.Text<EntryOperation> {

	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig config) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public EntryOperation decode(String s) throws DecodeException {
		try {
			JsonReader reader = Json.createReader(new StringReader(s));
			JsonObject obj = reader.readObject();
			int operation = obj.getInt("op");
			JsonArray arr = obj.getJsonArray("entries");
			List<Entry>[] entries = new List[EntryType.values().length];
			for (int i = 0; i < arr.size(); i++) {
				JsonObject o = arr.getJsonObject(i);
				int type = o.getInt("type");
				if (entries[type] == null) {
					entries[type] = new ArrayList<Entry>();
				}
				entries[type].add(EntryType.values()[type].getDecoder().decode(o));
			}
			return new EntryOperation(operation, entries);
		} catch (JsonException | NullPointerException e) {
			throw new DecodeException(s, Throwables.getStackTraceAsString(e));
		}
	}

	@Override
	public boolean willDecode(String s) {
		return true;
	}

}
