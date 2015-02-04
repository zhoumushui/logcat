package com.zms.logcat.data;

import java.util.Collections;
import java.util.List;

import com.zms.logcat.R;
import com.zms.logcat.R.layout;
import com.zms.logcat.prefs.Prefs;

import android.app.Activity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class LogEntryAdapter extends ArrayAdapter<LogEntry> {
	private Activity mActivity;
	private List<LogEntry> entries;
	private Prefs mPrefs;

	public LogEntryAdapter(Activity activity, int resourceId,
			List<LogEntry> entries) {
		super(activity, resourceId, entries);
		this.mActivity = activity;
		this.entries = entries;
		this.mPrefs = new Prefs(activity);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LogEntry entry = entries.get(position);
		TextView tv;
		if (convertView == null) {
			LayoutInflater inflater = mActivity.getLayoutInflater();
			tv = (TextView) inflater.inflate(R.layout.entry, null);
		} else {
			tv = (TextView) convertView;
		}

		tv.setText(entry.getText());
		tv.setTextColor(entry.getLevel().getColor()); //根据不同的级别显示不同的颜色
		tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mPrefs.getTextsize() //第一个参数为单位 dip
				.getValue());

		return tv;
	}

	public void remove(int position) {
		LogEntry entry = entries.get(position);
		remove(entry);
	}

	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int position) {
		return false;
	}

	public LogEntry get(int position) {
		return entries.get(position);
	}
	
	public List<LogEntry> getEntries() {
		return Collections.unmodifiableList(entries); //返回一个不可修改的List
	}
}

