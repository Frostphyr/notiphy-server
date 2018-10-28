package com.frostphyr.notiphy;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class IOUtils {
	
	private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	
	public static Document parseDocument(String filePath) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilder builder = dbFactory.newDocumentBuilder();
		return builder.parse(new File(filePath));
	}

}
