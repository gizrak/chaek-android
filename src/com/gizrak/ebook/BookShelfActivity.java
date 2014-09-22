package com.gizrak.ebook;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.gizrak.ebook.utils.FileUtil;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

public class BookShelfActivity extends Activity implements Constants {

	private static final String TAG = "BookShelfActivity";

	private static final int DIALOG_DELETE = 0;
	private static final int DIALOG_IMPORT = 1;

	// Members
	private ChaekHelper mChaekDB;
	private ArrayList<BookInfo> mBookList = new ArrayList<BookInfo>();

	private boolean isBookExists = false;
	private int mTotalCount = 0;
	private int mFailCount = 0;
	
	private AdView adView;

	// Controls
	private ListView lvBookList;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "[CALLBACK] void onCreate()");

		setContentView(R.layout.bookshelf);

		this.initView();
		this.initListener();

		registerForContextMenu(lvBookList);

		// prepare DB
		mChaekDB = new ChaekHelper(getApplicationContext());

		this.refreshList();
	}

	/**
	 * Initializes Views
	 * 
	 */
	private void initView() {
		lvBookList = (ListView) findViewById(R.id.lvBookList);
		adView = (AdView) findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest();
		adRequest.addKeyword("book");
		adRequest.addKeyword("magazine");
		adRequest.addKeyword("comic");
		adView.loadAd(adRequest);
	}

	/**
	 * Initializes Event Listeners
	 * 
	 */
	private void initListener() {
		// book listview onClick
		lvBookList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> list, View view, int position, long id) {
				Log.i(TAG, "[CALLBACK] void onItemClick(list:" + list + ", view:" + view + ", position:" + position
						+ ", id:" + id + ")");

				if (isBookExists) {
					BookInfo book = mBookList.get(position);

					if (new File(book.getPath()).exists()) {
						Intent intent = new Intent(BookShelfActivity.this, EpubActivity.class);
						intent.putExtra(Extras.ID, book.getId());
						intent.putExtra(Extras.TYPE, book.getBookType().name());
						intent.putExtra(Extras.TITLE, book.getTitle());
						intent.putExtra(Extras.AUTHOR, book.getAuthor());
						intent.putExtra(Extras.PUBLISHER, book.getPublisher());
						intent.putExtra(Extras.ISBN, book.getIsbn());
						intent.putExtra(Extras.LANGUAGE, book.getLanguage());
						intent.putExtra(Extras.PATH, book.getPath());
						startActivity(intent);

						// start fadein and hold animation
						overridePendingTransition(R.anim.right_in, R.anim.fadeout);
					}
					else {
						Toast.makeText(getApplicationContext(), getString(R.string.msg_no_file), Toast.LENGTH_LONG)
								.show();
					}
				}
			}
		});

		// book listview onClick
		lvBookList.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				Log.i(TAG, "[CALLBACK] boolean onLongClick(view:" + view + ")");
				return false;
			}
		});
	}

	/**
	 * Refreshes current list
	 * 
	 */
	private void refreshList() {
		String[] from = { _ID, CL_BOOK_TYPE, CL_BOOK_TITLE, CL_BOOK_AUTHOR, CL_BOOK_PUBLISHER, CL_BOOK_ISBN,
				CL_BOOK_LANGUAGE, CL_BOOK_IMAGE, CL_BOOK_PATH };

		SQLiteDatabase db = mChaekDB.getReadableDatabase();
		Cursor cursor = db.query(TB_BOOK, from, null, null, null, null, CL_BOOK_TITLE);
		startManagingCursor(cursor);

		// clear list
		mBookList.clear();

		// fetch rows
		if (cursor.getCount() > 0) {
			// cursor.moveToFirst() and cursor.moveToNext() makes exception
			// "java.lang.IllegalStateException: get field slot from row 0 col 0 failed"
			// i don't know why.
			// just use below method. reverse moving from last.
			cursor.moveToLast();
			while (cursor.moveToPrevious()) {
				long _id = cursor.getLong(cursor.getColumnIndex(_ID));
				String title = cursor.getString(cursor.getColumnIndex(CL_BOOK_TITLE));
				String author = cursor.getString(cursor.getColumnIndex(CL_BOOK_AUTHOR));
				String publisher = cursor.getString(cursor.getColumnIndex(CL_BOOK_PUBLISHER));
				String isbn = cursor.getString(cursor.getColumnIndex(CL_BOOK_ISBN));
				String language = cursor.getString(cursor.getColumnIndex(CL_BOOK_LANGUAGE));
				byte[] coverImg = cursor.getBlob(cursor.getColumnIndex(CL_BOOK_IMAGE));
				String path = cursor.getString(cursor.getColumnIndex(CL_BOOK_PATH));

				BookInfo book = new BookInfo();
				book.setId(_id);
				book.setTitle(title);
				book.setAuthor(author);
				book.setPublisher(publisher);
				book.setIsbn(isbn);
				book.setLanguage(language);
				book.setCoverImg(FileUtil.resizingByteArrayImage(coverImg, 120, 160));
				book.setPath(path);

				// set book type
				String bookType = cursor.getString(cursor.getColumnIndex(CL_BOOK_TYPE));
				if ("EPUB".equals(bookType)) {
					book.setBookType(BOOK_TYPE.EPUB);
				}
				else if ("TEXT".equals(bookType)) {
					book.setBookType(BOOK_TYPE.TEXT);
				}
				else if ("HTML".equals(bookType)) {
					book.setBookType(BOOK_TYPE.HTML);
				}

				mBookList.add(book);
			}
		}

		// clear DB resources
		cursor.close();
		db.close();

		// for empty list
		if (mBookList.size() == 0) {
			mBookList.add(null);
			isBookExists = false;
		}
		else {
			isBookExists = true;
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				BookAdapter adapter = new BookAdapter(getApplicationContext(), R.layout.bookshelf_item, mBookList);
				lvBookList.setAdapter(adapter);
				lvBookList.setItemsCanFocus(true);
			}
		});
	}

	class BookAdapter extends ArrayAdapter<BookInfo> {

		private Context context;
		private ArrayList<BookInfo> items;
		private ViewHolder holder;

		public BookAdapter(Context context, int textViewResourceId, ArrayList<BookInfo> items) {
			super(context, textViewResourceId, items);

			this.context = context;
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.bookshelf_item, null);

				holder = new ViewHolder();
				v.setTag(holder);

				holder.ivCoverImg = (ImageView) v.findViewById(R.id.ivCoverImg);
				holder.tvTitle = (TextView) v.findViewById(R.id.tvTitle);
				holder.tvAuthor = (TextView) v.findViewById(R.id.tvAuthor);
				holder.tvIsbn = (TextView) v.findViewById(R.id.tvIsbn);
			}
			else {
				holder = (ViewHolder) v.getTag();
			}

			BookInfo book = items.get(position);
			if (book != null) {
				if (book.getCoverImg() != null) {
					try {
						ByteArrayInputStream in = new ByteArrayInputStream(book.getCoverImg());
						Bitmap bitmap = BitmapFactory.decodeStream(in);
						holder.ivCoverImg.setImageBitmap(bitmap);
						in.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					holder.ivCoverImg.setImageResource(R.drawable.no_images);
				}

				holder.tvTitle.setText(book.getTitle());
				holder.tvAuthor.setText(book.getAuthor());
				holder.tvIsbn.setText(book.getIsbn());
			}
			else {
				holder.ivCoverImg.setVisibility(View.GONE);
				holder.tvTitle.setText(getString(R.string.msg_no_books));
				holder.tvAuthor.setText(getString(R.string.msg_how_to_import));
			}

			return v;
		}

		class ViewHolder {
			ImageView ivCoverImg;
			TextView tvTitle;
			TextView tvAuthor;
			TextView tvIsbn;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		Log.i(TAG, "[CALLBACK] void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)");

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		BookInfo book = mBookList.get(info.position);

		// set menu title
		menu.setHeaderTitle(book.getTitle());

		// inflate context menu
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_bookshelf, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.i(TAG, "[CALLBACK] boolean onContextItemSelected(item:" + item + ")");

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		BookInfo book = mBookList.get((int) info.id);
		Log.d(TAG, "book._id: " + book.getId());
		Log.d(TAG, "book.title: " + book.getTitle());

		switch (item.getItemId()) {
		case R.id.context_shortcut:
			Bitmap bitmap = null;
			try {
				ByteArrayInputStream in = new ByteArrayInputStream(book.getCoverImg());
				bitmap = BitmapFactory.decodeStream(in);
				in.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			Intent intent = new Intent(this, ShortcutActivity.class);
			intent.putExtra(Extras.CLASS_NAME, ".EpubActivity");
			intent.putExtra(Extras.ID, book.getId());
			intent.putExtra(Extras.TYPE, book.getBookType().name());
			intent.putExtra(Extras.TITLE, book.getTitle());
			intent.putExtra(Extras.AUTHOR, book.getAuthor());
			intent.putExtra(Extras.PATH, book.getPath());
			intent.putExtra(Extras.IMAGE, FileUtil.makeThumbImg(bitmap, 72, 72));
			startActivity(intent);
			return true;

		case R.id.context_delete:
			new RemoveInBackground().execute(book);
			return true;
		}
		return false;
	}

	private class RemoveInBackground extends AsyncTask<BookInfo, Integer, Long> {

		@Override
		protected void onPreExecute() {
			// show progress
			showDialog(DIALOG_DELETE);
			progressDialog.setMax(100);
			publishProgress(0);
		}

		@Override
		protected Long doInBackground(BookInfo... params) {
			BookInfo book = params[0];

			ChaekHelper chaekDB = new ChaekHelper(getApplicationContext());
			SQLiteDatabase db = chaekDB.getWritableDatabase();
			publishProgress(10);
			db.delete(TB_BOOK, "_id = ?", new String[] { String.valueOf(book.getId()) });
			publishProgress(30);
			db.delete(TB_TOC, CL_TOC_BOOK_ID + " = ?", new String[] { String.valueOf(book.getId()) });
			publishProgress(50);
			db.delete(TB_BOOKMARK, CL_BM_BOOK_ID + " = ?", new String[] { String.valueOf(book.getId()) });
			publishProgress(70);
			db.close();
			chaekDB.close();
			publishProgress(80);

			try {
				// delete unzip files
				String bookPath = book.getPath();
				String bookDir = bookPath.substring(0, bookPath.lastIndexOf("/") + 1);
				String bookName = bookPath.substring(bookPath.lastIndexOf("/") + 1, bookPath.lastIndexOf("."));
				String unzipPath = bookDir + "." + bookName;
				Log.d(TAG, "unzipPath: " + unzipPath);
				publishProgress(90);

				FileUtils.deleteDirectory(new File(unzipPath));
				publishProgress(100);
			}
			catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Long result) {
			// clean dialog
			progressDialog.dismiss();
			removeDialog(DIALOG_IMPORT);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), getString(R.string.msg_delete_succeed), Toast.LENGTH_LONG)
							.show();
					refreshList();
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		Log.i(TAG, "[CALLBACK] boolean onCreateOptionsMenu(menu:" + menu + ")");

		// inflate option menu
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_bookshelf, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.removeItem(R.id.menu_delete);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "[CALLBACK] boolean onOptionsItemSelected(item:" + item + ")");

		switch (item.getItemId()) {
		case R.id.menu_import:
			Intent intent = new Intent(this, ImportActivity.class);
			startActivityForResult(intent, Request.IMPORT_EPUB);
			break;

		// TODO - delete several items
		// case R.id.menu_delete:
		// break;

		case R.id.menu_about:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.i(TAG, "[CALLBACK] Dialog onCreateDialog(id:" + id + ")");

		switch (id) {
		case DIALOG_DELETE:
			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage(getString(R.string.dlg_deleting));
			return progressDialog;

		case DIALOG_IMPORT:
			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage(getString(R.string.dlg_importing));
			return progressDialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "[METHOD] void onActivityResult(requestCode:" + requestCode + ", resultCode:" + resultCode
				+ ", data:" + data + ")");

		if (resultCode == RESULT_OK) {
			if (requestCode == Request.IMPORT_EPUB) {
				new ImportInBackground().execute(data);
			}
		}
	}

	private class ImportInBackground extends AsyncTask<Intent, Integer, Long> {

		@Override
		protected void onPreExecute() {
			// show progress
			showDialog(DIALOG_IMPORT);
			progressDialog.setMax(100);
			publishProgress(0);
		}

		@Override
		protected Long doInBackground(Intent... params) {
			final ArrayList<String> pathList = params[0].getStringArrayListExtra(Extras.PATH_LIST);
			Log.d(TAG, "pathList: " + pathList);

			mTotalCount = pathList.size();

			// import sequentially
			for (int nProgress = 0; nProgress < mTotalCount; nProgress++) {
				String path = pathList.get(nProgress);
				int delim = 100 / mTotalCount;

				// for epub files
				if (path.toLowerCase().lastIndexOf(".epub") > 0) {
					try {
						EpubEngine engine = new EpubEngine(BOOK_TYPE.EPUB, path);
						publishProgress((delim * nProgress) + (int) (delim * 0.2));
						BookInfo bookInfo = engine.parseBookInfo();
						publishProgress((delim * nProgress) + (int) (delim * 0.35));
						TableOfContent toc = engine.parseTableOfContent();
						publishProgress((delim * nProgress) + (int) (delim * 0.5));

						long bookId = insertBookInfo(bookInfo);
						publishProgress((delim * nProgress) + (int) (delim * 0.75));
						insertTableOfContents(toc, bookId);
						publishProgress((delim * nProgress) + (int) (delim * 1.0));
					}
					catch (EbookException e) {
						e.printStackTrace();
						mFailCount++;
					}
				}
				// for text files
				else if (path.toLowerCase().lastIndexOf(".txt") > 0) {
					String title = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
					String baseUrl = path.substring(0, path.lastIndexOf("/") + 1) + "." + title;
					publishProgress((delim * nProgress) + (int) (delim * 0.1));

					try {
						ArrayList<String> genHtmlList = EpubEngine.makeFileTextToHtml(path, baseUrl);
						publishProgress((delim * nProgress) + (int) (delim * 0.2));

						// set book info
						BookInfo bookInfo = new BookInfo();
						bookInfo.setBookType(BOOK_TYPE.TEXT);
						bookInfo.setTitle(title);
						bookInfo.setAuthor("Unknown");
						bookInfo.setIsbn("");
						bookInfo.setCoverImg(null);
						bookInfo.setPath(path);
						publishProgress((delim * nProgress) + (int) (delim * 0.3));

						// set table of contents
						TableOfContent toc = new TableOfContent();
						for (int j = 0; j < genHtmlList.size(); j++) {
							Chapter chapter = new Chapter();
							chapter.setSeq(String.valueOf(j + 1));
							chapter.setTitle(String.format("Chapter %02d", (j + 1)));
							chapter.setUrl(genHtmlList.get(j));
							toc.addChapter(chapter);
						}
						publishProgress((delim * nProgress) + (int) (delim * 0.5));

						long bookId = insertBookInfo(bookInfo);
						publishProgress((delim * nProgress) + (int) (delim * 0.75));
						insertTableOfContents(toc, bookId);
						publishProgress((delim * nProgress) + (int) (delim * 1.0));
					}
					catch (IOException e) {
						e.printStackTrace();
						mFailCount++;

						// delete generated files
						try {
							FileUtils.deleteDirectory(new File(baseUrl));
						}
						catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				// for html files
				else if (path.toLowerCase().lastIndexOf(".htm") > 0 || path.toLowerCase().lastIndexOf(".html") > 0) {
					String title = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
					String fileName = path.substring(path.lastIndexOf("/") + 1);
					publishProgress((delim * nProgress) + (int) (delim * 0.1));

					// set book info
					BookInfo bookInfo = new BookInfo();
					bookInfo.setBookType(BOOK_TYPE.HTML);
					bookInfo.setTitle(title);
					bookInfo.setAuthor("Unknown");
					bookInfo.setIsbn("");
					bookInfo.setCoverImg(null);
					bookInfo.setPath(path);
					publishProgress((delim * nProgress) + (int) (delim * 0.2));

					// set table of contents
					TableOfContent toc = new TableOfContent();
					Chapter chapter = new Chapter();
					chapter.setSeq(String.valueOf(1));
					chapter.setTitle("Chapter");
					chapter.setUrl(fileName);
					toc.addChapter(chapter);
					publishProgress((delim * nProgress) + (int) (delim * 0.5));

					long bookId = insertBookInfo(bookInfo);
					publishProgress((delim * nProgress) + (int) (delim * 0.75));
					insertTableOfContents(toc, bookId);
					publishProgress((delim * nProgress) + (int) (delim * 1.0));
				}
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Long result) {
			// clean dialog
			progressDialog.dismiss();
			removeDialog(DIALOG_IMPORT);

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// toast a message
					String msg = getString(R.string.msg_import_succeed, (mTotalCount - mFailCount), mTotalCount);
					// msg = String.format(msg, (totalCount - failCount), totalCount);
					Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

					// refresh bookshelf
					BookShelfActivity.this.refreshList();
				}
			});
		}
	}

	/**
	 * Inserts BookInfo into DB
	 * 
	 * @param bookInfo
	 * @return
	 */
	private synchronized long insertBookInfo(BookInfo bookInfo) {
		long bookId = 0;
		SQLiteDatabase db = mChaekDB.getWritableDatabase();

		// insert into BookInfo
		ContentValues values = new ContentValues();
		values.put(CL_BOOK_TYPE, bookInfo.getBookType().name());
		values.put(CL_BOOK_TITLE, bookInfo.getTitle());
		values.put(CL_BOOK_AUTHOR, bookInfo.getAuthor());
		values.put(CL_BOOK_PUBLISHER, bookInfo.getPublisher());
		values.put(CL_BOOK_ISBN, bookInfo.getIsbn());
		values.put(CL_BOOK_LANGUAGE, bookInfo.getLanguage());
		values.put(CL_BOOK_IMAGE, bookInfo.getCoverImg());
		values.put(CL_BOOK_PATH, bookInfo.getPath());
		bookId = db.insertOrThrow(TB_BOOK, null, values);
		db.close();

		return bookId;
	}

	/**
	 * Inserts TOC into DB
	 * 
	 * @param toc
	 * @param bookId
	 * @param baseUrl
	 */
	private synchronized void insertTableOfContents(TableOfContent toc, long bookId) {
		ArrayList<Chapter> chapterList = toc.getChapterList();
		SQLiteDatabase db = mChaekDB.getWritableDatabase();

		for (int i = 0; i < chapterList.size(); i++) {
			Chapter chapter = chapterList.get(i);

			ContentValues values = new ContentValues();
			values.put(CL_TOC_BOOK_ID, bookId);
			values.put(CL_TOC_SEQ, chapter.getSeq());
			values.put(CL_TOC_TITLE, chapter.getTitle());
			values.put(CL_TOC_URL, chapter.getUrl());
			values.put(CL_TOC_ANCHOR, chapter.getAnchor());

			db.insertOrThrow(TB_TOC, null, values);
		}

		db.close();
	}

	@Override
	protected void onDestroy() {
		adView.destroy();
		super.onDestroy();
	}

}
