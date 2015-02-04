package com.zms.logcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.zms.logcat.prefs.Buffer;
import com.zms.logcat.prefs.Format;
import com.zms.logcat.prefs.Level;
import com.zms.logcat.prefs.Prefs;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Logcat {
	private static final long CAT_DELAY = 1;

	private Level mLevel = null;
	private String mFilter = null;
	private Pattern mFilterPattern = null;
	private boolean mRunning = false;
	private BufferedReader mReader = null;
	private boolean mIsFilterPattern;
	private Handler mHandler;
	private Buffer mBuffer;
	private Process logcatProc;
	private Context mContext;
	private ArrayList<String> mLogCache = new ArrayList<String>();
	private boolean mPlay = true;
	private long lastCat = -1;
	private boolean mAsRoot = false;
	private Runnable catRunner = new Runnable() {

		@Override
		public void run() {
			//Log.v("zxxcat", ">>>>>>>run"); //会每一秒执行一次
			if (!mPlay) {
				return;
			}
			long now = System.currentTimeMillis();
			if (now < lastCat + CAT_DELAY) {
				return;
			}
			lastCat = now;
			cat();
		}
	};
	private ScheduledExecutorService EX; //定时周期执行指定的任务

	Format mFormat;

	public Logcat(Context context, Handler handler) {
		mContext = context;
		mHandler = handler;

		Prefs prefs = new Prefs(mContext);

		mLevel = prefs.getLevel();
		mIsFilterPattern = prefs.isFilterPattern();
		mFilter = prefs.getFilter();
		mFilterPattern = prefs.getFilterPattern();
		mFormat = prefs.getFormat();
		mBuffer = prefs.getBuffer();
		
		mAsRoot = false;//prefs.isRootOn();
	}

	public void start() {
		// Log.d("alogcat", "starting ...");
		stop(); //stop EX

		mRunning = true;

		EX = Executors.newScheduledThreadPool(1);
		EX.scheduleAtFixedRate(catRunner, CAT_DELAY, CAT_DELAY,
				TimeUnit.SECONDS); //command：执行线程  initialDelay：初始化延时  period：前一次执行结束到下一次执行开始的间隔时间（间隔执行延迟时间）  unit：计时单位

		try {
			Message m = Message.obtain(mHandler, Main.CLEAR_WHAT);
			mHandler.sendMessage(m);

			List<String> progs = new ArrayList<String>();

			if(TLogcatApplication.getRoot()){
				Log.v("zxxroot", ">>>>>>>>root");
				progs.add("su");
				progs.add("-c");
			}
			progs.add("logcat"); //输出logcat的参数
			progs.add("-v");
			progs.add(mFormat.getValue());
			if (mBuffer != Buffer.MAIN) {
				progs.add("-b");
				progs.add(mBuffer.getValue());
			}
			progs.add("-s");
			progs.add("*:" + mLevel);

			logcatProc = Runtime.getRuntime()
					.exec(progs.toArray(new String[progs.size()])); 

			mReader = new BufferedReader(new InputStreamReader(
					logcatProc.getInputStream()), 1024);

			String line;
			while (mRunning&&((line = mReader.readLine()) != null) ) { //不停在这里执行 会读出空的line 但也不为全空，还会有前面的标签zxxr
				/*if((line = mReader.readLine()) == null){
					continue;
				}*/
				if (!mRunning) {
					break;
				}
				if (line.length() == 0) {
					continue;
				}
				if (mIsFilterPattern) {
					if (mFilterPattern != null
							&& !mFilterPattern.matcher(line).find()) {
						continue;
					}
				} else {
					if (mFilter != null
							&& !line.toLowerCase().contains(
									mFilter.toLowerCase())) {
						continue;
					}
				}
				//Log.v("zxxr", ">>>>>>>line="+line.toString()+"\n"); //会读到很多为空，但不全为空，包含tag
				synchronized (mLogCache) { //思路就是不停读出每一行数据，符合条件的就加入一个mLogCache，每隔一秒如果mLogCache有内容，就打印出来
					mLogCache.add(line);
				}
			}
		} catch (IOException e) {
			Log.e("alogcat", "error reading log", e);
			return;
		} finally {
			// Log.d("alogcat", "stopped");

			if (logcatProc != null) {
				logcatProc.destroy();
				logcatProc = null;
			}
			if (mReader != null) {
				try {
					mReader.close();
					mReader = null;
				} catch (IOException e) {
					Log.e("alogcat", "error closing stream", e);
				}
			}
		}
	}

	private void cat() { //打印logcat
		Message m;

		if (mLogCache.size() > 0) { 
			synchronized (mLogCache) { //在发送时不能添加
				if (mLogCache.size() > 0) {
					m = Message.obtain(mHandler, Main.CAT_WHAT);
					m.obj = mLogCache.clone(); //发送要打印出来的数据
					mLogCache.clear();
					mHandler.sendMessage(m);
					//Log.v("zxxr", ">>>>>m="+m.obj);
				}
			}
		}
		
	}

	public void stop() {
		// Log.d("alogcat", "stopping ...");
		mRunning = false;

		if (EX != null && !EX.isShutdown()) {
			EX.shutdown();
			EX = null;
		}
	}

	public boolean isRunning() {
		return mRunning;
	}

	public boolean isPlay() {
		return mPlay;
	}

	public void setPlay(boolean play) {
		mPlay = play;
	}

}
