package com.zms.logcat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.zms.logcat.data.LogEntry;
import com.zms.logcat.data.LogEntryAdapter;
import com.zms.logcat.data.LogSaver;
import com.zms.logcat.helper.SaveLogHelper;
import com.zms.logcat.helper.ServiceHelper;
import com.zms.logcat.prefs.BackgroundColor;
import com.zms.logcat.prefs.Format;
import com.zms.logcat.prefs.Level;
import com.zms.logcat.prefs.Prefs;
import com.zms.logcat.reader.LogcatReaderLoader;
import com.zms.logcat.util.FileControl;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


public class Main extends ListActivity {
    static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat(
            "MMM d, yyyy HH:mm:ss ZZZZ");
    private static final Executor EX = Executors.newCachedThreadPool();

    static final int FILTER_DIALOG = 1;

    private static final int PREFS_REQUEST = 1;

    private static final int MENU_FILTER = 1;
    private static final int MENU_SHARE = 5;
    private static final int MENU_PLAY = 6;
    private static final int MENU_CLEAR = 8;
    private static final int MENU_SAVE = 9;
    private static final int MENU_PREFS = 10;
    private static final int MENU_JUMP_TOP = 11; //private static final int
    private static final int MENU_JUMP_BOTTOM = 12;
    private static final int MENU_DELETE_FRONT = 13;
    private static final int MENU_DELETE_BACK = 14;
    private static final int MENU_RECORD_START = 15;
    private static final int MENU_RECORD_STOP = 16;


    static final int WINDOW_SIZE = 10000; //决定listView显示的行数

    static final int CAT_WHAT = 0;
    static final int CLEAR_WHAT = 2;

    private AlertDialog mFilterDialog;

    private ListView mLogList;
    private LogEntryAdapter mLogEntryAdapter;
    private MenuItem mPlayItem;
    private MenuItem mFilterItem;

    private Level mLastLevel = Level.V;
    private Logcat mLogcat;
    private Prefs mPrefs;
    private Context mContext;
    private boolean mPlay = true;
    private FileControl mFileControl=null;
    MenuItem startRecordItem=null;
    MenuItem stopRecordItem=null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CAT_WHAT:
                    final List<String> lines = (List<String>) msg.obj; //在Logcat类里发来的
                    cat(lines); //打印信息
                    break;
                case CLEAR_WHAT:
                    mLogEntryAdapter.clear();
                    break;
            }
        }
    };

    private void jumpTop() {
        pauseLog();
        mLogList.post(new Runnable() {
            public void run() {
                mLogList.setSelection(0); //跳转到顶部 停止滚动
            }
        });
    }

    private void jumpBottom() {
        playLog();
        mLogList.setSelection(mLogEntryAdapter.getCount() - 1);//跳转到底部 会自动滚动
    }

    private void cat(final String s) {
        if (mLogEntryAdapter.getCount() > WINDOW_SIZE) {
            mLogEntryAdapter.remove(0); //如果多于指定的数目，就移除掉第一条
        }

        Format format = mLogcat.mFormat;
        Level level = format.getLevel(s);
        if (level == null) {
            level = mLastLevel;
        } else {
            mLastLevel = level;
        }

        final LogEntry entry = new LogEntry(s, level);
        mLogEntryAdapter.add(entry); //只须知道内容、字体颜色、字体大小  就可以显示
    }

    private void cat(List<String> lines) {
        for (String line : lines) {
            cat(line);
        }
        jumpBottom(); //实现在打印的时候会动
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        //getWindow().setTitle(getResources().getString(R.string.app_name_play));
        setTitle(R.string.app_name_play);
        mContext = this;
        mPrefs = new Prefs(this);
        mFileControl=new FileControl(mContext);
        mLogList = (ListView) findViewById(android.R.id.list);//android.R.id.list
        mLogList.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {//listview长按菜单

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenuInfo menuInfo) {
                MenuItem jumpTopItem = menu.add(0, MENU_JUMP_TOP, 0,
                        R.string.jump_start_menu);
                jumpTopItem.setIcon(android.R.drawable.ic_media_previous);

                MenuItem jumpBottomItem = menu.add(0, MENU_JUMP_BOTTOM, 0,
                        R.string.jump_end_menu);
                jumpBottomItem.setIcon(android.R.drawable.ic_media_next);
            }
        });
        mLogList.setOnScrollListener(new AbsListView.OnScrollListener() { //listView上下滚动操作

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                pauseLog(); //一滚动就暂停打印log
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });

        // Log.v("alogcat", "created");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Log.i("alogcat", "new intent: " + intent);
        Log.v("zxxl", ">>>>>>>>>>>>onNewIntent intent="+intent);
		/*>>>>>>>>>>>>onNewIntent intent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10000000 cmp=org.jtb.alogcat/.LogActivity }*/
        if (intent == null) {
            return;
        }
        if (intent.getAction() == null) {
            return;
        }
        setIntent(intent);
        if (intent.getAction().equals(com.zms.logcat.data.Intent.START_INTENT)) {
            com.zms.logcat.data.Intent.handleExtras(this, intent); //通过intent设置filter和level
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Log.v("alogcat", "started");
    }

    private void init() {
        BackgroundColor bc = mPrefs.getBackgroundColor();
        int color = bc.getColor();
        mLogList.setBackgroundColor(color);
        mLogList.setCacheColorHint(color);

        mLogEntryAdapter = new LogEntryAdapter(this, R.layout.entry,
                new ArrayList<LogEntry>(WINDOW_SIZE));
        setListAdapter(mLogEntryAdapter);
        //reset();
        playLogRest();
        setKeepScreenOn();
    }

    @Override
    public void onResume() {
        //Debug.startMethodTracing("alogcat");
        super.onResume();
        onNewIntent(getIntent());
        init();
        // Log.v("alogcat", "resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Log.v("alogcat", "paused");

        //Debug.stopMethodTracing();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLogcat != null) {
            mLogcat.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Log.v("alogcat", "destroyed");
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        // Log.v("alogcat", "save instance");
    }

    @Override
    protected void onRestoreInstanceState(Bundle b) {
        // Log.v("alogcat", "restore instance");
    }

    public void reset() {
        Toast.makeText(this, R.string.reading_logs, Toast.LENGTH_SHORT).show();
        mLastLevel = Level.V;

        if (mLogcat != null) {
            mLogcat.stop();
        }

        mPlay = true;

        EX.execute(new Runnable() {
            public void run() {
                mLogcat = new Logcat(mContext, mHandler);
                mLogcat.start();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) { //每次点击隐藏的菜单时都会执行这里
        Log.v("zxxmenu", ">>>>>>>onPrepareOptionsMenu");
        boolean recordingInProgress = ServiceHelper.checkIfServiceIsRunning(mContext, LogcatRecordService.class);
        startRecordItem.setEnabled(!recordingInProgress);
        startRecordItem.setVisible(!recordingInProgress);

        stopRecordItem.setEnabled(recordingInProgress);
        stopRecordItem.setVisible(recordingInProgress);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {//标题栏上的菜单
        super.onCreateOptionsMenu(menu);
        Log.v("zxxmenu", ">>>>>>>oncreate");
        // TODO: maybe this should be in a menu.xml file. ;)
        mPlayItem = menu.add(0, MENU_PLAY, 0, R.string.pause_menu);
        mPlayItem.setIcon(android.R.drawable.ic_media_pause);
        MenuItemCompat.setShowAsAction(mPlayItem,//让菜单显示在标题栏
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        setPlayMenu();

        mFilterItem = menu.add(
                0,
                MENU_FILTER,
                0,
                getResources().getString(R.string.filter_menu,
                        mPrefs.getFilter()));
        mFilterItem.setIcon(android.R.drawable.ic_menu_search);
        MenuItemCompat.setShowAsAction(mFilterItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                        | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        setFilterMenu();

        MenuItem clearItem = menu.add(0, MENU_CLEAR, 0, R.string.clear_menu);
        clearItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        MenuItemCompat.setShowAsAction(clearItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM| MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

		/*MenuItem shareItem = menu.add(0, MENU_SHARE, 0, R.string.share_menu);
		shareItem.setIcon(android.R.drawable.ic_menu_share);
		MenuItemCompat.setShowAsAction(shareItem,
				MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);*/

        MenuItem saveItem = menu.add(0, MENU_SAVE, 0, R.string.save_menu);
        saveItem.setIcon(android.R.drawable.ic_menu_save);
        MenuItemCompat.setShowAsAction(saveItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        MenuItem prefsItem = menu.add(0, MENU_PREFS, 0, getResources()
                .getString(R.string.prefs_menu));
        prefsItem.setIcon(android.R.drawable.ic_menu_preferences);
        MenuItemCompat.setShowAsAction(prefsItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);


        MenuItem deleteFrontItem = menu.add(0, MENU_DELETE_FRONT, 0, R.string.item_delete_front);
        deleteFrontItem.setIcon(android.R.drawable.ic_menu_delete);
        MenuItemCompat.setShowAsAction(deleteFrontItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItem deleteBackItem = menu.add(0, MENU_DELETE_BACK, 0, R.string.item_delete_back);
        deleteBackItem.setIcon(android.R.drawable.ic_menu_delete);
        MenuItemCompat.setShowAsAction(deleteBackItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        boolean recordingInProgress = ServiceHelper.checkIfServiceIsRunning(mContext, LogcatRecordService.class);
        startRecordItem = menu.add(0, MENU_RECORD_START, 0, R.string.item_record_start);
        startRecordItem.setIcon(R.drawable.ic_menu_record_start);
        MenuItemCompat.setShowAsAction(startRecordItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM| MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        stopRecordItem = menu.add(0, MENU_RECORD_STOP, 0, R.string.item_record_stop);
        stopRecordItem.setIcon(R.drawable.ic_menu_record_stop);
        MenuItemCompat.setShowAsAction(stopRecordItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM| MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        startRecordItem.setEnabled(!recordingInProgress);
        startRecordItem.setVisible(!recordingInProgress);

        stopRecordItem.setEnabled(recordingInProgress);
        stopRecordItem.setVisible(recordingInProgress);

        return true;
    }

	/*@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}*/

    public void setPlayMenu() { //改变menu按键的图标
        if (mPlayItem == null) {
            return;
        }
        if (mPlay) {
            mPlayItem.setTitle(R.string.pause_menu);
            mPlayItem.setIcon(android.R.drawable.ic_media_pause);
        } else {
            mPlayItem.setTitle(R.string.play_menu);
            mPlayItem.setIcon(android.R.drawable.ic_media_play);
        }
    }

    void setFilterMenu() { //把过滤的条件显示在标题上
        if (mFilterItem == null) {
            return;
        }
        int filterMenuId;
        String filter = mPrefs.getFilter();
        if (filter == null || filter.length() == 0) {
            filterMenuId = R.string.filter_menu_empty;
        } else {
            filterMenuId = R.string.filter_menu;
        }
        mFilterItem.setTitle(getResources().getString(filterMenuId, filter));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_FILTER:
                showDialog(FILTER_DIALOG); //会自动调用onCreateDialog  系统方法 －－good
                return true;
            case MENU_SHARE:
                share();
                return true;
            case MENU_SAVE:
                if(mFileControl.isSdExist()){
                    File f = save();
                    String msg = getResources().getString(R.string.saving_log,
                            f.toString()); //得到file的路径 f.toString()
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
                return true;
            case MENU_PLAY:
                if (mPlay) {
                    pauseLog();
                } else {
                    jumpBottom();
                }
                return true;
            case MENU_CLEAR:
                clear();
                //reset();
                playLogRest();
                return true;
            case MENU_PREFS:
                Intent intent = new Intent(this, PrefsActivity.class);
                startActivityForResult(intent, PREFS_REQUEST);
                return true;
            case MENU_DELETE_FRONT:
                deleteSave(mFileControl.SAVEFILE_NAME);
                return true;
            case MENU_DELETE_BACK:
                deleteSave(SaveLogHelper.LOGFULL_DIR);
                return true;
            case MENU_RECORD_START:
                if (!mFileControl.isSdExist()) {
                    Toast.makeText(mContext, R.string.not_sd, Toast.LENGTH_SHORT).show();
                }else {
                    startRecordDialog(mContext);
                }

                return true;
            case MENU_RECORD_STOP:
                stopRecordingLog(mContext);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PREFS_REQUEST:
                setKeepScreenOn();
                break;
        }
    }

    private void setKeepScreenOn() {
        if (mPrefs.isKeepScreenOn()) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //取消保持屏幕常亮  clearFlags
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_JUMP_TOP:
                Toast.makeText(this, R.string.jump_top,
                        Toast.LENGTH_SHORT).show();
                jumpTop();
                return true;
            case MENU_JUMP_BOTTOM:
                Toast.makeText(this, R.string.jump_bottom,
                        Toast.LENGTH_SHORT).show();
                jumpBottom();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void clear() {
        try {
            Runtime.getRuntime().exec(new String[] { "logcat", "-c" }); //以数组的形式执行命令
        } catch (IOException e) {
            Log.e("alogcat", "error clearing log", e);
        } finally {
        }
    }

    private String dump(boolean html) {
        StringBuilder sb = new StringBuilder();
        Level lastLevel = Level.V;

        // make copy to avoid CME
        List<LogEntry> entries = new ArrayList<LogEntry>(
                mLogEntryAdapter.getEntries());

        for (LogEntry le : entries) {
            if (!html) {
                sb.append(le.getText());
                sb.append('\n');
            } else {
                Level level = le.getLevel();
                if (level == null) {
                    level = lastLevel;
                } else {
                    lastLevel = level;
                }
                sb.append("<font color=\"");
                sb.append(level.getHexColor());
                sb.append("\" face=\"sans-serif\"><b>");
                sb.append(TextUtils.htmlEncode(le.getText()));
                sb.append("</b></font><br/>\n");
            }
        }

        return sb.toString();
    }

    private void share() {
        EX.execute(new Runnable() {
            public void run() {
                boolean html = mPrefs.isShareHtml();
                String content = dump(html);

                Intent shareIntent = new Intent(
                        android.content.Intent.ACTION_SEND);

                // emailIntent.setType("message/rfc822");
                if (html) {
                    shareIntent.setType("text/html");
                } else {
                    shareIntent.setType("text/plain");
                }

                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        "Android Log: " + LOG_DATE_FORMAT.format(new Date()));
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                        html ? Html.fromHtml(content) : content);
                startActivity(Intent.createChooser(shareIntent,
                        "Share Android Log ..."));
            }
        });

    }

    private File save() { //导出数据
        final File path = new File(Environment.getExternalStorageDirectory(),
                "TchipLogcat");
        final File file = new File(path + File.separator + "tlogcat."
                + LogSaver.LOG_FILE_FORMAT.format(new Date()) + ".txt");

        // String msg = "saving log to: " + file.toString();
        // Log.d("alogcat", msg);

        EX.execute(new Runnable() {
            public void run() {
                String content = dump(false);

                if (!path.exists()) {
                    path.mkdir();
                }

                BufferedWriter bw = null;
                try {
                    file.createNewFile();
                    bw = new BufferedWriter(new FileWriter(file), 1024);
                    bw.write(content);
                } catch (IOException e) {
                    Log.e("alogcat", "error saving log", e);
                } finally {
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            Log.e("alogcat", "error closing log", e);
                        }
                    }
                }
            }
        });

        return file;
    }

    private void deleteSave(String fileName){
        if(mFileControl.isSdExist()){
            mFileControl.deleteSaveData(fileName);
        }

    }



    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case FILTER_DIALOG:
                mFilterDialog = new FilterDialog(this);
                return mFilterDialog;
        }
        return null;
    }

    private void pauseLog() {
        if (!mPlay) {
            return;
        }
        getWindow()
                .setTitle(getResources().getString(R.string.app_name_paused)); //设置标题为暂停状态
        if (mLogcat != null) {
            mLogcat.setPlay(false);
            mPlay = false;
        }
        setPlayMenu();
    }

    private void playLog() {
        if (mPlay) {
            return;
        }
        getWindow().setTitle(getResources().getString(R.string.app_name_play));
        if (mLogcat != null) {
            mLogcat.setPlay(true);
            mPlay = true;
        } else {
            reset();
        }
        setPlayMenu();
    }

    private void playLogRest() {
        if (mPlay) {
            reset();
            return;
        }
        getWindow().setTitle(getResources().getString(R.string.app_name_play));
        mLogcat.setPlay(true);
        mPlay = true;
        reset();
        setPlayMenu();
    }

    private void startRecordDialog(final Context mContext){

        AlertDialog.Builder innerBuilder = new AlertDialog.Builder(
                mContext);
        innerBuilder.setTitle(R.string.dlg_title);
        innerBuilder.setMessage(R.string.dlg_msg);
        final EditText myValue = new EditText(mContext);
        myValue.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        String value=mFileControl.createLogFilename();
        myValue.setText(value);
        myValue.setSelection(0,value.length()-4);
        innerBuilder.setView(myValue);
        innerBuilder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        String newValue = myValue.getText().toString().trim();
                        Log.v("zxxr", ">>>>>>>>fileName="+newValue);
                        if(mFileControl.isInvalidFilename(newValue)){
                            Toast.makeText(mContext, R.string.dlg_input_invalid, Toast.LENGTH_LONG).show();
                            closeDialogDisable(dialog);
                        }else {
                            startRecordService(mContext, newValue);
                            closeDialogEnable(dialog);
                            dialog.dismiss();
                        }

                    }
                });
        innerBuilder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        closeDialogEnable(dialog);
                        dialog.dismiss();
                    }
                });
        innerBuilder.create().show();
    }

    private void closeDialogDisable(DialogInterface dialog){
        try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void closeDialogEnable(DialogInterface dialog ){

        try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startRecordService(Context mContext,String fileName){
		
		/*Intent intent = new Intent(mContext, LogcatRecordService.class);
		intent.putExtra(LogcatRecordService.EXTRA_FILENAME, fileName);
		
		// load "lastLine" in the background
		LogcatReaderLoader loader = LogcatReaderLoader.create(mContext, true);
		intent.putExtra(LogcatRecordService.EXTRA_LOADER, loader);
		
		// add query text and log level
		intent.putExtra(LogcatRecordService.EXTRA_QUERY_FILTER, "");
		intent.putExtra(LogcatRecordService.EXTRA_LEVEL, "V");
		
		mContext.startService(intent);*/
        ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(mContext, fileName, "", "");
    }

    public static void stopRecordingLog(Context context) {

        ServiceHelper.stopBackgroundServiceIfRunning(context);

    }
}
