package com.frostphyr.notiphy;

public interface MessageEncoder<T> {
	
	String encode(T message);

}
