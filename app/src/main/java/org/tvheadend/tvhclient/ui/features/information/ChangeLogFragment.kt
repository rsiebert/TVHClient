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
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ChangeLogFragment : Fragment(), BackPressedInterface {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var showFullChangeLog = false
    private var versionName: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.webview_fragment, null)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            (activity as ToolbarInterface).setTitle(getString(R.string.pref_changelog))
        }

        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            showFullChangeLog = savedInstanceState.getBoolean("showFullChangelog", true)
            versionName = savedInstanceState.getString("versionNameForChangelog", "")
        } else {
            val bundle = arguments
            if (bundle != null) {
                showFullChangeLog = bundle.getBoolean("showFullChangelog", true)
                versionName = bundle.getString("versionNameForChangelog", BuildConfig.VERSION_NAME)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showChangelog(showFullChangeLog)
    }

    override fun onPause() {
        super.onPause()
        job.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showFullChangelog", showFullChangeLog)
        outState.putString("versionNameForChangelog", versionName)
    }

    private fun showChangelog(showFullChangeLog: Boolean) {
        // Make the background transparent to remove flickering. This avoids
        // seeing the default theme background color before the stylesheets are loaded.
        webview.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webview.gone()
        loading_view.visible()

        scope.launch { loadChangeLogContents(context!!, versionName, showFullChangeLog) }
    }

    private suspend fun loadChangeLogContents(context: Context, versionName: String, showFullChangeLog: Boolean) {
        val deferredLoader = scope.async {
            return@async ChangeLogLoader(context, versionName).getChangeLogFromFile(showFullChangeLog)
        }
        // Switch the context to the main thread to call the following method
        withContext(Dispatchers.Main) {
            onFileContentsLoaded((deferredLoader.await()))
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_show_full_changelog)?.isVisible = !showFullChangeLog
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.changelog_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.menu_show_full_changelog -> {
                scope.launch { loadChangeLogContents(context!!, versionName, true) }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // Save the information that the changelog was shown
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putString("versionNameForChangelog", BuildConfig.VERSION_NAME)
        editor.apply()

        activity.let {
            if (it is RemoveFragmentFromBackstackInterface) {
                it.removeFragmentFromBackstack()
            }
        }
    }

    private fun onFileContentsLoaded(fileContent: String) {
        if (fileContent.isNotEmpty() && isVisible) {
            webview.loadDataWithBaseURL("file:///android_asset/", fileContent, "text/html", "utf-8", null)
            webview.visible()
            loading_view.gone()
        }
    }

    internal class ChangeLogLoader(context: Context, private val lastAppVersion: String) {

        private var inputStream: InputStream
        private var isLightTheme: Boolean = false
        private var listMode = ListMode.NONE
        private var stringBuffer = StringBuffer()

        // modes for HTML-Lists (bullet, numbered)
        private enum class ListMode {
            NONE, ORDERED, UNORDERED
        }

        init {
            Timber.d("Creating input stream from changelog file")
            inputStream = context.resources.openRawResource(R.raw.changelog)
            isLightTheme = getThemeId(context) == R.style.CustomTheme_Light
        }

        fun getChangeLogFromFile(full: Boolean): String {
            Timber.d("Loading full changelog $full")

            // Add the style sheet depending on the used theme
            stringBuffer = StringBuffer()
            stringBuffer.append("<html><head>")
            if (isLightTheme) {
                stringBuffer.append("<link href=\"html/styles_light.css\" type=\"text/css\" rel=\"stylesheet\"/>")
            } else {
                stringBuffer.append("<link href=\"html/styles_dark.css\" type=\"text/css\" rel=\"stylesheet\"/>")
            }
            stringBuffer.append("</head><body>")

            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            // ignore further version sections if set to true
            var advanceToEOVS = false

            // Loop through the contents for the file line by line
            while (true) {
                var line = bufferedReader.readLine() ?: break
                // Remove any spaces before or after the line
                line = line.trim()
                // Get the first character which indicates the type of content on each line
                val marker: Char = if (line.isNotEmpty()) line[0] else '0'
                if (marker == '$') {
                    // begin of a version section
                    closeList()
                    val version = line.substring(1).trim()
                    // stop output?
                    if (!full) {
                        if (lastAppVersion == version) {
                            advanceToEOVS = true
                        } else if (version == "END_OF_CHANGE_LOG") {
                            advanceToEOVS = false
                        }
                    }
                } else if (!advanceToEOVS) {
                    when (marker) {
                        '%' -> {
                            // line contains version title
                            closeList()
                            stringBuffer.append("<div class=\"title\">")
                            stringBuffer.append(line.substring(1))
                            stringBuffer.append("</div>\n")
                        }
                        '_' -> {
                            // line contains version title
                            closeList()
                            stringBuffer.append("<div class=\"subtitle\">")
                            stringBuffer.append(line.substring(1))
                            stringBuffer.append("</div>\n")
                        }
                        '!' -> {
                            // line contains free text
                            closeList()
                            stringBuffer.append("<div class=\"content\">")
                            stringBuffer.append(line.substring(1))
                            stringBuffer.append("</div>\n")
                        }
                        '#' -> {
                            // line contains numbered list item
                            openList(ListMode.ORDERED)
                            stringBuffer.append("<li class=\"list_content\">")
                            stringBuffer.append(line.substring(1))
                            stringBuffer.append("</li>\n")
                        }
                        '*' -> {
                            // line contains bullet list item
                            openList(ListMode.UNORDERED)
                            stringBuffer.append("<li class=\"list_content\">")
                            stringBuffer.append(line.substring(1))
                            stringBuffer.append("</li>\n")
                        }
                        else -> {
                            // no special character: just use line as is
                            closeList()
                            stringBuffer.append(line)
                            stringBuffer.append("\n")
                        }
                    }
                }
            }
            closeList()
            bufferedReader.close()

            stringBuffer.append("</body></html>")
            Timber.d("Done reading changelog file")
            return stringBuffer.toString()
        }

        private fun openList(listMode: ListMode) {
            if (this.listMode != listMode) {
                closeList()
                if (listMode == ListMode.ORDERED) {
                    stringBuffer.append("<ol>\n")
                } else if (listMode == ListMode.UNORDERED) {
                    stringBuffer.append("<ul>\n")
                }
                this.listMode = listMode
            }
        }

        private fun closeList() {
            if (listMode == ListMode.ORDERED) {
                stringBuffer.append("</ol>\n")
            } else if (listMode == ListMode.UNORDERED) {
                stringBuffer.append("</ul>\n")
            }
            listMode = ListMode.NONE
        }
    }
}
