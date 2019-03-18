package org.tvheadend.tvhclient.util.logging;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.tvheadend.tvhclient.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import timber.log.Timber;

public class FileLoggingTree extends BaseDebugTree {
    private File logPath;
    private File file;

    public FileLoggingTree(Context context) {
        createNewLogFile(context);
        removeLogFiles();
    }

    private void removeLogFiles() {
        final long currentTime = System.currentTimeMillis();
        final long sevenDays = 7 * 24 * 3600 * 1000;

        if (logPath.exists()) {
            File[] files = logPath.listFiles();
            if (files != null) {
                for (File file : files) {
                    long diff = currentTime - file.lastModified();
                    if (diff > sevenDays) {
                        if (!file.delete()) {
                            Timber.d("Could not remove logfile '" + file.getName() + "'");
                        }
                    }
                }
            }
        }
    }

    private void createNewLogFile(Context context) {
        logPath = new File(context.getCacheDir(), "logs");
        if (!logPath.exists()) {
            if (!logPath.mkdirs()) {
                return;
            }
        }
        // Open the log file with the current date. This ensures that the log files are rotated daily
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        file = new File(logPath, "tvhclient_" +
                BuildConfig.VERSION_NAME + "_" +
                BuildConfig.BUILD_VERSION + "_" +
                simpleDateFormat.format(System.currentTimeMillis()) + ".log");

        try {
            if (file.createNewFile() || file.exists()) {
                FileOutputStream stream = new FileOutputStream(file, true);
                stream.write(("Logging started\n").getBytes());
                stream.write(("Application version: " + BuildConfig.VERSION_NAME + "(" + BuildConfig.BUILD_VERSION + ")\n").getBytes());
                stream.write(("Application build time is " + BuildConfig.BUILD_TIME + ", git commit hash is " + BuildConfig.GIT_SHA + ")\n").getBytes());
                stream.write(("Android version: " + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")\n").getBytes());
                stream.close();
            }
        } catch (IOException e) {
            // NOP
        }
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority == Log.VERBOSE || priority == Log.INFO) {
            return;
        }
        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            stream.write(tag.getBytes());
            stream.write((" ").getBytes());
            stream.write((message + "\n").getBytes());
            stream.close();
        } catch (IOException e) {
            // NOP
        }
    }
}
