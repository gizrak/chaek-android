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

import com.gizrak.ebook.core.value.BookInfo;

public class BookInfoSAXParser extends DefaultHandler {

	private static final String TAG = "BookInfoSAXParser";

	private BookInfo bookInfo = new BookInfo();

	private String mStartTag;

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		Log.i(TAG, "[CALLBACK] void startDocument()");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
		Log.i(TAG, "void startElement(uri:" + uri + ", localName:" + localName + ", qName:" + qName
				+ ", attributes:" + attributes + ")");

		mStartTag = localName;

		if ("item".equalsIgnoreCase(localName)) {
			String id = attributes.getValue("id");
			String href = attributes.getValue("href");
			String mediaType = attributes.getValue("media-type");
			Log.d(TAG, "id: " + id);
			Log.d(TAG, "href: " + href);
			Log.d(TAG, "mediaType: " + mediaType);

			bookInfo.addManifest(new BookInfo().new Manifest(id, href, mediaType));

			// save css path
			if ("css".equalsIgnoreCase(id)) {
				bookInfo.setCssPath(href);
			}
		}
		else if ("itemref".equalsIgnoreCase(localName)) {
			String idref = attributes.getValue("idref");
			Log.d(TAG, "idref: " + idref);

			bookInfo.addSpine(idref);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		StringBuffer sb = new StringBuffer();
		for (int i = start; i < start + length; i++) {
			sb.append(ch[i]);
		}
		String text = sb.toString();

		if ("title".equalsIgnoreCase(mStartTag)) {
			Log.d(TAG, "title: " + text);
			bookInfo.setTitle(text);
		}
		else if ("creator".equalsIgnoreCase(mStartTag)) {
			Log.d(TAG, "creator: " + text);
			bookInfo.setAuthor(text);
		}
		else if ("publisher".equalsIgnoreCase(mStartTag)) {
			Log.d(TAG, "publisher: " + text);
			bookInfo.setPublisher(text);
		}
		else if ("identifier".equalsIgnoreCase(mStartTag)) {
			Log.d(TAG, "identifier: " + text);
			bookInfo.setIsbn(text);
		}
		else if ("language".equalsIgnoreCase(mStartTag)) {
			Log.d(TAG, "language: " + text);
			bookInfo.setLanguage(text);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		Log.i(TAG, "void endElement(uri:" + uri + ", localName:" + localName + ", qName:" + qName
				+ ")");

		if (mStartTag.equalsIgnoreCase(localName)) {
			mStartTag = "";
		}
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		Log.i(TAG, "[CALLBACK] void endDocument()");
	}

	/**
	 * Returns BookInfo
	 * 
	 * @param opfPath
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public BookInfo getBookInfo(String opfPath) throws ParserConfigurationException, SAXException,
			IOException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		xr.setContentHandler(this);
		xr.parse(new InputSource(new FileReader(new File(opfPath))));
		return bookInfo;
	}

}
