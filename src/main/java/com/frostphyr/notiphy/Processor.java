package com.frostphyr.notiphy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.websocket.Session;

public abstract class Processor<E extends Entry, M> {
	
	private List<Listener> listeners;
	private MessageDecoder<M> messageDecoder;
	private MessageEncoder<M> messageEncoder;
	
	public Processor(MessageDecoder<M> messageDecoder, MessageEncoder<M> messageEncoder) {
		this.messageDecoder = messageDecoder;
		this.messageEncoder = messageEncoder;
	}
	
	public abstract void performOperations(Session session, EntryOperation[] operations);
	
	public abstract void removeAll(Session session);
	
	protected abstract void processMessage(M message);
	
	protected abstract boolean isMatch(E entry, M message);
	
	public void processMessage(String encodedMessage) {
		M message = messageDecoder.decode(encodedMessage);
		if (message != null) {
			processMessage(message);
		}
	}
	
	public void addListener(Listener listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
	}
	
	public boolean removeListener(Listener listener) {
		return listeners != null ? listeners.remove(listener) : false;
	}
	
	protected void notifyListeners() {
		if (listeners != null) {
			for (Listener l : listeners) {
				l.entriesModified();
			}
		}
	}
	
	protected void processMessage(Collection<SessionEntry<E>> entries, M message) {
		if (entries != null) {
			String encodedMessage = null;
			for (SessionEntry<E> e : entries) {
				if (isMatch(e.getEntry(), message)) {
					if (encodedMessage == null) {
						encodedMessage = messageEncoder.encode(message);
					}
					
					try {
						e.getSession().getBasicRemote().sendText(encodedMessage);
					} catch (IOException ex) {
					}
				}
			}
		}
	}
	
	public interface Listener {
		
		void entriesModified();
		
	}

}
