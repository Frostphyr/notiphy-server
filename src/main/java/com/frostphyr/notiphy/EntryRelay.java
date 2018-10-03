package com.frostphyr.notiphy;

import javax.websocket.Session;

public interface EntryRelay {
	
	boolean init();
	
	void performOperations(Session session, EntryOperation[] operations);
	
	void removeAll(Session session);

}
