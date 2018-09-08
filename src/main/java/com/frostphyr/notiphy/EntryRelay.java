package com.frostphyr.notiphy;

import java.util.List;

import javax.websocket.Session;

public interface EntryRelay {
	
	boolean init();
	
	void add(Session session, List<Entry> entries);
	
	void remove(Session session, List<Entry> entries);
	
	void removeAll(Session session);

}
