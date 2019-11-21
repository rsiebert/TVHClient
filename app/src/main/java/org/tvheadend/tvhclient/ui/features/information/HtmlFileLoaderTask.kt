package org.tvheadend.tvhclient.ui.features.information

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.ui.common.getLocale
import timber.log.Timber
import java.io.*
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets

class HtmlFileLoaderTask(context: Context, private val file: String, private val defaultLocale: String, private val callback: Listener) : AsyncTask<Void, Void, String>() {
    private val context: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg voids: Void): String? {
        val ctx = context.get()
        return if (ctx != null) {
            loadHtmlFromFile(ctx, file)
        } else null
    }

    override fun onPostExecute(content: String) {
        callback.onFileContentsLoaded(content)
    }

    private fun loadHtmlFromFile(context: Context, filename: String): String {

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val languageCode = prefs.getString("language", getLocale(context).language)!!.substring(0, 2)
        val htmlFile = "html/" + filename + "_" + languageCode.substring(0, 2) + ".html"
        val defaultHtmlFile = "html/" + filename + "_" + defaultLocale + ".html"

        // Open the HTML file of the defined language. This is determined by
        // the defaultLocale. If the file doesn't exist, open the default (English)
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open(htmlFile)
        } catch (e: IOException) {
            Timber.e(e, "Could not open file $htmlFile")
        }

        if (inputStream == null) {
            try {
                inputStream = context.assets.open(defaultHtmlFile)
            } catch (e: IOException) {
                Timber.e(e, "Could not open default file $defaultHtmlFile")
            }
        }

        val sb = StringBuilder()
        // Try to parse the HTML contents from the given asset file. It
        // contains the feature description with the required HTML tags.
        if (inputStream != null) {
            try {
                val reader: BufferedReader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                } else {
                    BufferedReader(InputStreamReader(inputStream))
                }
                var htmlData: String
                while (true) {
                    htmlData = reader.readLine() ?: break
                    sb.append(htmlData)
                }
                reader.close()

            } catch (e: UnsupportedEncodingException) {
                Timber.e(e, "Could not create buffered reader, unsupported encoding")
            } catch (e: IOException) {
                Timber.e(e, "Error while reading contents from input stream or closing it")
            }

            try {
                inputStream.close()
            } catch (e: IOException) {
                Timber.e(e, "Error closing input stream")
            }

        }

        // Add the closing HTML tags and load show the page
        Timber.d("Done loading file")
        return sb.toString()
    }

    interface Listener {
        fun onFileContentsLoaded(fileContent: String)
    }
}
