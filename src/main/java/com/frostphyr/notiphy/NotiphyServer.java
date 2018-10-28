package com.frostphyr.notiphy;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
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
	
	private HeartbeatManager heartbeatManager = new HeartbeatManager();
	
	static {
		for (EntryType e : EntryType.values()) {
			if (!e.getRelay().init()) {
				System.exit(0);
			}
		}
	}
	
	@OnOpen
	public void onOpen(Session session) {
		heartbeatManager.start(session);
	}
	
	@OnMessage
	public void onMessage(Session session, EntryOperation[][] operations) {
		if (operations != null) {
			for (int i = 0; i < operations.length; i++) {
				EntryType.values()[i].getRelay().performOperations(session, operations[i]);
			}
		}
	}
	
	@OnMessage
	public void onMessage(Session session, PongMessage message) {
		heartbeatManager.onPong(session, message);
	}
	
	@OnClose
	public void onClose(Session session) {
		heartbeatManager.stop(session);
		for (EntryType t : EntryType.values()) {
			t.getRelay().removeAll(session);
		}
	}
	
	@OnError
	public void onError(Throwable throwable) {
		logger.error(throwable);
	}

}
