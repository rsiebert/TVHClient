package org.tvheadend.tvhclient.ui.features.information

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.getLocale
import org.tvheadend.tvhclient.ui.common.interfaces.FileContentsLoadedInterface
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import java.io.InputStream
import java.util.regex.Pattern

class FileContentLoader(val context: Context, private val defaultLocale: String, val callback: FileContentsLoadedInterface) {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var contents = ""

    fun start(filename: String) {
        Timber.d("Starting to load contents for $filename")
        scope.launch {
            loadFileContents(filename)
        }
    }

    fun stop() {
        job.cancel()
    }

    private suspend fun loadFileContents(filename: String) {

        val deferredLoader = scope.async {
            val languageCode = PreferenceManager.getDefaultSharedPreferences(context).getString("language", getLocale(context).language)!!.substring(0, 2)
            val htmlFile = "html/" + filename + "_" + languageCode.substring(0, 2) + ".html"
            val defaultHtmlFile = "html/" + filename + "_" + defaultLocale + ".html"

            loadContentsFromAssetFile(htmlFile)
            if (contents.isEmpty()) {
                loadContentsFromAssetFile(defaultHtmlFile)
            }

            replaceCorrectStyleSheet()
            replaceApplicationVersion()

            return@async
        }
        // Switch the context to the main thread to call the following method
        withContext(Dispatchers.Main) {
            deferredLoader.await()
            callback.onFileContentsLoaded(contents)
        }
    }

    private fun replaceApplicationVersion() {
        if (contents.contains("APP_VERSION")) {
            // Replace the placeholder in the html file with the real version
            val version = BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_VERSION + ")"
            contents = Pattern.compile("APP_VERSION").matcher(contents).replaceAll(version)
        }
    }

    private fun replaceCorrectStyleSheet() {
        if (contents.contains("styles_light.css")) {
            contents = if (getThemeId(context) == R.style.CustomTheme_Light) {
                contents.replace("styles_light.css", "html/styles_light.css")
            } else {
                contents.replace("styles_light.css", "html/styles_dark.css")
            }
        }
    }

    private fun loadContentsFromAssetFile(filename: String) {
        try {
            Timber.d("Reading contents of file $filename")
            val inputStream: InputStream = context.assets.open(filename)
            contents = inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.d("Could not open or read contents of file $filename")
        }
    }
}

