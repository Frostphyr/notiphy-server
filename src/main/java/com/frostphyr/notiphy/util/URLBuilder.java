package com.frostphyr.notiphy.util;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class URLBuilder {
	
	private Map<String, String> queries = new HashMap<String, String>();
	private String domain;
	private String path;
	
	public URLBuilder setDomain(String domain) {
		this.domain = domain;
		return this;
	}
	
	public URLBuilder setPath(String path) {
		this.path = !path.startsWith("/") ? "/" + path : path;
		return this;
	}
	
	public URLBuilder addParameter(String name, String value) {
		queries.put(name, value);
		return this;
	}
	
	public URLBuilder removeParameter(String name) {
		queries.remove(name);
		return this;
	}
	
	public String build() throws MalformedURLException {
		StringBuilder builder = new StringBuilder()
				.append(domain)
				.append(path != null ? path : '/');
		boolean first = true;
		for (Map.Entry<String, String> e : queries.entrySet()) {
			builder.append(first == true ? '?' : '&');
			builder.append(e.getKey());
			builder.append('=');
			builder.append(e.getValue());
			first = false;
		};
		return builder.toString();
	}
	
}
