package com.frostphyr.notiphy;

public interface MessageDecoder<T> {
	
	T decode(String encodedMessage);

}
