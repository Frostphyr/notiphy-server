package com.frostphyr.notiphy;

import java.util.HashMap;
import java.util.Map;

public class User {

	private Map<String, UserEntry<?>> entries = new HashMap<>();
	private String uid;
	private String token;
	private long timestamp;
	
	public User(String uid, String token, long timestamp) {
		this.uid = uid;
		updateToken(token, timestamp);
	}
	
	public void updateToken(String token, long timestamp) {
		this.token = token;
		this.timestamp = timestamp;
	}
	
	public void invalidateToken() {
		token = null;
		timestamp = -1;
	}

	public Map<String, UserEntry<?>> getEntries() {
		return entries;
	}
	
	public String getUid() {
		return uid;
	}

	public String getToken() {
		return token;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

}
