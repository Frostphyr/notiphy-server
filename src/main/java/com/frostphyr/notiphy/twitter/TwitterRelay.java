package com.frostphyr.notiphy.twitter;

import java.util.List;

import javax.websocket.Session;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryRelay;

public class TwitterRelay implements EntryRelay {

	@Override
	public void add(Session session, List<Entry> entries) {
	}

	@Override
	public void remove(Session session, List<Entry> entries) {
	}

	@Override
	public void removeAll(Session session) {
	}

}
