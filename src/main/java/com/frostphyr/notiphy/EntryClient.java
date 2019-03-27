package com.frostphyr.notiphy;

public interface EntryClient<T extends Processor<?>> {
	
	boolean init(T processor);

}
