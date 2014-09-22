package com.gizrak.ebook.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.util.Log;

import com.gizrak.ebook.exception.EbookException;

public class DOMUtil
{
	private static final String TAG = "DOMUtil";

	/**
	 * Returns DOM object by file name
	 * 
	 * @param fileName
	 * @return
	 */
	public static Document getDom(String fileName) throws EbookException
	{
		return getDom(new File(fileName));
	}

	/**
	 * Returns DOM object by file object
	 * 
	 * @param file
	 * @return
	 */
	public static Document getDom(File file) throws EbookException
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(file);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}
		return getDom(fis);
	}

	/**
	 * Returns DOM object by input stream
	 * 
	 * @param istream
	 * @return
	 */
	public static Document getDom(InputStream istream) throws EbookException
	{
		Document doc = null;

		try
		{
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			doc = documentBuilder.parse(istream);

			DOMImplementation domImpl = doc.getImplementation();
			if (domImpl.hasFeature("Core", "2.0"))
			{
				Log.i(TAG, "DOM Core 2.0 is supported.");
			}
			else if (domImpl.hasFeature("Core", "5.0"))
			{
				Log.i(TAG, "DOM Core 5.0 is supported.");
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}
		catch (SAXException e)
		{
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}

		return doc;
	}

}
