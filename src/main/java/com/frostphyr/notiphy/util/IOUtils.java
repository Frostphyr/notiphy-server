package com.frostphyr.notiphy.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.NameValuePair;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class IOUtils {
	
	public static final String CHARSET = "UTF-8";
	
	private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	
	public static Document parseDocument(String filePath) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilder builder = dbFactory.newDocumentBuilder();
		return builder.parse(new File(filePath));
	}
	
	public static void writeBodyParameters(HttpURLConnection connection, List<NameValuePair> params) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), CHARSET))) {
			writer.write(getQuery(params));
		}
	}
	
	public static String readString(InputStream in) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
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
		boolean first = true;
		for (NameValuePair p : params) {
			if (first) {
				first = false;
			} else {
				builder.append("&");
			}

			builder.append(URLEncoder.encode(p.getName(), CHARSET));
			builder.append("=");
			builder.append(URLEncoder.encode(p.getValue(), CHARSET));
		}
		return builder.toString();
	}

}
