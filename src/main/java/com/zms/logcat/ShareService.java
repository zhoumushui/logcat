package com.zms.logcat;

import java.util.Date;

import com.zms.logcat.data.Lock;
import com.zms.logcat.data.LogDumper;
import com.zms.logcat.prefs.Prefs;

import android.app.IntentService;
import android.content.Intent;
import android.text.Html;
import android.util.Log;

public class ShareService extends IntentService {
	public ShareService() {
		super("saveService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		//Log.d("alogcat", "handling intent: " + intent.getAction());

		Prefs prefs = new Prefs(this);

		LogDumper dumper = new LogDumper(this);
		boolean html = prefs.isShareHtml();
		String content = dumper.dump(html);

		Intent shareIntent = new Intent(Intent.ACTION_SEND);

		// emailIntent.setType("message/rfc822");
		if (html) {
			shareIntent.setType("text/html");
		} else {
			shareIntent.setType("text/plain");
		}

		shareIntent.putExtra(
				Intent.EXTRA_SUBJECT,
				"Android Log: "
						+ Main.LOG_DATE_FORMAT.format(new Date()));
		shareIntent.putExtra(Intent.EXTRA_TEXT,
				html ? Html.fromHtml(content) : content);
		Intent chooserIntent = Intent
				.createChooser(shareIntent, "Share Android Log ...");
		chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(chooserIntent);
		
		Lock.release();
	}
}
