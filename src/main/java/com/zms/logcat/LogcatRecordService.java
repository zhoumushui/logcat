package com.zms.logcat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import com.zms.logcat.data.LogLine;
import com.zms.logcat.data.SearchCriteria;
import com.zms.logcat.helper.SaveLogHelper;
import com.zms.logcat.helper.ServiceHelper;
import com.zms.logcat.reader.LogcatReader;
import com.zms.logcat.reader.LogcatReaderLoader;
import com.zms.logcat.util.ArrayUtil;
import com.zms.logcat.util.LogLineAdapterUtil;
import com.zms.logcat.util.UtilLogger;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


public class LogcatRecordService extends IntentService {

    private static final String ACTION_STOP_RECORDING = "com.tchip.logcat.action.STOP_RECORDING";
    public static final String URI_SCHEME = "logcat_recording_service";

    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_LOADER = "loader";
    public static final String EXTRA_QUERY_FILTER = "filter";
    public static final String EXTRA_LEVEL = "level";


    private static UtilLogger log = new UtilLogger(LogcatRecordService.class);

    private LogcatReader reader;

    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Method mSetForeground;
    private boolean killed;
    private final Object lock = new Object();

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            log.d("onReceive()");

            // received broadcast to kill service
            killProcess();
            ServiceHelper.stopBackgroundServiceIfRunning(context);
        }
    };

    private Handler handler;


    public LogcatRecordService() {
        super("AppTrackerService");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        log.d("onCreate()");
        Log.v("zxxr", ">>>>>>>>>>>onCreate()");
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        IntentFilter intentFilter = new IntentFilter(ACTION_STOP_RECORDING);
        intentFilter.addDataScheme(URI_SCHEME);

        registerReceiver(receiver, intentFilter);

        handler = new Handler(Looper.getMainLooper());
        //startForeground，stopForeground是从2.0开始支持的，之前的版本采用setForeground。
        try {
            mStartForeground = getClass().getMethod("startForeground", int.class, Notification.class);
            mStopForeground = getClass().getMethod("stopForeground", boolean.class);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            log.d(e, "running on older platform; couldn't find startForeground method");
            mStartForeground = mStopForeground = null;
        }
        try {
            mSetForeground = getClass().getMethod("setForeground", boolean.class);
        } catch (NoSuchMethodException e) {
            // running on newer platform
            log.d(e, "running on newer platform; couldn't find setForeground method");
            mSetForeground = null;
        }
        //为了支持2.0之前和2.0之后的版本，采用了Reflection的方法来来查找当前版本是否含有startForeground和stopForeground，如果有则调用，没有则还是使用setForeground。

    }


    private void initializeReader(Intent intent) {
        try {
            // use the "time" log so we can see what time the logs were logged at
            LogcatReaderLoader loader = intent.getParcelableExtra(EXTRA_LOADER);
            reader = loader.loadReader();

            while (!reader.readyToRecord() && !killed) { //SingleLogcatReader
                reader.readLine();
                // keep skipping lines until we find one that is past the last log line, i.e.
                // it's ready to record
            }
            if (!killed) {
                makeToast(R.string.log_recording_started, Toast.LENGTH_SHORT);
            }
        } catch (IOException e) {
            log.d(e, "");
        }

    }


    @Override
    public void onDestroy() {
        log.d("onDestroy()");
        super.onDestroy();
        Log.v("zxxstop", ">>>>>>onDestroy");
        killProcess();

        unregisterReceiver(receiver);

        stopForegroundCompat(R.string.notification_title);

        //WidgetHelper.updateWidgets(getApplicationContext(), false);


    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.
    @Override
    public void onStart(Intent intent, int startId) {
        log.d("onStart()");
        super.onStart(intent, startId);
        handleCommand(intent);
    }

    private void handleCommand(Intent intent) {

        // notify the widgets that we're running
        //WidgetHelper.updateWidgets(getApplicationContext());

        CharSequence tickerText = getText(R.string.notification_ticker);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, tickerText,
                System.currentTimeMillis());


        Intent stopRecordingIntent = new Intent();
        stopRecordingIntent.setAction(ACTION_STOP_RECORDING);
        // have to make this unique for God knows what reason
        stopRecordingIntent.setData(Uri.withAppendedPath(Uri.parse(URI_SCHEME + "://stop/"),
                Long.toHexString(new Random().nextLong())));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                0 /* no requestCode */, stopRecordingIntent, PendingIntent.FLAG_ONE_SHOT);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.notification_title),
                getText(R.string.notification_subtext), pendingIntent);

        startForegroundCompat(R.string.notification_title, notification);


    }


    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    private void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            try {
                mStartForeground.invoke(this, Integer.valueOf(id), notification); //执行上面所getMethod的方法
            } catch (InvocationTargetException e) {
                // Should not happen.
                log.d(e, "Unable to invoke startForeground");
            } catch (IllegalAccessException e) {
                // Should not happen.
                log.d(e, "Unable to invoke startForeground");
            }
            return;
        }

        // Fall back on the old API.
        if (mSetForeground != null) {
            try {
                mSetForeground.invoke(this, Boolean.TRUE);
            } catch (IllegalAccessException e) {
                // Should not happen.
                log.d(e, "Unable to invoke setForeground");
            } catch (InvocationTargetException e) {
                // Should not happen.
                log.d(e, "Unable to invoke setForeground");
            }
        }
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    private void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            try {
                mStopForeground.invoke(this, Boolean.TRUE);
            } catch (InvocationTargetException e) {
                // Should not happen.
                log.d(e, "Unable to invoke stopForeground");
            } catch (IllegalAccessException e) {
                // Should not happen.
                log.d(e, "Unable to invoke stopForeground");
            }
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        if (mSetForeground != null) {
            try {
                mSetForeground.invoke(this, Boolean.FALSE);
            } catch (IllegalAccessException e) {
                // Should not happen.
                log.d(e, "Unable to invoke setForeground");
            } catch (InvocationTargetException e) {
                // Should not happen.
                log.d(e, "Unable to invoke setForeground");
            }
        }
    }

    protected void onHandleIntent(Intent intent) {
        log.d("onHandleIntent()");
        Log.v("zxxservice", ">>>>>>onHandleIntent =" + intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        log.d("Starting up %s now with intent: %s", LogcatRecordService.class.getSimpleName(), intent);

        String filename = intent.getStringExtra(EXTRA_FILENAME); //文件名
        String queryText = intent.getStringExtra(EXTRA_QUERY_FILTER); //过滤字段
        String logLevel = intent.getStringExtra(EXTRA_LEVEL); //log level

        SearchCriteria searchCriteria = new SearchCriteria(queryText);

        CharSequence[] logLevels = getResources().getStringArray(R.array.log_levels_values);
        int logLevelLimit = ArrayUtil.indexOf(logLevels, logLevel);

        boolean searchCriteriaWillAlwaysMatch = searchCriteria.isEmpty();
        boolean logLevelAcceptsEverything = logLevelLimit == 0;

        SaveLogHelper.deleteLogIfExists(filename);

        initializeReader(intent);

        StringBuilder stringBuilder = new StringBuilder();

        try {

            String line;
            int lineCount = 0;
            int logLinePeriod = 100;//PreferenceHelper.getLogLinePeriodPreference(getApplicationContext());//每读多少行后写入sd卡
            if (TLogcatApplication.getRoot()) {
                logLinePeriod = 100;
            } else {
                logLinePeriod = 5;
            }
            while ((line = reader.readLine()) != null && !killed) {

                // filter
                if (!searchCriteriaWillAlwaysMatch || !logLevelAcceptsEverything) {
                    if (!checkLogLine(line, searchCriteria, logLevelLimit)) {
                        continue;
                    }
                }

                stringBuilder.append(line).append("\n");

                if (++lineCount % logLinePeriod == 0) { //每logLinePeriod行保存一次
                    // avoid OutOfMemoryErrors; flush now
                    SaveLogHelper.saveLog(stringBuilder, filename);
                    stringBuilder.delete(0, stringBuilder.length()); // clear
                }
            }
        } catch (IOException e) {
            log.e(e, "unexpected exception");
        } finally {
            killProcess();
            log.d("CatlogService ended");

            boolean logSaved = SaveLogHelper.saveLog(stringBuilder, filename);

            if (logSaved) { //logSaved是否保存成功
                //makeToast(R.string.log_saved, Toast.LENGTH_LONG);
                //startLogcatActivityToViewSavedFile(filename);
                String msg = getResources().getString(R.string.log_saved, Environment.getExternalStorageDirectory().toString() + "/" + SaveLogHelper.LOGFULL_DIR);
                makeToast(msg, Toast.LENGTH_LONG);
            } else {
                makeToast(R.string.unable_to_save_log, Toast.LENGTH_LONG);
            }
        }
    }

    private boolean checkLogLine(String line, SearchCriteria searchCriteria, int logLevelLimit) {
        LogLine logLine = LogLine.newLogLine(line, false);
        return searchCriteria.matches(logLine)
                && LogLineAdapterUtil.logLevelIsAcceptableGivenLogLevelLimit(logLine.getLogLevel(), logLevelLimit);
    }


    private void makeToast(final int stringResId, final int toastLength) {
        handler.post(new Runnable() {

            @Override
            public void run() {

                Toast.makeText(LogcatRecordService.this, stringResId, toastLength).show();

            }
        });

    }

    private void makeToast(final String msg, final int toastLength) {
        handler.post(new Runnable() {

            @Override
            public void run() {

                Toast.makeText(LogcatRecordService.this, msg, toastLength).show();

            }
        });
    }

    private void killProcess() {
        if (!killed) {
            synchronized (lock) {
                if (!killed && reader != null) {
                    // kill the logcat process
                    reader.killQuietly();
                    killed = true;
                }
            }
        }
    }

}
