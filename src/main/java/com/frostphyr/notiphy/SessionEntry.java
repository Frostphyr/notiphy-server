package com.frostphyr.notiphy;

import java.util.Objects;

import javax.websocket.Session;

public class SessionEntry<T> {
	
	private Session session;
	private T entry;
	
	public SessionEntry(Session session, T entry) {
		this.session = session;
		this.entry = entry;
	}

	public Session getSession() {
		return session;
	}

	public T getEntry() {
		return entry;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof SessionEntry) {
			SessionEntry<?> e = (SessionEntry<?>) o;
			return e.session.getId().equals(session.getId())
					&& e.entry.equals(entry);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(session.getId(), entry);
	}

}
