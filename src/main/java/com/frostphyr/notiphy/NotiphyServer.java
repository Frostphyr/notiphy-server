package com.frostphyr.notiphy;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ServerEndpoint(
	value = "/server",
	decoders = {EntryOperationDecoder.class}
)
public class NotiphyServer {
	
	private static final Logger logger = LogManager.getLogger(NotiphyServer.class);
	
	static {
		for (EntryType e : EntryType.values()) {
			if (!e.getRelay().init()) {
				System.exit(0);
			}
		}
	}
	
	@OnMessage
	public void onMessage(EntryOperation[][] operations, Session session) {
		if (operations != null) {
			for (int i = 0; i < operations.length; i++) {
				EntryType.values()[i].getRelay().performOperations(session, operations[i]);
			}
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		for (EntryType t : EntryType.values()) {
			t.getRelay().removeAll(session);
		}
	}
	
	@OnError
	public void onError(Throwable throwable) {
		logger.error(throwable);
	}

}
