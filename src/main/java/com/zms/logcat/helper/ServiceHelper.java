package com.zms.logcat.helper;

import java.util.List;

import com.zms.logcat.LogcatRecordService;
import com.zms.logcat.reader.LogcatReaderLoader;
import com.zms.logcat.util.UtilLogger;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class ServiceHelper {

	private static UtilLogger log = new UtilLogger(ServiceHelper.class);
	
	
	
	public static synchronized void stopBackgroundServiceIfRunning(Context context) {
		boolean alreadyRunning = ServiceHelper.checkIfServiceIsRunning(context, LogcatRecordService.class);
		
		log.d("Is CatlogService running: %s", alreadyRunning);
		Log.v("zxxstop", ">>>>>>stopBackgroundServiceIfRunning");
		if (alreadyRunning) {
			Intent intent = new Intent(context, LogcatRecordService.class);
			context.stopService(intent); //关闭服务
		}
		
	}
	
	public static synchronized void startBackgroundServiceIfNotAlreadyRunning(
			Context context, String filename, String queryFilter, String level) {
		
		boolean alreadyRunning = ServiceHelper.checkIfServiceIsRunning(context, LogcatRecordService.class);
		
		log.d("Is CatlogService already running: %s", alreadyRunning);
		
		if (!alreadyRunning) { //打开记录服务
			
			Intent intent = new Intent(context, LogcatRecordService.class);
			intent.putExtra(LogcatRecordService.EXTRA_FILENAME, filename);
			
			// load "lastLine" in the background
			LogcatReaderLoader loader = LogcatReaderLoader.create(context, true);
			intent.putExtra(LogcatRecordService.EXTRA_LOADER, loader);
			
			// add query text and log level
			intent.putExtra(LogcatRecordService.EXTRA_QUERY_FILTER, queryFilter);
			intent.putExtra(LogcatRecordService.EXTRA_LEVEL, level);
			
			context.startService(intent);
		}
	}
	
	public static boolean checkIfServiceIsRunning(Context context, Class<?> service) { //检测服务是否还在运行
		
		String serviceName = service.getName();
		
		ComponentName componentName = new ComponentName(context.getPackageName(), serviceName);
		
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		List<ActivityManager.RunningServiceInfo> procList = activityManager.getRunningServices(Integer.MAX_VALUE);

		if (procList != null) {

			for (ActivityManager.RunningServiceInfo appProcInfo : procList) {
				if (appProcInfo != null && componentName.equals(appProcInfo.service)) {
					log.d("%s is already running", serviceName);
					return true;
				}
			}
		}
		log.d("%s is not running", serviceName);
		return false;	
	}
}
