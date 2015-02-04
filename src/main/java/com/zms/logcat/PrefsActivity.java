package com.zms.logcat;

import com.zms.logcat.prefs.Prefs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;


public class PrefsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	private ListPreference mLevelPreference;
	private ListPreference mFormatPreference;
	private ListPreference mBufferPreference;
	private ListPreference mTextsizePreference;
	private ListPreference mBackgroundColorPreference;
	
	private Prefs mPrefs;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.prefs); //设置选项布局

		mPrefs = new Prefs(this);
		
		mLevelPreference = (ListPreference) getPreferenceScreen()
		.findPreference(Prefs.LEVEL_KEY);
		mFormatPreference = (ListPreference) getPreferenceScreen()
		.findPreference(Prefs.FORMAT_KEY);
		mBufferPreference = (ListPreference) getPreferenceScreen()
		.findPreference(Prefs.BUFFER_KEY);
		mTextsizePreference = (ListPreference) getPreferenceScreen()
		.findPreference(Prefs.TEXTSIZE_KEY);
		mBackgroundColorPreference = (ListPreference) getPreferenceScreen()
		.findPreference(Prefs.BACKGROUND_COLOR_KEY);
		
		setResult(Activity.RESULT_OK);
	}

	private void setLevelTitle() { //改变选项的标题
		mLevelPreference.setTitle("类型? (" + mPrefs.getLevel().getTitle(this) + ")");
	}

	private void setFormatTitle() {
		mFormatPreference.setTitle("格式? (" + mPrefs.getFormat().getTitle(this) + ")");
	}

	private void setBufferTitle() {
		mBufferPreference.setTitle("缓冲? (" + mPrefs.getBuffer().getTitle(this) + ")");
	}

	private void setTextsizeTitle() {
		mTextsizePreference.setTitle("字体大小? (" + mPrefs.getTextsize().getTitle(this) + ")");
	}
	
	private void setBackgroundColorTitle() {
		mBackgroundColorPreference.setTitle("背景颜色? (" + mPrefs.getBackgroundColor().getTitle(this) + ")");
	}

	@Override
	protected void onResume() {
		super.onResume();

		setLevelTitle();
		setFormatTitle();
		setBufferTitle();
		setTextsizeTitle();
		setBackgroundColorTitle();
		
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);  //注册状态改变监听
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);//取消注册状态改变监听
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(Prefs.LEVEL_KEY)) {
			setLevelTitle();
		} else if (key.equals(Prefs.FORMAT_KEY)) {
			setFormatTitle();
		} else if (key.equals(Prefs.BUFFER_KEY)) {
			setBufferTitle();
		} else if (key.equals(Prefs.TEXTSIZE_KEY)) {
			setTextsizeTitle();
		} else if (key.equals(Prefs.BACKGROUND_COLOR_KEY)) {
			setBackgroundColorTitle();
		}
	}
}
