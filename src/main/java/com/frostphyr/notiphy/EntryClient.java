package com.frostphyr.notiphy;

import java.util.concurrent.ExecutorService;

import jakarta.servlet.ServletContext;

public interface EntryClient<E extends Entry> {
	
	String getStatus();
	
	void init(ServletContext context, ExecutorService mainExecutor, MessageDispatcher messageDispatcher) throws Exception;
	
	void shutdown();
	
	void clear();
	
	void update();
	
	void add(UserEntry<E> entry);
	
	void remove(UserEntry<E> entry);

}
