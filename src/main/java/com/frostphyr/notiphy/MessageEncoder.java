package com.frostphyr.notiphy;

public interface MessageEncoder<T extends Message> {
	
	String encode(T message);

}
