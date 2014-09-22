package com.gizrak.ebook;

import android.R.color;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.gizrak.ebook.constant.Extras;
import com.gizrak.ebook.constant.Request;
import com.gizrak.ebook.core.EpubEngine;
import com.gizrak.ebook.core.value.BookInfo;
import com.gizrak.ebook.core.value.BookInfo.BOOK_TYPE;
import com.gizrak.ebook.core.value.TableOfContent;
import com.gizrak.ebook.core.value.TableOfContent.Chapter;
import com.gizrak.ebook.db.ChaekHelper;
import com.gizrak.ebook.db.Constants;
import com.gizrak.ebook.exception.EbookException;
import com.gizrak.ebook.utils.StringUtil;

public class EpubActivity extends Activity implements Constants {

	private static final String TAG = "EpubActivity";

	private static final int SCREEN_WIDTH_CORRECTION = 5;
	private static final int SCREEN_HEIGHT_CORRECTION = 2;

	private static final int DECISION_FLING_HORIZONTAL = 100;

	private static final int HANDLER_JS_CURPAGE = 1;
	private static final int HANDLER_JS_TOTPAGE = 2;
	private static final int HANDLER_SEEKBAR_CHANGING = 3;
	private static final int HANDLER_SEEKBAR_CHANGED = 4;
	private static final int HANDLER_SHOW_OSD = 5;
	private static final int HANDLER_HIDE_OSD = 6;
	private static final int HANDLER_PAGING = 7;
	private static final int HANDLER_TOAST_SHORT = 8;
	private static final int HANDLER_TOAST_LONG = 9;
	private static final int HANDLER_WHAT_OTHERS = 0;

	static class DisplayInfo {
		static float density;
		static int densityDpi;
		static int widthPixels;
		static int heightPixels;
		static float scaledDensity;
		static float xdpi;
		static float ydpi;
	}

	// Members
	private int mBookWidth;
	private int mBookHeight;

	private int mCurChapter = 1;
	private int mCurPage = 1;
	private float mCurPercentage = 0.0f;
	private int mMaxPage = 1;
	private int[] mPageList;

	private boolean isOsdOn = false;

	private EpubEngine mEngine;
	private BookInfo mBook = new BookInfo();
	private TableOfContent mToc = new TableOfContent();

	// bookmarks
	private long mBookmarkChapter[];
	private int mBookmarkPage[];
	private float mBookmarkPercentage[];
	private boolean mBookmarkCheck[];

	// layout
	private WebView webViewPaging;
	private WebView webView;
	private TextView tvChapter;
	private TextView tvPages;
	private FrameLayout flEvent;

	// OSD layer
	private FrameLayout flOSD;
	private TextView tvInfo;
	private TextView tvPageTitle;
	private TextView tvPageNumber;
	private LinearLayout llControls;
	private TextView btnBookmark;
	private SeekBar sbPages;

	// preference
	private boolean isFlingUse = true;

	private GestureDetector mGestureDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "[CALLBACK] void onCreate()");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.ebook);

		// get intent extras
		Intent i = getIntent();
		mBook.setId(i.getLongExtra(Extras.ID, 0));
		mBook.setTitle(i.getStringExtra(Extras.TITLE));
		mBook.setAuthor(i.getStringExtra(Extras.AUTHOR));
		mBook.setPublisher(i.getStringExtra(Extras.PUBLISHER));
		mBook.setIsbn(i.getStringExtra(Extras.ISBN));
		mBook.setLanguage(i.getStringExtra(Extras.LANGUAGE));
		mBook.setPath(i.getStringExtra(Extras.PATH));

		// set book type
		String bookType = i.getStringExtra(Extras.TYPE);
		if ("EPUB".equals(bookType)) {
			mBook.setBookType(BOOK_TYPE.EPUB);
		}
		else if ("TEXT".equals(bookType)) {
			mBook.setBookType(BOOK_TYPE.TEXT);
		}
		else if ("HTML".equals(bookType)) {
			mBook.setBookType(BOOK_TYPE.HTML);
		}
		else {
			Toast.makeText(getApplicationContext(), getString(R.string.msg_invalid_book_type), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		Log.d(TAG, "Book._id: " + mBook.getId());
		Log.d(TAG, "Book.type: " + mBook.getBookType());
		Log.d(TAG, "Book.title: " + mBook.getTitle());
		Log.d(TAG, "Book.author: " + mBook.getAuthor());
		Log.d(TAG, "Book.publisher: " + mBook.getPublisher());
		Log.d(TAG, "Book.isbn: " + mBook.getIsbn());
		Log.d(TAG, "Book.language: " + mBook.getLanguage());
		Log.d(TAG, "Book.path: " + mBook.getPath());

		// query table of content
		mToc = this.getTOC();

		// async task for book first open
		BookOpenTask bot = new BookOpenTask();
		bot.execute(savedInstanceState);

		// initialize
		this.initViews();
		this.initListeners();
	}

	/**
	 * Queries Table of Contents
	 * 
	 */
	private TableOfContent getTOC() {
		Log.i(TAG, "[METHOD] void getTOC()");
		TableOfContent toc = new TableOfContent();
		String[] from = { CL_TOC_SEQ, CL_TOC_TITLE, CL_TOC_URL, CL_TOC_ANCHOR };

		ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
		SQLiteDatabase db = chaekDB.getReadableDatabase();
		Cursor cursor = db.query(TB_TOC, from, "toc_book_id=?", new String[] { String.valueOf(mBook.getId()) }, null,
				null, CL_TOC_SEQ);

		while (cursor.moveToNext()) {
			int seq = cursor.getInt(cursor.getColumnIndex(CL_TOC_SEQ));
			String title = cursor.getString(cursor.getColumnIndex(CL_TOC_TITLE));
			String url = cursor.getString(cursor.getColumnIndex(CL_TOC_URL));
			String anchor = cursor.getString(cursor.getColumnIndex(CL_TOC_ANCHOR));

			Chapter chapter = new Chapter();
			chapter.setSeq(String.valueOf(seq));
			chapter.setTitle(title);
			chapter.setUrl(url);
			chapter.setAnchor(anchor);
			toc.addChapter(chapter);
		}
		cursor.close();
		db.close();
		chaekDB.close();

		return toc;
	}

	private class BookPagingTask extends AsyncTask<Bundle, Integer, EbookException> {

		@Override
		protected EbookException doInBackground(Bundle... params) {
			sbPages.setEnabled(false);

			int chapterCount = mToc.getChapterList().size();
			mPageList = new int[chapterCount];

			// for all chapters
			for (int i = 1; i < chapterCount + 1; i++) {
				try {
					openChapter(i, webViewPaging);
				}
				catch (EbookException e) {
					e.printStackTrace();
					return e;
				}
			}

			sbPages.setEnabled(true);
			return null;
		}

		@Override
		protected void onPostExecute(EbookException result) {
			if (result != null) {
				exitOnEbookException(result);
			}
		}
	}

	private class BookOpenTask extends AsyncTask<Bundle, Integer, EbookException> {

		@Override
		protected EbookException doInBackground(Bundle... params) {
			Log.i(TAG, "[CALLBACK] EbookException doInBackground(params:" + params + ")");
			Bundle savedInstanceState = params[0];

			// set epub mEngine
			try {
				mEngine = new EpubEngine(mBook.getBookType(), mBook.getPath());
			}
			catch (EbookException e) {
				e.printStackTrace();
				return e;
			}

			// rotated or something
			if (savedInstanceState != null) {
				Log.d(TAG, "savedInstanceState != null");

				mCurChapter = savedInstanceState.getInt("mCurChapter");
				mCurPage = savedInstanceState.getInt("mCurPage");
				mCurPercentage = savedInstanceState.getFloat("mCurPercentage");
				isOsdOn = savedInstanceState.getBoolean("isOsdOn");

				if (isOsdOn) {
					flOSD.setVisibility(View.VISIBLE);
				}
			}
			else {
				Log.d(TAG, "read from DB");
				String[] from = { CL_BOOK_CUR_CHAP, CL_BOOK_CUR_PAGE, CL_BOOK_CUR_PCNT };

				ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
				SQLiteDatabase db = chaekDB.getReadableDatabase();
				Cursor cursor = db.query(TB_BOOK, from, "_id=?", new String[] { String.valueOf(mBook.getId()) }, null,
						null, null);

				while (cursor.moveToNext()) {
					mCurChapter = cursor.getInt(cursor.getColumnIndex(CL_BOOK_CUR_CHAP));
					mCurPage = cursor.getInt(cursor.getColumnIndex(CL_BOOK_CUR_PAGE));
					mCurPercentage = cursor.getFloat(cursor.getColumnIndex(CL_BOOK_CUR_PCNT));
				}

				cursor.close();
				db.close();
				chaekDB.close();
			}
			Log.d(TAG, "mCurChapter: " + mCurChapter);
			Log.d(TAG, "mCurPage: " + mCurPage);
			Log.d(TAG, "mCurPercentage: " + mCurPercentage);

			// open chapter
			try {
				EpubActivity.this.openChapter(mCurChapter, webView);
			}
			catch (EbookException e) {
				e.printStackTrace();
				return e;
			}

			// async task for book first open
			// BookPagingTask bpt = new BookPagingTask();
			// bpt.execute();

			return null;
		}

		@Override
		protected void onPostExecute(EbookException result) {
			if (result != null) {
				exitOnEbookException(result);
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}
	}

	@Override
	protected void onStart() {
		Log.i(TAG, "[CALLBACK] void onStart()");
		super.onStart();

		// set preference values
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		isFlingUse = pref.getBoolean(PrefActivity.KEY_FLING_USE, true);
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "[CALLBACK] void onResume()");
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "[CALLBACK] void onPause()");
		super.onPause();

		// save current location into DB
		new Thread(new Runnable() {
			@Override
			public void run() {
				ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
				SQLiteDatabase db = chaekDB.getWritableDatabase();

				ContentValues values = new ContentValues();
				values.put(CL_BOOK_CUR_CHAP, mCurChapter);
				values.put(CL_BOOK_CUR_PAGE, mCurPage);
				values.put(CL_BOOK_CUR_PCNT, mCurPercentage);
				db.update(TB_BOOK, values, "_id = ?", new String[] { String.valueOf(mBook.getId()) });

				db.close();
				chaekDB.close();
			}
		}).start();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "[CALLBACK] void onStop()");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "[CALLBACK] void onDestroy()");
		super.onDestroy();
	}

	boolean isMovingPrev = false;
	boolean isMovingNext = false;

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_UP) {
			int keyCode = event.getKeyCode();

			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
				nextPage();
				break;

			case KeyEvent.KEYCODE_BACK:
				if (isOsdOn) {
					flOSD.setVisibility(View.GONE);
					isOsdOn = false;
					return false;
				}
				break;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "[CALLBACK] void onSaveInstanceState(outState:" + outState + ")");

		outState.putInt("mCurChapter", mCurChapter);
		outState.putInt("mCurPage", mCurPage);
		outState.putFloat("mCurPercentage", mCurPercentage);
		outState.putBoolean("isOsdOn", isOsdOn);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i(TAG, "[CALLBACK] void onRestoreInstanceState(savedInstanceState:" + savedInstanceState + ")");
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState != null) {
			mCurChapter = savedInstanceState.getInt("mCurChapter");
			mCurPage = savedInstanceState.getInt("mCurPage");
			mCurPercentage = savedInstanceState.getFloat("mCurPercentage");
			isOsdOn = savedInstanceState.getBoolean("isOsdOn");

			if (isOsdOn) {
				flOSD.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == Request.PREFERENCE_CHANGE) {
				// font changed
				boolean isFontChanged = data.getBooleanExtra(Extras.FONT_CHANGED, false);
				if (isFontChanged) {
					try {
						this.openChapter(mCurChapter, webView);
					}
					catch (EbookException e) {
						e.printStackTrace();
						exitOnEbookException(e);
					}
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "[CALLBACK] boolean onCreateOptionsMenu(menu:" + menu + ")");
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_epub, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "[CALLBACK] boolean onOptionsItemSelected(item:" + item + ")");

		switch (item.getItemId()) {
		case R.id.menu_info:
			StringBuffer sb = new StringBuffer();
			sb.append(getString(R.string.str_title) + ": " + mBook.getTitle() + "\n");
			sb.append(getString(R.string.str_author) + ": " + mBook.getAuthor() + "\n");
			sb.append(getString(R.string.str_publisher) + ": " + mBook.getPublisher() + "\n");
			sb.append(getString(R.string.str_isbn) + ": " + mBook.getIsbn() + "\n");
			sb.append(getString(R.string.str_language) + ": " + mBook.getLanguage() + "\n");

			new AlertDialog.Builder(this).setIcon(R.drawable.icon).setTitle(R.string.menu_info)
					.setMessage(sb.toString()).show();
			return true;

		case R.id.menu_toc:
			new AlertDialog.Builder(this).setIcon(R.drawable.icon).setTitle(R.string.menu_toc)
					.setSingleChoiceItems(mToc.getTitleArray(), mCurChapter - 1, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							try {
								openChapter(i + 1, webView);
							}
							catch (EbookException e) {
								e.printStackTrace();
								exitOnEbookException(e);
							}
							mCurPage = 1;
							mCurPercentage = 0.0f;
							dialogInterface.dismiss();
						}
					}).show();
			return true;

		case R.id.menu_bookmark:
			// check current chapter's bookmark
			ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
			SQLiteDatabase db = chaekDB.getReadableDatabase();

			Cursor cursor = db.query(TB_BOOKMARK, new String[] { CL_BM_CHAPTER, CL_BM_PERCENTAGE }, CL_BM_BOOK_ID
					+ " = ?", new String[] { String.valueOf(mBook.getId()) }, null, null, CL_BM_CHAPTER + ", "
					+ CL_BM_PERCENTAGE);
			Log.d(TAG, "SELECT * FROM " + TB_BOOKMARK + " WHERE " + CL_BM_BOOK_ID + " = " + mBook.getId());

			int rowCount = 0;
			int resultCount = cursor.getCount();
			String[] bmTitleList = new String[resultCount];
			mBookmarkChapter = new long[resultCount];
			mBookmarkPage = new int[resultCount];
			mBookmarkPercentage = new float[resultCount];

			while (cursor.moveToNext()) {
				long bmChapter = cursor.getInt(cursor.getColumnIndex(CL_BM_CHAPTER));
				float bmPercentage = cursor.getFloat(cursor.getColumnIndex(CL_BM_PERCENTAGE));
				int bmPage = (int) (mMaxPage * bmPercentage);

				Log.d(TAG, CL_BM_CHAPTER + ": " + bmChapter + ", " + CL_BM_PERCENTAGE + ": " + bmPercentage + " ("
						+ bmPage + " / " + mMaxPage + ")");

				bmTitleList[rowCount] = "Chapter " + bmChapter + ": " + (Math.round(bmPercentage * 1000) / 10.0) + "%";
				mBookmarkChapter[rowCount] = bmChapter;
				mBookmarkPage[rowCount] = bmPage;
				mBookmarkPercentage[rowCount] = bmPercentage;
				rowCount++;
			}
			cursor.close();
			db.close();
			chaekDB.close();

			new AlertDialog.Builder(this).setIcon(R.drawable.icon).setTitle(R.string.menu_bookmark)
					.setSingleChoiceItems(bmTitleList, 0, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							int chapter = (int) mBookmarkChapter[i];

							if (chapter == mCurChapter) {
								webView.loadUrl("javascript:openPageByPercentage(" + mBookmarkPercentage[i] + ")");
								mCurPage = mBookmarkPage[i];
								mCurPercentage = mBookmarkPercentage[i];
							}
							else {
								try {
									openChapter(chapter, webView);
								}
								catch (EbookException e) {
									e.printStackTrace();
									exitOnEbookException(e);
								}
								mCurPage = mBookmarkPage[i];
								mCurPercentage = mBookmarkPercentage[i];
							}
							dialogInterface.dismiss();
						}
					}).show();
			return true;

		case R.id.menu_settings:
			startActivityForResult(new Intent(this, PrefActivity.class), Request.PREFERENCE_CHANGE);
			return true;
		}
		return false;
	}

	/**
	 * Handler for epub viewer
	 */
	private Handler handler = new Handler() {
		Animation animation;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_JS_CURPAGE:
				Log.d(TAG, "HANDLER_JS_CURPAGE");
				this.refreshOSD((Integer) msg.obj);
				this.refreshPage((Integer) msg.obj);

				if (mBookmarkCheck[mCurPage - 1]) {
					btnBookmark.setText("–");
				}
				else {
					btnBookmark.setText("+");
				}
				sbPages.setProgress(mCurPage - 1);
				break;

			case HANDLER_JS_TOTPAGE:
				Log.d(TAG, "HANDLER_JS_TOTPAGE");
				sbPages.setMax(((Integer) msg.obj) - 1);
				break;

			case HANDLER_SEEKBAR_CHANGING:
				Log.d(TAG, "HANDLER_SEEKBAR_CHANGING");
				this.refreshOSD((Integer) msg.obj);
				break;

			case HANDLER_SEEKBAR_CHANGED:
				Log.d(TAG, "HANDLER_SEEKBAR_CHANGED");
				this.refreshPage((Integer) msg.obj);
				break;

			case HANDLER_SHOW_OSD:
				Log.d(TAG, "HANDLER_SHOW_OSD");
				flOSD.setVisibility(View.VISIBLE);
				isOsdOn = true;

				// animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadein);
				// flOSD.startAnimation(animation);
				break;

			case HANDLER_HIDE_OSD:
				Log.d(TAG, "HANDLER_HIDE_OSD");
				// animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadeout);
				// flOSD.startAnimation(animation);

				flOSD.setVisibility(View.GONE);
				isOsdOn = false;
				break;

			case HANDLER_PAGING:
				break;

			case HANDLER_TOAST_SHORT:
				Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
				break;

			case HANDLER_TOAST_LONG:
				Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
				break;

			case HANDLER_WHAT_OTHERS:
				Log.d(TAG, "HANDLER_WHAT_OTHERS");
				break;
			}
		}

		/**
		 * Refreshes OSD
		 * 
		 * @param curPage
		 */
		private void refreshOSD(int curPage) {
			tvPageTitle.setText(mToc.getTitleArray()[mCurChapter - 1]);
			tvPageNumber.setText(String.valueOf(curPage) + "/" + String.valueOf(mMaxPage));
		}

		/**
		 * Refreshes Page info.
		 * 
		 * @param curPage
		 */
		private void refreshPage(int curPage) {
			tvChapter.setText(mToc.getTitleArray()[mCurChapter - 1]);
			tvPages.setText(String.valueOf(curPage) + "/" + String.valueOf(mMaxPage));
		}
	};

	/**
	 * Bridge for Javascript call
	 */
	class AndroidBridge {
		public void setCurPageLocation(final int page, final float percentage) throws InterruptedException {
			Log.i(TAG, "[BRIDGE] void setCurPageLocation(page:" + page + ", percentage:" + percentage + ")");
			mCurPage = page;
			mCurPercentage = percentage;
			handler.sendMessage(Message.obtain(handler, HANDLER_JS_CURPAGE, (Integer) mCurPage));

			Log.i(TAG, "[BRIDGE] void setCurPageLocation() finished");
		}

		public void setTotalPageNum(final int page) {
			Log.i(TAG, "[BRIDGE] void setTotalPageNum(page:" + page + ")");
			mMaxPage = page;
			handler.sendMessage(Message.obtain(handler, HANDLER_JS_TOTPAGE, (Integer) mMaxPage));

			// check current chapter bookmark
			this.selectBookmark();
		}

		public void setTotalPageNumPaging(final int page) {
			Log.i(TAG, "[BRIDGE] void setTotalPageNum(page:" + page + ")");
			mMaxPage = page;
			handler.sendMessage(Message.obtain(handler, HANDLER_JS_TOTPAGE, (Integer) mMaxPage));

			// check current chapter bookmark
			this.selectBookmark();
		}

		/**
		 * Checks Current Chapter Bookmark
		 * 
		 */
		private void selectBookmark() {
			Log.i(TAG, "[METHOD] void selectBookmark()");

			// check current chapter's bookmark
			ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
			SQLiteDatabase db = chaekDB.getReadableDatabase();

			Cursor cursor = db.query(TB_BOOKMARK, new String[] { CL_BM_BOOK_ID, CL_BM_CHAPTER, CL_BM_PERCENTAGE },
					CL_BM_BOOK_ID + " = ? AND " + CL_BM_CHAPTER + " = ?", new String[] { String.valueOf(mBook.getId()),
							String.valueOf(mCurChapter) }, null, null, CL_BM_PERCENTAGE);
			Log.d(TAG, "SELECT * FROM " + TB_BOOKMARK + " WHERE " + CL_BM_BOOK_ID + " = " + mBook.getId() + " AND "
					+ CL_BM_CHAPTER + " = " + mCurChapter);

			int rowCount = 0;
			mBookmarkCheck = new boolean[mMaxPage];

			for (int i = 0; i < mMaxPage; i++) {
				mBookmarkCheck[i] = false;
			}

			while (cursor.moveToNext()) {
				long bmBookId = cursor.getLong(cursor.getColumnIndex(CL_BM_BOOK_ID));
				long bmChapter = cursor.getInt(cursor.getColumnIndex(CL_BM_CHAPTER));
				float bmPercentage = cursor.getFloat(cursor.getColumnIndex(CL_BM_PERCENTAGE));
				int bmPage = (int) (mMaxPage * bmPercentage);
				Log.d(TAG, CL_BM_BOOK_ID + ": " + bmBookId + ", " + CL_BM_CHAPTER + ": " + bmChapter + ", "
						+ CL_BM_PERCENTAGE + ": " + bmPercentage + " (" + bmPage + " / " + mMaxPage + ")");

				mBookmarkCheck[bmPage - 1] = true;
				rowCount++;
			}
			cursor.close();
			db.close();
			chaekDB.close();
		}
	}

	/**
	 * Initializes Views
	 */
	private void initViews() {
		Log.i(TAG, "[METHOD] void initViews()");

		// calculate display width, height
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		Log.d(TAG, "density: " + (DisplayInfo.density = metrics.density));
		Log.d(TAG, "densityDpi: " + (DisplayInfo.densityDpi = metrics.densityDpi));
		Log.d(TAG, "widthPixels: " + (DisplayInfo.widthPixels = metrics.widthPixels));
		Log.d(TAG, "heightPixels: " + (DisplayInfo.heightPixels = metrics.heightPixels));
		Log.d(TAG, "scaledDensity: " + (DisplayInfo.scaledDensity = metrics.scaledDensity));
		Log.d(TAG, "xdpi: " + (DisplayInfo.xdpi = metrics.xdpi));
		Log.d(TAG, "ydpi: " + (DisplayInfo.ydpi = metrics.ydpi));

		mBookWidth = metrics.widthPixels + SCREEN_WIDTH_CORRECTION;
		mBookHeight = metrics.heightPixels + SCREEN_HEIGHT_CORRECTION;
		Log.d(TAG, "mBookWidth: " + mBookWidth);
		Log.d(TAG, "mBookHeight: " + mBookHeight);

		// paging webView
		webViewPaging = (WebView) findViewById(R.id.webViewPaging);
		webViewPaging.setClickable(false);
		webViewPaging.setSelected(false);
		webViewPaging.setHorizontalScrollBarEnabled(false);
		webViewPaging.setVerticalScrollBarEnabled(false);
		webViewPaging.addJavascriptInterface(new AndroidBridge(), "android");

		// paging webView setting
		WebSettings webSetting1 = webViewPaging.getSettings();
		webSetting1.setDefaultZoom(this.getZoomDensity());
		webSetting1.setDefaultFontSize(24);
		webSetting1.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);

		// rendering webView
		webView = (WebView) findViewById(R.id.webView);
		webView.setSelected(true);
		webView.setHorizontalScrollBarEnabled(false);
		webView.setVerticalScrollBarEnabled(false);
		webView.addJavascriptInterface(new AndroidBridge(), "android");

		// day/night mode
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String dayNightMode = pref.getString(PrefActivity.KEY_DAY_NIGHT_MODE, "Day");
		Log.d(TAG, "dayNightMode: " + dayNightMode);

		webView.setBackgroundColor(0);
		if ("Night".equalsIgnoreCase(dayNightMode)) {
			webView.setBackgroundResource(color.black);
		}
		else {
			webView.setBackgroundResource(color.white);
		}

		// rendering webView setting
		WebSettings webSetting2 = webView.getSettings();
		webSetting2.setDefaultZoom(this.getZoomDensity());
		webSetting2.setDefaultFontSize(24);
		webSetting2.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
		webSetting2.setJavaScriptEnabled(true);

		// tail infos (chapter, pages)
		tvChapter = (TextView) findViewById(R.id.tvChapter);
		tvPages = (TextView) findViewById(R.id.tvPages);

		// frame layout for event handling
		flEvent = (FrameLayout) findViewById(R.id.flEvent);

		// frame layout for OSD
		flOSD = (FrameLayout) findViewById(R.id.flOSD);
		tvInfo = (TextView) findViewById(R.id.tvInfo);
		tvInfo.setText(mBook.getTitle() + " (" + StringUtil.null2str(mBook.getAuthor()) + ")");
		tvPageTitle = (TextView) findViewById(R.id.tvPageTitle);
		tvPageNumber = (TextView) findViewById(R.id.tvPageNumber);
		llControls = (LinearLayout) findViewById(R.id.llControls);
		btnBookmark = (TextView) findViewById(R.id.btnBookmark);
		sbPages = (SeekBar) findViewById(R.id.sbPages);
	}

	/**
	 * Calc zoom density
	 * 
	 * @return
	 */
	private ZoomDensity getZoomDensity() {
		ZoomDensity zd;
		if (DisplayInfo.densityDpi == 240) {
			zd = WebSettings.ZoomDensity.FAR;
		}
		else if (DisplayInfo.densityDpi == 160) {
			zd = WebSettings.ZoomDensity.MEDIUM;
		}
		else if (DisplayInfo.densityDpi == 120) {
			zd = WebSettings.ZoomDensity.CLOSE;
		}
		else {
			zd = WebSettings.ZoomDensity.MEDIUM;
		}
		return zd;
	}

	/**
	 * Initializes Event Listeners
	 */
	private void initListeners() {
		Log.i(TAG, "[METHOD] void initListeners()");

		mGestureDetector = new GestureDetector(this, mGestureListener);

		flEvent.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				Log.i(TAG, "[CALLBACK_FL] boolean onTouch(view:" + view + ", event:" + event + ")");
				return mGestureDetector.onTouchEvent(event);
			}
		});

		flEvent.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				Log.i(TAG, "[CALLBACK_FL boolean onLongClick(view:" + view + ")] ");
				return false;
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
				Log.i(TAG, "[CALLBACK_WV] boolean onJsAlert(view:" + view + ", url:" + url + ", message:" + message
						+ ", result:" + result + ")");
				Log.d(TAG, "message: " + message);
				result.confirm();
				return true;
			}
		});

		// WebViewClient must be set BEFORE calling loadUrl!
		webViewPaging.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.i(TAG, "[CALLBACK_WVP] void onPageStarted(view:" + view + ", url:" + url + ", favicon:" + favicon
						+ ")");
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				Log.i(TAG, "[CALLBACK_WVP] void onPageFinished(view:" + view + ", url:" + url + ")");

				// calc total page number
				// also, move to certain location
				webView.loadUrl("javascript:getTotalPageNumPaging()");
				Log.d(TAG, "javascript:getTotalPageNumPaging()");
			}
		});

		// WebViewClient must be set BEFORE calling loadUrl!
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.i(TAG, "[CALLBACK_WV] void onPageStarted(view:" + view + ", url:" + url + ", favicon:" + favicon
						+ ")");
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				Log.i(TAG, "[CALLBACK_WV] void onPageFinished(view:" + view + ", url:" + url + ")");

				// calc total page number
				// also, move to certain location
				webView.loadUrl("javascript:getTotalPageNum()");
				Log.d(TAG, "javascript:getTotalPageNum()");

				// move to certain location
				webView.loadUrl("javascript:openPageByPercentage(" + mCurPercentage + ")");
				Log.d(TAG, "javascript:openPageByPercentage(" + mCurPercentage + ")");
			}
		});

		// Layer for control area (bottom)
		llControls.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				return true;
			}
		});

		// Button for Bookmark
		btnBookmark.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				Log.i(TAG, "[CALLBACK_BOOKMARK] boolean onTouch(view:" + view + ", event:" + event + ")");

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					Log.d(TAG, "MotionEvent.ACTION_DOWN");

					if (mBookmarkCheck[mCurPage - 1]) {
						this.deleteBookmark();

						mBookmarkCheck[mCurPage - 1] = false;
						btnBookmark.setText("+");
					}
					else {
						this.insertBookmark();

						mBookmarkCheck[mCurPage - 1] = true;
						btnBookmark.setText("–");
					}
					return true;
				}
				return false;
			}

			private void insertBookmark() {
				Log.i(TAG, "[METHOD] void insertBookmark()");

				ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
				SQLiteDatabase db = chaekDB.getWritableDatabase();

				ContentValues values = new ContentValues();
				values.put(CL_BM_BOOK_ID, String.valueOf(mBook.getId()));
				values.put(CL_BM_CHAPTER, String.valueOf(mCurChapter));
				values.put(CL_BM_PERCENTAGE, String.valueOf(mCurPercentage));

				db.insert(TB_BOOKMARK, null, values);
				Log.d(TAG, "INSERT INTO " + TB_BOOKMARK + " VALUES(" + mBook.getId() + ", " + mCurChapter + ", "
						+ mCurPercentage + ")");

				mBookmarkCheck[mCurPage - 1] = false;
				btnBookmark.setText("+");

				db.close();
				chaekDB.close();

				Toast.makeText(getApplicationContext(), getString(R.string.msg_bookmark_succeed), Toast.LENGTH_SHORT)
						.show();
			}

			private void deleteBookmark() {
				Log.i(TAG, "[METHOD] void deleteBookmark()");

				ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
				SQLiteDatabase db = chaekDB.getWritableDatabase();

				db.delete(
						TB_BOOKMARK,
						CL_BM_BOOK_ID + " = ? AND " + CL_BM_CHAPTER + " = ? AND " + CL_BM_PERCENTAGE + " = ?",
						new String[] { String.valueOf(mBook.getId()), String.valueOf(mCurChapter),
								String.valueOf(mCurPercentage) });
				Log.d(TAG,
						"DELETE FROM " + TB_BOOKMARK + " WHERE " + CL_BM_BOOK_ID + " = "
								+ String.valueOf(mBook.getId()) + " AND " + CL_BM_CHAPTER + " = "
								+ String.valueOf(mCurChapter) + " AND " + CL_BM_PERCENTAGE + " = "
								+ String.valueOf(mCurPercentage));

				mBookmarkCheck[mCurPage - 1] = false;
				btnBookmark.setText("+");

				db.close();
				chaekDB.close();

				Toast.makeText(getApplicationContext(), getString(R.string.msg_bookmark_removed), Toast.LENGTH_SHORT)
						.show();
			}
		});

		// SeekBar change listener
		sbPages.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				Log.i(TAG, "[CALLBACK_SEEKBAR] void onStartTrackingTouch(seekBar:" + seekBar + ")");
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Log.i(TAG, "[CALLBACK_SEEKBAR] void onStopTrackingTouch(seekBar:" + seekBar + ")");

				int progress = seekBar.getProgress();
				Log.d(TAG, "progress: " + progress);

				//
				webView.loadUrl("javascript:openPageByNum(" + (progress + 1) + ")");
				handler.sendMessage(Message.obtain(handler, HANDLER_SEEKBAR_CHANGED, (Integer) (progress + 1)));
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Log.i(TAG, "[CALLBACK_SEEKBAR] void onProgressChanged(seekBar:" + seekBar + ", progress:" + progress
						+ ", fromUser:" + fromUser + ")");
				handler.sendMessage(Message.obtain(handler, HANDLER_SEEKBAR_CHANGING, (Integer) (progress + 1)));
			}
		});
	}

	/**
	 * Gesture Event Handler
	 */
	private SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {

		@Override
		public boolean onDown(MotionEvent e) {
			Log.i(TAG, "[CALLBACK_GL] boolean onDown(e:" + e + ")");
			return super.onDown(e);
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			Log.i(TAG, "[CALLBACK_GL] boolean onSingleTapConfirmed(e:" + e + ")");

			float posX = e.getX();
			int screenWidth = DisplayInfo.widthPixels;
			int flingAreaSize = 100;
			Log.d(TAG, "posX: " + posX);
			Log.d(TAG, "screenWidth: " + screenWidth);
			Log.d(TAG, "flingAreaSize: " + flingAreaSize);

			// open previous
			if (0 <= posX && posX <= flingAreaSize) {
				Log.d(TAG, "if (0 <= " + posX + " <= " + flingAreaSize + ")");
				prevPage();
			}
			// open next
			else if ((screenWidth - flingAreaSize) <= posX && posX <= screenWidth) {
				Log.d(TAG, "if(" + (screenWidth - flingAreaSize) + " <= " + posX + " <= " + screenWidth + ")");
				nextPage();
			}
			// toggle OSD
			else {
				if (flOSD.getVisibility() == View.GONE) {
					handler.sendMessage(Message.obtain(handler, HANDLER_SHOW_OSD));
				}
				else {
					handler.sendMessage(Message.obtain(handler, HANDLER_HIDE_OSD));
				}
			}

			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			Log.i(TAG, "[CALLBACK_GL] boolean onDoubleTap(e:" + e + ")");
			return super.onDoubleTap(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			Log.i(TAG, "[CALLBACK_GL] boolean onFling(e1:" + e1 + ", e2:" + e2 + ", velocityX:" + velocityX
					+ ", velocityY:" + velocityY + "");

			if (isFlingUse) {
				Log.d(TAG, "preference fling is on.");

				float x1 = e1.getX();
				float x2 = e2.getX();
				int movement = (int) (x2 - x1) / DECISION_FLING_HORIZONTAL;
				Log.d(TAG, "movement: " + movement);

				if (movement > 0) {
					prevPage();
				}
				else if (movement < 0) {
					nextPage();
				}
			}
			else {
				Log.d(TAG, "preference fling is off.");
			}
			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public void onShowPress(MotionEvent e) {
			Log.i(TAG, "[CALLBACK_GL] void onShowPress(e:" + e + ")");
			super.onShowPress(e);
		}

		@Override
		public void onLongPress(MotionEvent e) {
			Log.i(TAG, "[CALLBACK_GL] void onLongPress(e:" + e + ")");
			super.onLongPress(e);
		}
	};

	/**
	 * Opens Chapter
	 * 
	 * @param chapNo
	 */
	private void openChapter(int chapNo, WebView webView) throws EbookException {
		Log.i(TAG, "[METHOD] void openChapter(chapNo:" + chapNo + ")");

		Chapter chapter = null;
		String chapterPath = null;
		try {
			chapter = mToc.getChapter(chapNo - 1);
			chapterPath = mEngine.getBaseUrl() + "/" + chapter.getUrl();
			Log.d(TAG, "chapterPath: " + chapterPath);
		}
		catch (Exception e) {
			throw new EbookException(getString(R.string.msg_invalid_toc));
		}

		// generate HTML inlcludes javascript
		String html = EpubEngine.preprocess(chapterPath, mBookWidth, mBookHeight, EpubActivity.this);
		// System.out.println(html);
		// Log.v(TAG, html);

		// load data on webView
		String baseUrl = "file://" + mEngine.getBaseUrl();
		Log.d(TAG, "baseUrl: " + baseUrl);
		webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);

		// set current chapter
		mCurChapter = chapNo;
	}

	/**
	 * Open Previous Page
	 */
	private void prevPage() {
		Log.i(TAG, "[METHOD] void prevPage()");

		if (mCurPage == 1) {
			if ((mCurChapter - 1) <= 0) {
				Toast.makeText(this, getString(R.string.msg_first_page), Toast.LENGTH_SHORT).show();
			}
			else {
				try {
					this.openChapter(--mCurChapter, webView);
				}
				catch (EbookException e) {
					e.printStackTrace();
					exitOnEbookException(e);
				}
				mCurPage = mMaxPage;
				mCurPercentage = 1.0f;
			}
		}
		else {
			webView.loadUrl("javascript:prevPage()");
		}
	}

	/**
	 * Open Next Page
	 */
	private void nextPage() {
		Log.i(TAG, "[METHOD] void nextPage()");

		if (mCurPage == mMaxPage) {
			if ((mCurChapter + 1) > mToc.getTotalSize()) {
				Toast.makeText(this, getString(R.string.msg_last_page), Toast.LENGTH_SHORT).show();
			}
			else {
				try {
					this.openChapter(++mCurChapter, webView);
				}
				catch (EbookException e) {
					e.printStackTrace();
					exitOnEbookException(e);
				}
				mCurPage = 1;
				mCurPercentage = 0.0f;
			}
		}
		else {
			webView.loadUrl("javascript:nextPage()");
		}
	}

	/**
	 * Toast error message and exit
	 * 
	 * @param e
	 */
	private void exitOnEbookException(EbookException e) {
		Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		finish();
	}

}
