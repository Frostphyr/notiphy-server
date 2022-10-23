package com.frostphyr.notiphy.reddit;

public enum RedditEntryType {
	
	USER(20, new char[][] {
			{'a', 'z'},
			{'A', 'Z'},
			{'0', '9'},
			{'-'},
			{'_'}
    }),
	
	SUBREDDIT(21, new char[][] {
			{'a', 'z'},
			{'A', 'Z'},
			{'0', '9'},
			{'_'}
    });
	
	private final int charLimit;
	private final char[][] charRanges;
	
	private RedditEntryType(int charLimit, char[][] charRanges) {
		this.charLimit = charLimit;
		this.charRanges = charRanges;
	}
	
	public int getCharLimit() {
		return charLimit;
	}
	
	public char[][] getCharRanges() {
		return charRanges;
	}

}
