package com.frostphyr.notiphy;

public interface Transformer<R, P> {
	
	R transform(P param);

}
