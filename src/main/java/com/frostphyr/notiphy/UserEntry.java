package com.frostphyr.notiphy;

public class UserEntry<T extends Entry> {
	
	private User user;
	private T entry;
	String uid;
	
	public UserEntry(User user, T entry, String entryUid) {
		this.user = user;
		this.entry = entry;
		this.uid = user.getUid() + ":" + entryUid;
	}
	
	public User getUser() {
		return user;
	}
	
	public T getEntry() {
		return entry;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof UserEntry) {
			return ((UserEntry<?>) o).uid.equals(uid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return uid.hashCode();
	}

}
