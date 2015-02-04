package com.zms.logcat.helper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.zms.logcat.TLogcatApplication;
import com.zms.logcat.util.ArrayUtil;

import android.text.TextUtils;
import android.util.Log;



/**
 * Helper functions for running processes.
 * @author nolan
 *
 */
public class RuntimeHelper { //命令执行类
    
	/**
	 * Exec the arguments, using root if necessary.
	 * @param args
	 */
	public static Process exec(List<String> args) throws IOException { //执行命令
		
		// since JellyBean, sudo is required to read other apps' logs
		if (VersionHelper.getVersionSdkIntCompat() >= VersionHelper.VERSION_JELLYBEAN //大于或等于4.2的版本 
				&& TLogcatApplication.getRoot() ) {
			Process process = Runtime.getRuntime().exec("su");
			Log.v("zxxrun", ">>>>>run su");
			PrintStream outputStream = null;
			try {
				outputStream = new PrintStream(new BufferedOutputStream(process.getOutputStream(), 8192));
				outputStream.println(TextUtils.join(" ", args));
				outputStream.flush();
			} finally {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			
			return process;
		}
		Log.v("zxxrun", ">>>>>run not su");
		return Runtime.getRuntime().exec(ArrayUtil.toArray(args, String.class));
	}
	
	public static void destroy(Process process) { //销毁进程
	    // if we're in JellyBean, then we need to kill the process as root, which requires all this
	    // extra UnixProcess logic
	    if (VersionHelper.getVersionSdkIntCompat() >= VersionHelper.VERSION_JELLYBEAN
	            && TLogcatApplication.getRoot()) { //大于或等于4.2的版本 杀进程需要root
	       SuperUserHelper.destroy(process);
	    } else {
	        process.destroy();
	    }
	}
	
}