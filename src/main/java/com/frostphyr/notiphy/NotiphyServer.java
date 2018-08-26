package com.frostphyr.notiphy;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(
	value = "/server",
	decoders = {EntriesDecoder.class}
)
public class NotiphyServer {
	
	@OnMessage
	public void onMessage(String message, Session session) {
		
	}
	
	@OnClose
	public void onClose(Session session) {
		
	}

}
