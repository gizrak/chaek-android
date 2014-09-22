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

import com.gizrak.ebook.core.value.TableOfContent;

public class TocSAXParser extends DefaultHandler {

	private static final String TAG = "TocSAXParser";

	private int seq = 0;
	private String mStartTag;
	private String prevUrl;

	private TableOfContent toc = new TableOfContent();
	private TableOfContent.Chapter chapter = new TableOfContent.Chapter();

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		Log.i(TAG, "[CALLBACK] void startDocument()");
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		mStartTag = localName;

		if ("content".equalsIgnoreCase(localName)) {
			String src = attributes.getValue("src");
			int index = src.lastIndexOf("#");

			String url = (index > 0) ? src.substring(0, index) : src;
			String anchor = (index > 0) ? src.substring(index + 1) : "";
			Log.d(TAG, "url: " + url);
			Log.d(TAG, "anchor: " + anchor);

			if (!url.equals(prevUrl)) {
				chapter.setSeq(String.valueOf(++seq));
				chapter.setUrl(url);
				chapter.setAnchor(anchor);

				// add this navPoint to TOC
				toc.addChapter(chapter);
				chapter = new TableOfContent.Chapter();
			}

			prevUrl = url;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);

		StringBuffer sb = new StringBuffer();
		for (int i = start; i < (start + length); i++) {
			sb.append(ch[i]);
		}
		String value = sb.toString();

		if ("text".equalsIgnoreCase(mStartTag)) {
			Log.d(TAG, "title: " + value);
			chapter.setTitle(value);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		super.endElement(uri, localName, qName);

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
	 * Returns TOC
	 * 
	 * @param tocPath
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public TableOfContent getTableOfContents(String tocPath)
			throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		xr.setContentHandler(this);
		xr.parse(new InputSource(new FileReader(new File(tocPath))));
		return toc;
	}

}
