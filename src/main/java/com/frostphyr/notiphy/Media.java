package com.frostphyr.notiphy;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Media {
	
	public static final int PREFERRED_WIDTH = 1024;
	
	public static boolean isPreferredWidth(int oldWidth, int newWidth) {
		return oldWidth == 0 || newWidth > oldWidth && oldWidth < PREFERRED_WIDTH || newWidth > PREFERRED_WIDTH && newWidth < oldWidth;
	}
	
	private MediaType type;
	private String thumbnailUrl;
	private int width;
	private int height;
	private int count;
	
	public Media(MediaType type, String thumbnailUrl, int width, int height, int count) {
		this.type = type;
		this.thumbnailUrl = thumbnailUrl;
		this.width = width;
		this.height = height;
		this.count = count;
	}

	public Media(MediaType type, String thumbnailUrl, int width, int height) {
		this(type, thumbnailUrl, width, height, 1);
	}
	
	public MediaType getType() {
		return type;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getCount() {
		return count;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("type", type)
				.append("thumbnailUrl", thumbnailUrl)
				.append("width", width)
				.append("height", height)
				.append("count", count)
				.build();
	}
	

}
