package org.tvheadend.tvhclient;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    private final static String TAG = Logger.class.getSimpleName();

    private static Logger mInstance;
    // File name and path for the internal logging functionality
    private File logPath = null;
    private BufferedOutputStream logfileBuffer = null;
    // The prefix with the date in each log entry
    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    public static synchronized Logger getInstance() {
        if (mInstance == null)
            mInstance = new Logger();
        return mInstance;
    }

    /**
     * Writes the given tag name and the message into the log file
     *
     * @param tag The tag which identifies who has made the log statement
     * @param msg The message that shall be logged
     */
    public void log(String tag, String msg) {
        if (BuildConfig.DEBUG_MODE) {
            Log.d(tag, msg);
        }
        if (logfileBuffer != null) {
            String timestamp = format.format(new Date()) + ": " + tag + ", " + msg + "\n";
            try {
                logfileBuffer.write(timestamp.getBytes());
            } catch (IOException e) {
                // NOP
            }
        }
    }

    /**
     *
     */
    public void saveLog() {
        if (logfileBuffer != null) {
            try {
                logfileBuffer.flush();
            } catch (IOException e) {
                // NOP
            }
        }
    }

    /**
     *
     */
    public void enableLogToFile() {
        log(TAG, "enableLogToFile() called");

        Context context = TVHClientApplication.getInstance();

        // Get the path where the logs are stored
        logPath = new File(context.getCacheDir(), "logs");
        if (!logPath.exists()) {
            if (!logPath.mkdirs()) {
                log(TAG, "enableLogToFile: Could not create directory " + logPath.getName());
                return;
            }
        }

        // Open the log file with the current date. This ensures that the log
        // files are rotated daily
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        File logFile = new File(logPath, "tvhclient_" + sdf.format(new Date().getTime()) + ".log");

        try {
            // Open the buffer to write data into the log file. Append the data.
            logfileBuffer = new BufferedOutputStream(new FileOutputStream(logFile, true));
            log(TAG, "\n\n\n");
            log(TAG, "enableLogToFile: Logging started");

            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                log(TAG, "enableLogToFile: Application version: " + info.versionName + " (" + info.versionCode + ")");
                log(TAG, "enableLogToFile: Android version: " + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")");
            } catch (PackageManager.NameNotFoundException e) {
                // NOP
            }

        } catch (IOException e) {
            // NOP
        }
    }

    /**
     * Closes the output buffer to stop logging to the defined file
     */
    public void disableLogToFile() {
        if (logfileBuffer != null) {
            try {
                logfileBuffer.flush();
                logfileBuffer.close();
                logfileBuffer = null;
            } catch (IOException e) {
                // NOP
            }
        }
        removeOldLogfiles();
    }

    /**
     * Removes any log files that are older than a week
     */
    private void removeOldLogfiles() {
        File[] files = logPath.listFiles();
        for(File f : files) {
            long diff = new Date().getTime() - f.lastModified();
            if (diff > 7 * 24 * 60 * 60 * 1000) {
                if (!f.delete()) {
                    log(TAG, "removeOldLogfiles: Could not remove file " + f.getName());
                }
            }
        }
    }
}
