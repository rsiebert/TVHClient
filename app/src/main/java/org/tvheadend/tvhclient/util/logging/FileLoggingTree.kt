package org.tvheadend.tvhclient.util.logging

import android.content.Context
import android.os.Build
import android.util.Log
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FileLoggingTree(context: Context) : BaseDebugTree() {

    private companion object {
        const val LOG_TAG = "FileLoggingTree"
    }

    private var logPath: File? = null
    private var file: File? = null

    init {
        createNewLogFile(context)
        removeLogFiles()
    }

    private fun removeLogFiles() {
        val logPath = logPath ?: return
        val currentTime = System.currentTimeMillis()
        val sevenDays = (7 * 24 * 3600 * 1000).toLong()

        for (logfile in logPath.listFiles().orEmpty()) {
            val diff = currentTime - logfile.lastModified()
            if (diff > sevenDays && !logfile.delete()) {
                // Log directly: this tree is the one writing to the file, so going through Timber
                // would re-enter this class.
                logToLogcat(Log.WARN, "Could not remove old logfile ${logfile.name}")
            }
        }
    }

    private fun createNewLogFile(context: Context) {
        val logPath = File(context.cacheDir, "logs")
        this.logPath = logPath
        if (!logPath.exists() && !logPath.mkdirs()) {
            return
        }
        // Open the log file with the current date. This ensures that the log files are rotated daily
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val logfile = File(logPath, "tvhclient_" +
                BuildConfig.VERSION_NAME + "_" +
                BuildConfig.BUILD_VERSION + "_" +
                simpleDateFormat.format(System.currentTimeMillis()) + ".log")
        file = logfile

        try {
            if (logfile.createNewFile() || logfile.exists()) {
                val stream = FileOutputStream(logfile, true)
                stream.write("Logging started\n".toByteArray())
                stream.write(("Application version: " + BuildConfig.VERSION_NAME + "(" + BuildConfig.BUILD_VERSION + ")\n").toByteArray())
                stream.write(("Application build time is " + context.getString(R.string.build_time) + ", git commit hash is " + context.getString(R.string.build_git_sha) + ")\n").toByteArray())
                stream.write(("Android version: " + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")\n").toByteArray())
                stream.close()
            }
        } catch (e: IOException) {
            logToLogcat(Log.ERROR, "Could not open logfile ${logfile.name} for writing", e)
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.INFO) {
            return
        }
        // The log file could not be created (e.g. the cache directory is not writable), so there
        // is nowhere to write to. Returning here keeps the tree from failing on every message.
        val logfile = file ?: return
        try {
            val stream = FileOutputStream(logfile, true)
            stream.write(tag?.toByteArray())
            stream.write(" ".toByteArray())
            // The log file is written for support and can leave the device, so the server address
            // and playback tickets are stripped before anything is stored.
            stream.write((LogRedaction.apply(message) + "\n").toByteArray())
            stream.close()
        } catch (e: IOException) {
            logToLogcat(Log.ERROR, "Could not write to logfile ${logfile.name}", e)
        }
    }

    /**
     * Writes directly to logcat. Used because this tree is the one writing to the file, so falling
     * back to Timber here would re-enter [log] and recurse.
     */
    @Suppress("LogNotTimber")
    private fun logToLogcat(priority: Int, message: String, throwable: Throwable? = null) {
        when {
            throwable == null -> Log.println(priority, LOG_TAG, message)
            priority >= Log.ERROR -> Log.e(LOG_TAG, message, throwable)
            else -> Log.w(LOG_TAG, message, throwable)
        }
    }
}
