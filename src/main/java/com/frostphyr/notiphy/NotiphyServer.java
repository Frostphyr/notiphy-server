package com.frostphyr.notiphy;

import java.util.List;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(
	value = "/server",
	decoders = {EntryOperationDecoder.class}
)
public class NotiphyServer {
	
	@OnMessage
	public void onMessage(EntryOperation operation, Session session) {
		if (operation != null) {
			for (int i = 0; i < operation.getEntries().length; i++) {
				List<Entry> l = operation.getEntries()[i];
				if (l != null) {
					EntryType type = EntryType.values()[i];
					switch (operation.getOperation()) {
						case EntryOperation.ADD:
							type.getRelay().add(session, l);
							break;
						case EntryOperation.REMOVE:
							type.getRelay().remove(session, l);
							break;
					}
				}
			}
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		for (EntryType t : EntryType.values()) {
			t.getRelay().removeAll(session);
		}
	}

}
