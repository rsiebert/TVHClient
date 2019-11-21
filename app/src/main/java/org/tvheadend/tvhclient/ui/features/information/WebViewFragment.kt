package org.tvheadend.tvhclient.ui.features.information

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.webview_fragment.*
import kotlinx.coroutines.*
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.getLocale
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import java.io.InputStream
import java.util.regex.Pattern

open class WebViewFragment : Fragment() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    protected lateinit var toolbarInterface: ToolbarInterface
    var website: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.webview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is LayoutInterface) {
            (activity as LayoutInterface).forceSingleScreenLayout()
        }
        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        setHasOptionsMenu(true)

        // Make the background transparent to remove flickering.
        // This avoids seeing the default theme background color
        // before the stylesheets are loaded.
        webview.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webview.gone()
    }

    /**
     * Reads the contents of the HTML file of the defined language.
     * The language is determined by the locale. If the file could
     * not be loaded, then a default file (English) will be loaded.
     */
    private suspend fun loadFileContents(context: Context, filename: String, defaultLocale: String) {
        val deferredLoader = scope.async {
            val languageCode = PreferenceManager.getDefaultSharedPreferences(context).getString("language", getLocale(context).language)!!.substring(0, 2)
            val htmlFile = "html/" + filename + "_" + languageCode.substring(0, 2) + ".html"
            val defaultHtmlFile = "html/" + filename + "_" + defaultLocale + ".html"

            var contents = loadContentsFromAssetFile(context, htmlFile)
            if (contents.isEmpty()) {
                contents = loadContentsFromAssetFile(context, defaultHtmlFile)
            }
            return@async contents
        }
        // Switch the context to the main thread to call the following method
        withContext(Dispatchers.Main) {
            onFileContentsLoaded((deferredLoader.await()))
        }
    }

    private fun loadContentsFromAssetFile(context: Context, filename: String): String {
        var contents = ""
        try {
            Timber.d("Reading contents of file $filename")
            val inputStream: InputStream = context.assets.open(filename)
            contents = inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.d("Could not open or read contents of file $filename")
        }
        return contents
    }

    override fun onResume() {
        super.onResume()
        scope.launch { loadFileContents(context!!, website, "en") }
    }

    override fun onPause() {
        super.onPause()
        job.cancel()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.media_route_menu_item)?.isVisible = false
        menu.findItem(R.id.menu_search)?.isVisible = false
        menu.findItem(R.id.menu_reconnect_to_server)?.isVisible = false
        menu.findItem(R.id.menu_send_wake_on_lan_packet)?.isVisible = false
        menu.findItem(R.id.menu_privacy_policy)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity.let {
                    if (it is RemoveFragmentFromBackstackInterface) {
                        it.removeFragmentFromBackstack()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onFileContentsLoaded(fileContent: String) {
        Timber.d("File contents loaded")
        val ctx = context ?: return
        var content = fileContent
        if (content.isNotEmpty()) {
            if (content.contains("styles_light.css")) {
                content = if (getThemeId(ctx) == R.style.CustomTheme_Light) {
                    content.replace("styles_light.css", "html/styles_light.css")
                } else {
                    content.replace("styles_light.css", "html/styles_dark.css")
                }
            }
            if (content.contains("APP_VERSION")) {
                // Replace the placeholder in the html file with the real version
                val version = BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_VERSION + ")"
                content = Pattern.compile("APP_VERSION").matcher(content).replaceAll(version)
            }

            webview.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null)
            loading_view.gone()
            webview.visible()
        }
    }
}
