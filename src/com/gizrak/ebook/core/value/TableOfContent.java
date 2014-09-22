package com.gizrak.ebook.core.value;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Table of Content
 */
public class TableOfContent implements Serializable {

	private static final long serialVersionUID = -8757154805331298337L;

	private String uid;
	private String depth;
	private ArrayList<Chapter> chapterList = new ArrayList<Chapter>();

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getDepth() {
		return depth;
	}

	public void setDepth(String depth) {
		this.depth = depth;
	}

	public void addChapter(final Chapter chapter) {
		this.chapterList.add(chapter);
	}

	public Chapter getChapter(final int num) {
		int chapNum = num;
		if (chapNum >= chapterList.size()) {
			chapNum = chapterList.size() - 1;
		}
		return chapterList.get(chapNum);
	}

	public ArrayList<Chapter> getChapterList() {
		return chapterList;
	}

	public String[] getTitleArray() {
		int totalSize = getTotalSize();
		String[] titles = new String[totalSize];
		for (int i = 0; i < totalSize; i++) {
			Chapter chap = chapterList.get(i);
			titles[i] = chap.getTitle();
		}
		return titles;
	}

	public String[] getUrlArray() {
		int totalSize = getTotalSize();
		String[] urls = new String[totalSize];
		for (int i = 0; i < totalSize; i++) {
			Chapter chap = chapterList.get(i);
			urls[i] = chap.getUrl();
		}
		return urls;
	}

	public int getTotalSize() {
		return chapterList.size();
	}

	/**
	 * Chapter
	 */
	public static class Chapter {

		String seq;
		String title;
		String url;
		String anchor;

		public String getSeq() {
			return seq;
		}

		public void setSeq(String seq) {
			this.seq = seq;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getTitle() {
			return title;
		}

		public String getUrl() {
			return url;
		}

		public String getAnchor() {
			return anchor;
		}

		public void setAnchor(String anchor) {
			this.anchor = anchor;
		}
	}

	@Override
	public String toString() {
		return "TableOfContent [uid=" + uid + ", depth=" + depth + ", chapterList=" + chapterList
				+ "]";
	}

}
