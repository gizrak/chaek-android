package com.gizrak.ebook.db;

import android.net.Uri;
import android.provider.BaseColumns;

public interface Constants extends BaseColumns {
	
	// Table Names
	public static final String TB_BOOK = "BOOK";
	public static final String TB_TOC = "TOC";
	public static final String TB_BOOKMARK = "Bookmark";

	// Columns in the Chaek database
	public static final String CL_BOOK_TYPE = "book_type";
	public static final String CL_BOOK_TITLE = "book_title";
	public static final String CL_BOOK_AUTHOR = "book_author";
	public static final String CL_BOOK_PUBLISHER = "book_publisher";
	public static final String CL_BOOK_ISBN = "book_isbn";
	public static final String CL_BOOK_LANGUAGE = "book_language";
	public static final String CL_BOOK_IMAGE = "book_image";
	public static final String CL_BOOK_PATH = "book_path";
	public static final String CL_BOOK_CUR_CHAP = "book_curChapter";
	public static final String CL_BOOK_CUR_PAGE = "book_curPage";
	public static final String CL_BOOK_CUR_PCNT = "book_curPercentage";

	public static final String CL_TOC_BOOK_ID = "toc_book_id";
	public static final String CL_TOC_SEQ = "toc_seq";
	public static final String CL_TOC_TITLE = "toc_title";
	public static final String CL_TOC_URL = "toc_url";
	public static final String CL_TOC_ANCHOR = "toc_anchor";
	
	public static final String CL_BM_BOOK_ID = "bm_book_id";
	public static final String CL_BM_CHAPTER = "bm_chapter";
	public static final String CL_BM_PERCENTAGE = "bm_percentage";
	
	// Content Provider
	public static final String AUTHORITY = "com.gizrak.ebook";
	public static final Uri CONTENT_URI_BOOK = Uri.parse("content://" + AUTHORITY + "/" + TB_BOOK);
	public static final Uri CONTENT_URI_TOC = Uri.parse("content://" + AUTHORITY + "/" + TB_TOC);
}
