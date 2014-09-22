package com.gizrak.ebook;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.gizrak.ebook.constant.Extras;

public class PrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final String TAG = "PrefActivity";

	public static final boolean TIP_POPUP = true;

	public static final float BRIGHTNESS_STEP = 128f; // MAX 255

	public static final String KEY_BRIGHTNESS_AUTO = "brightness_auto";
	public static final String KEY_DAY_NIGHT_MODE = "day_night_mode";
	public static final String KEY_FONT_TYPE = "font_type";
	public static final String KEY_FONT_SIZE = "font_size";
	public static final String KEY_FLING_USE = "fling_use";

	private SharedPreferences pref;
	private boolean isFontChanged = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "[CALLBACK] void onCreate()");
		addPreferencesFromResource(R.xml.preferences);

		// set pref object
		pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "[CALLBACK] void onResume()");
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "[CALLBACK] void onPause()");
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.i(TAG, "[CALLBACK] void onSharedPreferenceChanged(sharedPreferences:"
				+ sharedPreferences + ", key:" + key + ")");

		// let's do something when my counter preference value changes
		String keyName = "";
		if (KEY_BRIGHTNESS_AUTO.equals(key)) {
			keyName = getString(R.string.brightness_auto_title);
			boolean isBrightnessAuto = pref.getBoolean(PrefActivity.KEY_BRIGHTNESS_AUTO, true);
			this.setBrightness(isBrightnessAuto, BRIGHTNESS_STEP);
		}
		else if (KEY_DAY_NIGHT_MODE.equals(key)) {
			keyName = getString(R.string.day_night_mode_title);
			isFontChanged = true;
		}
		else if (KEY_FONT_TYPE.equals(key)) {
			keyName = getString(R.string.font_type_title);
			isFontChanged = true;
		}
		else if (KEY_FONT_SIZE.equals(key)) {
			keyName = getString(R.string.font_size_title);
			isFontChanged = true;
		}
		else if (KEY_FLING_USE.equals(key)) {
			keyName = getString(R.string.fling_use_title);
		}
		else {
			return;
		}

		// toast preference changed
		String msg = getString(R.string.msg_preference_changed);
		msg = String.format(msg, keyName);
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			Intent intent = getIntent();
			intent.putExtra(Extras.FONT_CHANGED, isFontChanged);
			setResult(RESULT_OK, intent);
		}
		return super.dispatchKeyEvent(event);
	}

	/**
	 * Configures Rotation
	 */
	private void setRotationLock(boolean flag) {
		//		IWindowManager windowService = android.view.IWindowManager.Stub.asInterface(ServiceManager
		//				.getService("window"));
		//		windowService.setRotationLock(flag);
	}

	/**
	 * Configures Brightness Step<br>
	 * - MAX: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
	 */
	private void setBrightness(boolean auto, float step) {
		int mode;
		if (auto) {
			mode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
		}
		else {
			mode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
		}
		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
				mode);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = step;
		getWindow().setAttributes(lp);
	}

	/**
	 * Configures Brightness Timeout<br>
	 * - 2min: 120000<br>
	 * - 5min: 300000<br>
	 * - 10min: 600000<br>
	 * - 15min: 900000<br>
	 * - never: -1
	 */
	private void setBrightness(int timeout) {
		Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
				timeout);
	}

}
