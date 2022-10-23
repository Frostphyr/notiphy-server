package com.frostphyr.notiphy;

public enum MediaType {
	
	NONE(2),
	IMAGE(3),
	VIDEO(4),
	ANY(1, IMAGE, VIDEO),
	OPTIONAL(0, NONE, IMAGE, VIDEO, ANY);
	
	private static final MediaType[] types = new MediaType[values().length];
	
	private final int id;
	private final MediaType[] validTypes;
	
	static {
		for (MediaType t : values()) {
			types[t.id] = t;
		}
	}
	
	MediaType(int id, MediaType... validTypes) {
		this.id = id;
		this.validTypes = validTypes;
	}
	
	public static MediaType forId(int id) {
		return id < 0 || id >= types.length ? null : types[id];
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isValidFor(MediaType type) {
		if (type == this) {
			return true;
		}
		
		for (MediaType t : validTypes) {
			if (t == type) {
				return true;
			}
		}
		return false;
	}

}
