package com.gizrak.ebook.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gizrak.ebook.PrefActivity;
import com.gizrak.ebook.core.parser.BookInfoSAXParser;
import com.gizrak.ebook.core.parser.CoverImgSAXParser;
import com.gizrak.ebook.core.parser.TocSAXParser;
import com.gizrak.ebook.core.value.BookInfo;
import com.gizrak.ebook.core.value.BookInfo.BOOK_TYPE;
import com.gizrak.ebook.core.value.BookInfo.Manifest;
import com.gizrak.ebook.core.value.TableOfContent;
import com.gizrak.ebook.exception.EbookException;
import com.gizrak.ebook.utils.DOMUtil;
import com.gizrak.ebook.utils.ZipUtil;

public class EpubEngine {

	private static final String TAG = "EpubEngine";

	private BOOK_TYPE mBookType;
	private String mEpubPath;
	private String mUnzipPath;
	private String mContentPath;
	private String mBaseUrl;
	private String mTocPath;

	/**
	 * Constructor
	 * 
	 * @param bookType
	 * @param bookPath
	 * @throws EbookException
	 */
	public EpubEngine(BOOK_TYPE bookType, String bookPath) throws EbookException {
		Log.i(TAG, "[METHOD] EpubEngine(bookType:" + bookType + ", bookPath:" + bookPath + ")");

		// get epub path
		mEpubPath = bookPath;
		Log.d(TAG, "mEpubPath: " + mEpubPath);

		// get book type
		mBookType = bookType;
		Log.d(TAG, "mBookType: " + mBookType);

		/*
		 * epub book
		 */
		if (bookType == BOOK_TYPE.EPUB) {
			// 1. get unzip path
			String epubDir = mEpubPath.substring(0, mEpubPath.lastIndexOf("/") + 1);
			String epubName = mEpubPath.substring(mEpubPath.lastIndexOf("/") + 1, mEpubPath.lastIndexOf("."));
			mUnzipPath = epubDir + "." + epubName;
			Log.d(TAG, "mUnzipPath: " + mUnzipPath);

			// unzip if not exists
			File unzipDir = new File(mUnzipPath);
			if (!unzipDir.exists()) {
				try {
					ZipUtil.unzipAll(new File(bookPath), unzipDir);
				}
				catch (IOException e) {
					e.printStackTrace();
					throw new EbookException(e.getMessage());
				}
			}

			// 2. get content path
			try {
				String containerPath = mUnzipPath + "/META-INF/container.xml";
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser xpp = factory.newPullParser();
				xpp.setInput(new FileReader(new File(containerPath)));

				int eventType = xpp.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_TAG) {
						String tagName = xpp.getName();
						if ("rootfile".equalsIgnoreCase(tagName)) {
							mContentPath = mUnzipPath + "/" + xpp.getAttributeValue(null, "full-path");
							Log.d(TAG, "mContentPath: " + mContentPath);
							break;
						}
					}
					eventType = xpp.next();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new EbookException(e.getMessage());
			}
			catch (XmlPullParserException e) {
				e.printStackTrace();
				throw new EbookException(e.getMessage());
			}

			// 3. get base url
			mBaseUrl = mContentPath.substring(0, mContentPath.lastIndexOf("/") + 1);
			Log.d(TAG, "mBaseUrl: " + mBaseUrl);

			// 4. get toc path
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder;
				builder = factory.newDocumentBuilder();
				Document doc = builder.parse(new File(mContentPath));

				// get ncx element
				Element elmtNcx = (Element) doc.getElementById("ncx");
				mTocPath = mBaseUrl + elmtNcx.getAttributeNode("href").getValue();
				Log.d(TAG, "mTocPath: " + mTocPath);
			}
			catch (IOException e) {
				e.printStackTrace();
				throw new EbookException(e.getMessage());
			}
			catch (ParserConfigurationException e) {
				e.printStackTrace();
				throw new EbookException(e.getMessage());
			}
			catch (SAXException e) {
				e.printStackTrace();
				throw new EbookException(e.getMessage());
			}
		}
		/*
		 * text book
		 */
		else if (bookType == BOOK_TYPE.TEXT) {
			// 1. get unzip path
			mUnzipPath = "";
			Log.d(TAG, "mUnzipPath: " + mUnzipPath);

			// 2. get content path
			mContentPath = "";
			Log.d(TAG, "mContentPath: " + mContentPath);

			// 3. get base url
			String title = bookPath.substring(bookPath.lastIndexOf("/") + 1, bookPath.lastIndexOf("."));
			mBaseUrl = bookPath.substring(0, bookPath.lastIndexOf("/") + 1) + "." + title;
			Log.d(TAG, "mBaseUrl: " + mBaseUrl);

			// 4. get toc path
			mTocPath = "";
			Log.d(TAG, "mTocPath: " + mTocPath);
		}
		/*
		 * html book
		 */
		else if (bookType == BOOK_TYPE.HTML) {
			// 1. get unzip path
			mUnzipPath = "";
			Log.d(TAG, "mUnzipPath: " + mUnzipPath);

			// 2. get content path
			mContentPath = "";
			Log.d(TAG, "mContentPath: " + mContentPath);

			// 3. get base url
			mBaseUrl = bookPath.substring(0, bookPath.lastIndexOf("/"));
			Log.d(TAG, "mBaseUrl: " + mBaseUrl);

			// 4. get toc path
			mTocPath = "";
			Log.d(TAG, "mTocPath: " + mTocPath);
		}
		else {
			throw new EbookException("Unsupported format. Only epub, txt can be used.");
		}
	}

	/**
	 * Parses content.opf
	 * 
	 * @return
	 * @throws EbookException
	 */
	public BookInfo parseBookInfo() throws EbookException {
		Log.i(TAG, "[METHOD] BookInfo parseBookInfo()");
		BookInfo bookInfo = new BookInfo();

		BookInfoSAXParser bookInfoParser = new BookInfoSAXParser();
		try {
			bookInfo = bookInfoParser.getBookInfo(mContentPath);
			bookInfo.setBookType(BOOK_TYPE.EPUB);
			bookInfo.setPath(mEpubPath);

			// cover image
			String imgHtml = "";
			String idref = bookInfo.getSpineItem(0);
			ArrayList<Manifest> manifestList = bookInfo.getManifestList();

			for (int i = 0; i < manifestList.size(); i++) {
				Manifest manifest = manifestList.get(i);

				if (idref.equals(manifest.id)) {
					imgHtml = mBaseUrl + "/" + manifest.href;
					Log.d(TAG, "imgHtml: " + imgHtml);
					break;
				}
			}

			// check html file
			if (imgHtml == null || "".equals(imgHtml)) {
				bookInfo.setCoverImg(null);
			}
			else {
				// parse cover image html
				CoverImgSAXParser coverImgParser = new CoverImgSAXParser();
				String imgPath = coverImgParser.getBookCover(imgHtml);

				if (imgPath != null && !"".equals(imgPath)) {
					imgPath = mBaseUrl + "/" + imgPath;

					// img file to byte[]
					byte[] imgBytes = FileUtils.readFileToByteArray(new File(imgPath));
					bookInfo.setCoverImg(imgBytes);
				}
				else {
					bookInfo.setCoverImg(null);
				}
			}
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}
		catch (SAXException e) {
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}

		return bookInfo;
	}

	/**
	 * Parses toc.ncx
	 * 
	 * @return
	 * @throws EbookException
	 */
	public TableOfContent parseTableOfContent() throws EbookException {
		Log.i(TAG, "[METHOD] void parseTableOfContent()");
		TableOfContent toc = new TableOfContent();

		TocSAXParser parser = new TocSAXParser();
		try {
			toc = parser.getTableOfContents(mTocPath);
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}
		catch (SAXException e) {
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}

		return toc;
	}

	/**
	 * Gets Base URL
	 * 
	 * @return
	 */
	public String getBaseUrl() {
		return mBaseUrl;
	}

	/**
	 * Gets ePub Unzip Path
	 * 
	 * @return
	 */
	public String getUnzipPath() {
		return mUnzipPath;
	}

	/**
	 * Returns input stream for each type of book
	 * 
	 * @return
	 */
	public InputStream getChapterInputStream() {
		InputStream istream = null;
		switch (mBookType) {
		case EPUB:
			break;

		case TEXT:
			break;

		case HTML:
			break;

		default:
			break;
		}
		return istream;
	}

	/**
	 * Preprocess
	 * 
	 * @param chapter
	 * @param width
	 * @param height
	 * @return
	 * @throws EbookException
	 */
	public static String preprocess(String chapter, int width, int height, Context ctx) throws EbookException {
		Log.i(TAG, "[METHOD] String preprocess(chapter:" + chapter + ", width:" + width + ", height:" + height
				+ ", ctx:" + ctx + ")");

		/*
		 * 1. prepare dom
		 */
		// get dom
		Document doc = null;
		try {
			doc = DOMUtil.getDom(chapter);
		}
		catch (EbookException e) {
			e.printStackTrace();
			throw new EbookException(e.getMessage());
		}

		/*
		 * 2. handle dom tree
		 */
		NodeList nodeList = doc.getElementsByTagName("*");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Document.ELEMENT_NODE) {
				/*
				 * <head> Element
				 * 
				 * 1. append monocle library (CORE)
				 * 2. append monocle library (FLIPPERS)
				 * 3. append monocle library (CONTROLS)
				 * 4. set font face
				 */
				if ("head".equalsIgnoreCase(node.getNodeName())) {
					Element headElement = (Element) node;

					// 1. append monocle library (CORE)
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/monocle.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/compat.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/reader.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/book.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/component.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/place.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/styles.js");

					// 2. append monocle library (FLIPPERS)
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/flippers/slider.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/flippers/legacy.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/flippers/instant.js");

					// 3. append monocle library (CONTROLS)
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/controls/spinner.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/controls/magnifier.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/controls/scrubber.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/controls/placesaver.js");
					EpubEngine.addJavaScriptLink(doc, headElement, "monocle/controls/contents.js");

					// append monocle interface script
					EpubEngine.addJavaScriptLink(doc, headElement, "javascript/interface.js");

					// 4. set font face
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
					// String fontType = ctx.getSharedPreferences("font_type",
					// Context.MODE_PRIVATE).getString("fontType", "default font here...");
					String fontType = pref.getString(PrefActivity.KEY_FONT_TYPE, "DroidSans");
					Log.d(TAG, "fontType: " + fontType);

					// set css style for font-type
					Element cssElement = doc.createElement("style");
					cssElement.setAttribute("type", "text/css");
					cssElement.appendChild(doc.createComment("@font-face { font-family:custom_font; src:url('"
							+ fontType + "'); }"));

					headElement.appendChild(cssElement);
				}
				/*
				 * <body> Element
				 * 
				 * 1. insert div for monocle
				 * 2. set font size
				 */
				else if ("body".equalsIgnoreCase(node.getNodeName())) {
					Element bodyElement = (Element) node;

					// 1. insert div for monocle
					Element divElement = doc.createElement("div");
					divElement.setAttribute("id", "reader");
					divElement.setAttribute("style", "width:" + width + "px; height:" + height
							+ "px; border:none; overflow:hidden;");

					NodeList bodyChildList = bodyElement.getChildNodes();
					for (int j = 0; j < bodyChildList.getLength(); j++) {
						Node bodyChild = (Node) bodyChildList.item(j);
						divElement.appendChild(bodyChild);
					}
					bodyElement.appendChild(divElement);

					// 2. clear attributes
					bodyElement.removeAttribute("xml:lang");

					// 3. set font size
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
					String fontSize = pref.getString(PrefActivity.KEY_FONT_SIZE, "medium");
					Log.d(TAG, "fontSize: " + fontSize);

					// 4. day/night mode
					String dayNightMode = pref.getString(PrefActivity.KEY_DAY_NIGHT_MODE, "Day");
					Log.d(TAG, "dayNightMode: " + dayNightMode);

					String dayNightOption = "";
					if ("Night".equalsIgnoreCase(dayNightMode)) {
						dayNightOption = "color:#999999; background-color:#000000;";
					}
					else {
						dayNightOption = "background-color:#FFFFFF;";
					}

					// 5. set body style
					bodyElement.setAttribute("style",
							"margin:0 0 10 0; padding:0; line-height:1.5em; font-family:custom_font; font-size:"
									+ fontSize + "; " + dayNightOption);
				}
				/*
				 * <img> Element
				 * 
				 * 1. image max size
				 */
				else if ("img".equalsIgnoreCase(node.getNodeName())) {
					Element imgElement = (Element) node;

					// 1. image max size
					int maxImageWidth = width - 30;
					int maxImageHeight = height - 80;
					Log.d(TAG, "maxImageWidth: " + maxImageWidth);
					Log.d(TAG, "maxImageHeight: " + maxImageHeight);

					imgElement.setAttribute("style", "max-width:" + maxImageWidth + "px; max-height:" + maxImageHeight
							+ "px;");
				}
			}
		}

		/*
		 * 3. DOM to string
		 */
		StringWriter outText = new StringWriter();
		StreamResult sr = new StreamResult(outText);

		Properties oprops = new Properties();
		oprops.put(OutputKeys.METHOD, "html");
		// oprops.put("indent-amount", "4");

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = null;
		try {
			trans = tf.newTransformer();
			trans.setOutputProperties(oprops);
			trans.transform(new DOMSource(doc), sr);
		}
		catch (TransformerConfigurationException e) {
			e.printStackTrace();
		}
		catch (TransformerException e) {
			e.printStackTrace();
		}

		return outText.toString();
	}

	/**
	 * Add External Javascript Src
	 * 
	 * @param doc
	 * @param headElement
	 * @param path
	 */
	private static void addJavaScriptLink(Document doc, Element headElement, String path) {
		Element scriptElement = doc.createElement("script");
		scriptElement.setAttribute("type", "text/javascript");
		scriptElement.setAttribute("src", "url('file:///android_asset/" + path + "')");
		headElement.appendChild(scriptElement);
		headElement.appendChild(doc.createTextNode("\n"));
	}

	/**
	 * Creates separated HTML files from TEXT file
	 * 
	 * @param textPath
	 * @param baseUrl
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static String makeHtml(String textPath) throws FileNotFoundException, IOException {
		Log.i(TAG, "[METHOD] ArrayList<String> makeFileTextToHtml(textPath:" + textPath + ")");

		// generate html files
		StringBuffer sb = new StringBuffer();
		// sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
		// sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n");
		sb.append("<html>\n");
		sb.append("<head>\n");
		// sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n");
		sb.append("</head>\n");
		sb.append("<body>\n");
		sb.append("<p>");

		String lineStr = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(textPath), "euc-kr"));

		while ((lineStr = br.readLine()) != null) {
			// replace reference values
			lineStr = lineStr.replace("<", "&lt;");
			lineStr = lineStr.replace(">", "&gt;");
			lineStr = lineStr.replace("&", "&amp;");

			// append p tags
			if (lineStr.length() == 0 || "".equals(lineStr.trim())) {
				sb.append("</p>\n<p>");
			}
			sb.append(lineStr);
		}

		sb.append("</p>\n");
		sb.append("</body>\n");
		sb.append("</html>\n");

		return sb.toString();
	}

	/**
	 * Creates separated HTML files from TEXT file
	 * 
	 * @param textPath
	 * @param baseUrl
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<String> makeFileTextToHtml(String textPath, String baseUrl) throws FileNotFoundException,
			IOException {
		Log.i(TAG, "[METHOD] ArrayList<String> makeFileTextToHtml(textPath:" + textPath + ", baseUrl:" + baseUrl + ")");
		ArrayList<String> mGeneratedHtmlFiles = new ArrayList<String>();

		// make baseUrl
		new File(baseUrl).mkdirs();

		// generate html files
		StringBuffer sbHeader = new StringBuffer();
		sbHeader.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
		sbHeader.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n");
		sbHeader.append("<html>\n");
		sbHeader.append("<head>\n");
		sbHeader.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n");
		sbHeader.append("</head>\n");
		sbHeader.append("<body>\n");
		sbHeader.append("<p>");
		String header = sbHeader.toString();

		StringBuffer sbTail = new StringBuffer();
		sbTail.append("</p>\n");
		sbTail.append("</body>\n");
		sbTail.append("</html>\n");
		String tail = sbTail.toString();

		int lineCount = 0;
		int fileCount = 0;
		String lineStr = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(textPath), "euc-kr"));
		StringBuffer sbContent = new StringBuffer();

		while ((lineStr = br.readLine()) != null) {
			// replace reference values
			lineStr = lineStr.replace("&", "&amp;");
			lineStr = lineStr.replace("<", "&lt;");
			lineStr = lineStr.replace(">", "&gt;");

			if (lineCount == 0) {
				sbContent.append(lineStr);
				lineCount++;
				continue;
			}

			// append p tags
			if (lineStr.length() == 0 || "".equals(lineStr.trim())) {
				sbContent.append("</p>\n<p>");
			}
			sbContent.append(lineStr);

			// create file
			if (lineCount >= 300) {
				String genFile = "generated_" + String.format("%05d", (++fileCount)) + ".html";
				FileWriter fw = new FileWriter(new File(baseUrl + "/" + genFile));
				fw.write(header);
				fw.write(sbContent.toString());
				fw.write(tail);
				fw.close();

				String url = baseUrl + "/" + genFile;
				mGeneratedHtmlFiles.add(genFile);
				Log.d(TAG, url + " was created.");

				lineCount = 0;
				sbContent.delete(0, sbContent.length());

				continue;
			}

			lineCount++;
		}

		// create file
		String genFile = "generated_" + String.format("%05d", (++fileCount)) + ".html";
		FileWriter fw = new FileWriter(new File(baseUrl + "/" + genFile));
		fw.write(header);
		fw.write(sbContent.toString());
		fw.write(tail);
		fw.close();

		String url = baseUrl + "/" + genFile;
		mGeneratedHtmlFiles.add(genFile);
		Log.d(TAG, url + " was created.");

		lineCount = 0;
		sbContent.delete(0, sbContent.length());

		br.close();

		return mGeneratedHtmlFiles;
	}

}
