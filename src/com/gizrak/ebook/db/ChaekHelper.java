package com.gizrak.ebook.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.gizrak.ebook.R;

public class ChaekHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 7;
	private static final String DATABASE_NAME = "chaek.db";

	private List<Map<String, String>> ALL_TABLES = new ArrayList<Map<String, String>>();

	/**
	 * Create a helper object for the Events database
	 * 
	 * @param context
	 */
	public ChaekHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

		this.onSetCreateQuery(context);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create Table
		for (int i = 0; i < ALL_TABLES.size(); i++) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + ALL_TABLES.get(i).get("TABLE_NAME") + " ( "
					+ ALL_TABLES.get(i).get("FIELD_NAME") + " );");
		}

		// create Index table
		db.execSQL("DROP TABLE IF EXISTS dual;");
		db.execSQL("CREATE TABLE dual (idx INTEGER PRIMARY KEY, rownum TEXT);");
		db.execSQL("CREATE UNIQUE INDEX idx_rownum ON dual (rownum);");

		// create all tables
		db.beginTransaction();
		try {
			for (int i = 1; i < 100; i++) {
				String idxStr = "0" + i;
				if (i > 9)
					idxStr = idxStr.substring(1);

				db.execSQL("INSERT INTO dual (idx, rownum) VALUES (" + i + ",'" + idxStr + "');");
			}
			db.setTransactionSuccessful();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (int i = 0; i < ALL_TABLES.size(); i++) {
			db.execSQL("DROP TABLE IF EXISTS " + ALL_TABLES.get(i).get("TABLE_NAME") + ";");
		}
		onCreate(db);
	}

	/**
	 * To Create new Table,<br>
	 * <ol>
	 * <li>Define a new [TABLE_NAME]&[FIELD_NAME] in
	 * /ROOT/res/values/database.xml File,
	 * <li>Add a new [TABLE_NAME]&[FIELD_NAME] in ALL_TABLES to below Method.
	 * <li>Update the DATABASE_VERSION value (+1)
	 * </ol>
	 * 
	 * @param context
	 */
	private void onSetCreateQuery(Context context) {
		Map<String, String> params;

		// 1. BOOK
		params = new HashMap<String, String>();
		params.put("TABLE_NAME", context.getResources().getString(R.string.BOOK_TBL_NAME));
		params.put("FIELD_NAME", context.getResources().getString(R.string.BOOK_TBL_FIELDS));
		ALL_TABLES.add(params);

		// 2. TOC
		params = new HashMap<String, String>();
		params.put("TABLE_NAME", context.getResources().getString(R.string.TOC_TBL_NAME));
		params.put("FIELD_NAME", context.getResources().getString(R.string.TOC_TBL_FIELDS));
		ALL_TABLES.add(params);

		// 3. BOOKMARK
		params = new HashMap<String, String>();
		params.put("TABLE_NAME", context.getResources().getString(R.string.BM_TBL_NAME));
		params.put("FIELD_NAME", context.getResources().getString(R.string.BM_TBL_FIELDS));
		ALL_TABLES.add(params);
	}
}