package com.gizrak.ebook.core.value;

import java.util.ArrayList;
import java.util.Arrays;

public class BookInfo {

	/**
	 * Book Types<br>
	 * <ul>
	 * <li>EPUB
	 * <li>TEXT
	 * </ul>
	 */
	public static enum BOOK_TYPE {
		EPUB, TEXT, HTML
	}

	private long _id;
	private BOOK_TYPE bookType;
	private String title;
	private String author;
	private String publisher;
	private String date;
	private String subject;
	private String language;
	private String right;
	private String isbn;
	private byte[] coverImg;
	private String path;

	private String cssPath;

	private ArrayList<Manifest> manifestList = new ArrayList<BookInfo.Manifest>();
	private ArrayList<String> spineList = new ArrayList<String>();

	public class Manifest {
		public String id;
		public String href;
		public String mediaType;

		public Manifest(String id, String href, String mediaType) {
			this.id = id;
			this.href = href;
			this.mediaType = mediaType;
		}
	}

	public long getId() {
		return _id;
	}

	public void setId(long _id) {
		this._id = _id;
	}

	public BOOK_TYPE getBookType() {
		return bookType;
	}

	public void setBookType(BOOK_TYPE bookType) {
		this.bookType = bookType;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public byte[] getCoverImg() {
		return coverImg;
	}

	public void setCoverImg(byte[] coverImg) {
		this.coverImg = coverImg;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCssPath() {
		return cssPath;
	}

	public void setCssPath(String cssPath) {
		this.cssPath = cssPath;
	}

	public Manifest getManifestItem(int i) {
		return manifestList.get(i);
	}

	public ArrayList<Manifest> getManifestList() {
		return manifestList;
	}

	public void addManifest(Manifest manifest) {
		manifestList.add(manifest);
	}

	public String getSpineItem(int i) {
		return spineList.get(i);
	}

	public ArrayList<String> getSpineList() {
		return spineList;
	}

	public void addSpine(String spine) {
		spineList.add(spine);
	}

	@Override
	public String toString() {
		return "BookInfo [_id=" + _id + ", bookType=" + bookType + ", title=" + title + ", author="
				+ author + ", publisher=" + publisher + ", date=" + date + ", subject=" + subject
				+ ", language=" + language + ", right=" + right + ", isbn=" + isbn + ", coverImg="
				+ Arrays.toString(coverImg) + ", path=" + path + ", cssPath=" + cssPath
				+ ", manifestList=" + manifestList + ", spineList=" + spineList + "]";
	}

}
