package com.gizrak.ebook;

import com.gizrak.ebook.constant.Extras;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

/**
 * Creates shortcut for each activity.<br>
 * 
 * <ul>
 * <li>CLASS_NAME: Activity for shortcut executed.</li>
 * <li>ID: _id from Database.</li>
 * <li>TITLE: Shortcut Name.</li>
 * <li>COVER_IMG: Shortcut Image.</li>
 * </ul>
 * 
 * @author gizrak
 */
public class ShortcutActivity extends Activity {

	private static final String ACTION_SHORTCUT = "android.intent.action.CREATE_SHORTCUT";
	private static final String EXTRA_KEY = "com.gizrak.ebook.ShortcutActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent i = getIntent();
		String className = i.getStringExtra(Extras.CLASS_NAME);
		long id = i.getLongExtra(Extras.ID, 1);
		String type = i.getStringExtra(Extras.TYPE);
		String title = i.getStringExtra(Extras.TITLE);
		String author = i.getStringExtra(Extras.AUTHOR);
		String path = i.getStringExtra(Extras.PATH);
		Bitmap img = (Bitmap) i.getParcelableExtra(Extras.IMAGE);

		// First, set up the shortcut intent.
		// For this example, we simply create an intent that
		// will bring us directly back to this activity.
		// A more typical implementation would use a
		// data Uri in order to display a more specific result, or a custom
		// action in order to
		// launch a specific operation.
		Intent shortcutIntent = new Intent(ACTION_SHORTCUT);
		shortcutIntent.setClassName("com.gizrak.ebook", className);
		shortcutIntent.putExtra(EXTRA_KEY, "gizrak provides this shortcut");
		shortcutIntent.putExtra(Extras.ID, id);
		shortcutIntent.putExtra(Extras.TYPE, type);
		shortcutIntent.putExtra(Extras.TITLE, title);
		shortcutIntent.putExtra(Extras.AUTHOR, author);
		shortcutIntent.putExtra(Extras.PATH, path);

		// Then, set up the container intent (the response to the caller)
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, img);

		// Now, return the result to the launcher
		intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		sendBroadcast(intent);

		finish();
	}
}