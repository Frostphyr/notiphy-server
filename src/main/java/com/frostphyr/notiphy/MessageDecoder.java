package com.frostphyr.notiphy;

public interface MessageDecoder<T extends Message> {
	
	T decode(String encodedMessage);

}
