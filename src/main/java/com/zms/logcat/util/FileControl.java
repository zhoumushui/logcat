package com.zms.logcat.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.zms.logcat.R;
import com.zms.logcat.R.string;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class FileControl {
	Context mContext;
	boolean sdCardExist;
	public String SAVEFILE_NAME = "TchipLogcat";

	public FileControl(Context context) {

		this.mContext = context;

	}

	public String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED); // 判断sd卡是否存在

		if (sdCardExist) // 如果SD卡存在，则获取跟目录
		{
			sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
			return sdDir.toString();
		} else {
			return "";
		}

	}

	public boolean isSdExist() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			this.sdCardExist = true;
			return true;
		} else {
			this.sdCardExist = false;
			Toast.makeText(mContext, R.string.not_sd, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	public void deleteSaveData(String fileName) {
		String SAVEFILE_DIR = getSDPath() + "/" + fileName;
		File saveFile = new File(SAVEFILE_DIR);
		if (saveFile.exists()) {
			RecursionDeleteFile(saveFile);
			Toast.makeText(mContext, R.string.delete_success, Toast.LENGTH_LONG)
					.show();
		} else {
			Toast.makeText(mContext, R.string.delete_not, Toast.LENGTH_LONG)
					.show();
		}

	}

	/**
	 * 递归删除文件和文件夹
	 * 
	 * @param file
	 *            要删除的根目录
	 */
	public static void RecursionDeleteFile(File file) {
		if (file.isFile()) {
			file.delete();
			return;
		}
		if (file.isDirectory()) {
			File[] childFile = file.listFiles();
			if (childFile == null || childFile.length == 0) {
				file.delete();
				return;
			}
			for (File f : childFile) {
				RecursionDeleteFile(f);
			}
			file.delete();
		}
	}
	
	 public boolean requestRoot() { //判断有没有 root权限

			
			Process process = null;
			try {
				// Preform su to get root privledges
				process = Runtime.getRuntime().exec("su");

				// confirm that we have root
				DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
				outputStream.writeBytes("echo hello\n");

				// Close the terminal
				outputStream.writeBytes("exit\n");
				outputStream.flush();

				process.waitFor();
				if (process.exitValue() != 0) {
					Toast.makeText(mContext, R.string.not_root, Toast.LENGTH_LONG).show();
					return false;
				} else {
					// success
					Toast.makeText(mContext, R.string.root_success, Toast.LENGTH_LONG).show();
	                return true;
				}

			} catch (IOException e) {
				Log.w("root", "Cannot obtain root");
				Toast.makeText(mContext, R.string.not_root, Toast.LENGTH_LONG).show();
				return false;
			} catch (InterruptedException e) {
				Toast.makeText(mContext, R.string.not_root, Toast.LENGTH_LONG).show();
				return false;
			}

		}
	 
	 public static String createLogFilename() { //以当前时间创建保存文件名
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
	 
	 public static boolean isInvalidFilename(CharSequence filename) {
			
			String filenameAsString = null;
			
			return TextUtils.isEmpty(filename)
					|| (filenameAsString = filename.toString()).contains("/")
					|| filenameAsString.contains(":")
					|| filenameAsString.contains(" ");
					
					
	}

}
