package com.frostphyr.notiphy.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.apache.http.NameValuePair;

public final class IOUtils {
	
	private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	
	public static Document parseDocument(InputStream in) throws SAXException, IOException, ParserConfigurationException {
		return dbFactory.newDocumentBuilder().parse(in);
	}
	
	public static void writeBodyParameters(HttpURLConnection connection, List<NameValuePair> params) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
			writer.write(getQuery(params));
		}
	}
	
	public static String readString(InputStream in) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (builder.length() > 0) {
					builder.append(System.lineSeparator());
				}
				builder.append(line);
			}
			return builder.toString();
		}
	}
	
	private static String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		String charset = StandardCharsets.UTF_8.name();
		boolean first = true;
		for (NameValuePair p : params) {
			if (first) {
				first = false;
			} else {
				builder.append("&");
			}

			builder.append(URLEncoder.encode(p.getName(), charset));
			builder.append("=");
			builder.append(URLEncoder.encode(p.getValue(), charset));
		}
		return builder.toString();
	}

}
