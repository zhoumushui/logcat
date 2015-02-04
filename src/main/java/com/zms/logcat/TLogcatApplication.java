package com.zms.logcat;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;







import com.zms.logcat.reader.LogcatReaderLoader;
import com.zms.logcat.util.FileControl;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.util.Log;

public class TLogcatApplication extends Application {
	private static final boolean DEBUG = false;
	private FileControl myFileControl=null;
	private static  boolean isRoot;
	@Override
	public void onCreate() {
		super.onCreate();
		Log.v("zxxapp", "app>>>>>>oncreate");
		if (DEBUG) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectAll().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectAll().penaltyLog().penaltyDeath().build());
		}
		myFileControl=new FileControl(this);
		isRoot=myFileControl.requestRoot();
		//startRecordService(this);
	}
	
	public static boolean getRoot(){
		return isRoot;
	}
	
	public void startRecordService(Context mContext){
		
		Intent intent = new Intent(mContext, LogcatRecordService.class);
		intent.putExtra(LogcatRecordService.EXTRA_FILENAME, createLogFilename());
		
		// load "lastLine" in the background
		LogcatReaderLoader loader = LogcatReaderLoader.create(mContext, true);
		intent.putExtra(LogcatRecordService.EXTRA_LOADER, loader);
		
		// add query text and log level
		intent.putExtra(LogcatRecordService.EXTRA_QUERY_FILTER, "");
		intent.putExtra(LogcatRecordService.EXTRA_LEVEL, "V");
		
		mContext.startService(intent);
	}
	
	private static String createLogFilename() { //以当前时间创建保存文件名
		Date date = new Date();
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		
		DecimalFormat twoDigitDecimalFormat = new DecimalFormat("00");
		DecimalFormat fourDigitDecimalFormat = new DecimalFormat("0000");
		
		String year = fourDigitDecimalFormat.format(calendar.get(Calendar.YEAR));
		String month = twoDigitDecimalFormat.format(calendar.get(Calendar.MONTH) + 1);
		String day = twoDigitDecimalFormat.format(calendar.get(Calendar.DAY_OF_MONTH));
		String hour = twoDigitDecimalFormat.format(calendar.get(Calendar.HOUR_OF_DAY));
		String minute = twoDigitDecimalFormat.format(calendar.get(Calendar.MINUTE));
		String second = twoDigitDecimalFormat.format(calendar.get(Calendar.SECOND));
		
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(year).append("-").append(month).append("-")
				.append(day).append("-").append(hour).append("-")
				.append(minute).append("-").append(second).append("-full");
		
		stringBuilder.append(".txt");
		
		return stringBuilder.toString();
	}
}
