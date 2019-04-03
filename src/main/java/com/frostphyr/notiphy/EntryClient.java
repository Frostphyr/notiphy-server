package com.frostphyr.notiphy;

import java.io.IOException;
import java.util.List;

public abstract class EntryClient {
	
	private MessageDecoder<?> messageDecoder;
	private MessageEncoder<?> messageEncoder;
	private EntryCollection<?, ?> entries;
	
	public EntryClient(MessageDecoder<?> messageDecoder, MessageEncoder<?> messageEncoder, EntryCollection<?, ?> entries) {
		this.messageDecoder = messageDecoder;
		this.messageEncoder = messageEncoder;
		this.entries = entries;
	}
	
	public abstract boolean init();
	
	public EntryCollection<?, ?> getEntries() {
		return entries;
	}
	
	protected <E extends Entry, M extends Message> void processMessage(M message) {
		if (message != null) {
			List<SessionEntry<E>> entries = getMatches(message);
			if (entries != null && entries.size() > 0) {
				String sendText = encode(messageEncoder, message);
				for (SessionEntry<?> e : entries) {
					try {
						e.getSession().getBasicRemote().sendText(sendText);
					} catch (IOException ex) {
					}
				}
			}
		}
	}
	
	protected void processEncodedMessage(String encodedMessage) {
		processMessage(decode(encodedMessage));
	}
	
	@SuppressWarnings("unchecked")
	protected <M extends Message> M decode(String encodedMessage) {
		return (M) decode(messageDecoder, encodedMessage);
	}
	
	private <M extends Message> M decode(MessageDecoder<M> decoder, String encodedMessage) {
		return decoder.decode(encodedMessage);
	}
	
	@SuppressWarnings("unchecked")
	private <M extends Message> String encode(MessageEncoder<?> encoder, M message) {
		return ((MessageEncoder<M>) encoder).encode(message);
	}
	
	@SuppressWarnings("unchecked")
	private <E extends Entry, M extends Message> List<SessionEntry<E>> getMatches(M message) {
		EntryCollection<E, M> entries = (EntryCollection<E, M>) this.entries;
		return entries.getMatches(message);
	}

}
