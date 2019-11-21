package org.tvheadend.tvhclient.ui.features.changelog

import android.content.Context
import android.os.AsyncTask
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.information.HtmlFileLoaderTask
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ChangeLogLoaderTask(context: Context?, private val lastAppVersion: String, private val callback: HtmlFileLoaderTask.Listener) : AsyncTask<Boolean, Void, String>() {

    private var inputStream: InputStream? = null
    private var isLightTheme: Boolean = false
    private var listMode = ListMode.NONE
    private var stringBuffer = StringBuffer()

    // modes for HTML-Lists (bullet, numbered)
    private enum class ListMode {
        NONE, ORDERED, UNORDERED
    }

    init {
        if (context != null) {
            Timber.d("Creating input stream from changelog file")
            inputStream = context.resources.openRawResource(R.raw.changelog)
            isLightTheme = getThemeId(context) == R.style.CustomTheme_Light
        }
    }

    override fun doInBackground(vararg showFullChangeLog: Boolean?): String {
        return getChangeLogFromFile(showFullChangeLog[0] ?: false)
    }

    override fun onPostExecute(content: String) {
        callback.onFileContentsLoaded(content)
    }

    private fun getChangeLogFromFile(full: Boolean): String {
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
