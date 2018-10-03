package com.frostphyr.notiphy;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.google.common.base.Throwables;

public class EntryOperationDecoder implements Decoder.Text<EntryOperation[][]> {

	@Override
	public void destroy() {
	}

	@Override
	public void init(EndpointConfig config) {
	}

	@Override
	public EntryOperation[][] decode(String s) throws DecodeException {
		try {
			JsonReader reader = Json.createReader(new StringReader(s));
			JsonArray operationArray = reader.readArray();
			EntryOperation[][] operations = new EntryOperation[EntryType.values().length][2];
			for (int i = 0; i < operationArray.size(); i++) {
				JsonObject operationObject = operationArray.getJsonObject(i);
				int operationCode = operationObject.getInt("op");
				if (operationCode != EntryOperation.ADD && operationCode != EntryOperation.REMOVE) {
					throw new DecodeException(s, "Invalid operation code: " + operationCode);
				}
				JsonArray arr = operationObject.getJsonArray("entries");
				for (int j = 0; j < arr.size(); j++) {
					JsonObject entryObject = arr.getJsonObject(j);
					EntryType type = EntryType.valueOf(entryObject.getString("type"));
					Entry entry = EntryType.values()[type.ordinal()].getDecoder().decode(entryObject);
					EntryOperation operation = operations[type.ordinal()][operationCode];
					if (operation == null) {
						operation = new EntryOperation(operationCode);
						operations[type.ordinal()][operationCode] = operation;
					}
					operation.getEntries().add(entry);
				}
			}
			return operations;
		} catch (JsonException | NullPointerException e) {
			throw new DecodeException(s, Throwables.getStackTraceAsString(e));
		}
	}

	@Override
	public boolean willDecode(String s) {
		return true;
	}

}
