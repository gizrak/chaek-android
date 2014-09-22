package com.gizrak.ebook.core.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class CoverImgSAXParser extends DefaultHandler {

	private static final String TAG = "CoverImgSAXParser";

	private boolean isFound = false;
	private String mCoverImgPath;

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		Log.i(TAG, "[CALLBACK] void startDocument()");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
		Log.i(TAG, "void startElement(uri:" + uri + ", localName:" + localName + ", qName:"
				+ qName + ", attributes:" + attributes + ")");

		if (!isFound && "img".equalsIgnoreCase(localName)) {
			mCoverImgPath = attributes.getValue("src");
			Log.d(TAG, "mCoverImgPath: " + mCoverImgPath);
			isFound = true;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		Log.i(TAG, "[CALLBACK] void endDocument()");
	}

	/**
	 * Returns Book Cover Image
	 * 
	 * @param htmlPath
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public String getBookCover(String htmlPath) throws ParserConfigurationException, SAXException,
			IOException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		xr.setContentHandler(this);
		xr.parse(new InputSource(new FileReader(new File(htmlPath))));
		return mCoverImgPath;
	}

}
