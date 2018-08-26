package com.frostphyr.notiphy;

import javax.json.JsonObject;

public interface EntryDecoder {
	
	Entry decode(JsonObject o);

}
