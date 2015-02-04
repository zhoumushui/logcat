package com.zms.logcat;

import com.zms.logcat.data.Lock;
import com.zms.logcat.data.LogSaver;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class SaveService extends IntentService {
	public SaveService() {
		super("saveService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		//Log.d("alogcat", "handling intent");

		LogSaver saver = new LogSaver(this);
		saver.save();

		Lock.release();
	}
}
