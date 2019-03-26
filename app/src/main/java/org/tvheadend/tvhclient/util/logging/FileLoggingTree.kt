package org.tvheadend.tvhclient.util.logging

import android.content.Context
import android.os.Build
import android.util.Log
import org.tvheadend.tvhclient.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FileLoggingTree(context: Context) : BaseDebugTree() {
    private var logPath: File? = null
    private var file: File? = null

    init {
        createNewLogFile(context)
        removeLogFiles()
    }

    private fun removeLogFiles() {
        val currentTime = System.currentTimeMillis()
        val sevenDays = (7 * 24 * 3600 * 1000).toLong()

        if (logPath!!.exists()) {
            val files = logPath!!.listFiles()
            if (files != null) {
                for (file in files) {
                    val diff = currentTime - file.lastModified()
                    if (diff > sevenDays) {
                        if (!file.delete()) {
                            Timber.d("Could not remove logfile '" + file.name + "'")
                        }
                    }
                }
            }
        }
    }

    private fun createNewLogFile(context: Context) {
        logPath = File(context.cacheDir, "logs")
        if (!logPath!!.exists()) {
            if (!logPath!!.mkdirs()) {
                return
            }
        }
        // Open the log file with the current date. This ensures that the log files are rotated daily
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        file = File(logPath, "tvhclient_" +
                BuildConfig.VERSION_NAME + "_" +
                BuildConfig.BUILD_VERSION + "_" +
                simpleDateFormat.format(System.currentTimeMillis()) + ".log")

        try {
            if (file!!.createNewFile() || file!!.exists()) {
                val stream = FileOutputStream(file, true)
                stream.write("Logging started\n".toByteArray())
                stream.write(("Application version: " + BuildConfig.VERSION_NAME + "(" + BuildConfig.BUILD_VERSION + ")\n").toByteArray())
                stream.write(("Application build time is " + BuildConfig.BUILD_TIME + ", git commit hash is " + BuildConfig.GIT_SHA + ")\n").toByteArray())
                stream.write(("Android version: " + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")\n").toByteArray())
                stream.close()
            }
        } catch (e: IOException) {
            // NOP
        }

    }

    override fun log(priority: Int, tag: String, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.INFO) {
            return
        }
        try {
            val stream = FileOutputStream(file, true)
            stream.write(tag.toByteArray())
            stream.write(" ".toByteArray())
            stream.write((message + "\n").toByteArray())
            stream.close()
        } catch (e: IOException) {
            // NOP
        }

    }
}
