package com.gizrak.ebook;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class AboutActivity extends Activity {

	private static final String TAG = "BookShelfActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "[CALLBACK] void onCreate()");
		setContentView(R.layout.about);
	}

}
