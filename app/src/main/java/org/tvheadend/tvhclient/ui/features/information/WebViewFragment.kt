package org.tvheadend.tvhclient.ui.features.information

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.webview_fragment.*
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.gone
import org.tvheadend.tvhclient.ui.common.tasks.HtmlFileLoaderTask
import org.tvheadend.tvhclient.ui.common.visible
import org.tvheadend.tvhclient.util.getThemeId
import java.util.regex.Pattern

open class WebViewFragment : BaseFragment(), HtmlFileLoaderTask.Listener {

    private lateinit var htmlFileLoaderTask: HtmlFileLoaderTask
    private lateinit var website: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.webview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        forceSingleScreenLayout()

        // Make the background transparent to remove flickering. This avoids
        // seeing the default theme                                                                background color before the stylesheets are loaded.
        webview.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webview.gone()

        website = if (savedInstanceState != null) {
            savedInstanceState.getString("website", "")
        } else {
            arguments?.getString("website", "") ?: ""
        }

        when (website) {
            "information" -> toolbarInterface.setTitle(getString(R.string.pref_information))
            "help_and_support" -> toolbarInterface.setTitle(getString(R.string.help_and_support))
            "privacy_policy" -> toolbarInterface.setTitle(getString(R.string.pref_privacy_policy))
        }

        toolbarInterface.setSubtitle("")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("website", website)
    }

    override fun onResume() {
        super.onResume()
        htmlFileLoaderTask = HtmlFileLoaderTask(context!!, website, "en", this)
        htmlFileLoaderTask.execute()
    }

    override fun onPause() {
        super.onPause()
        htmlFileLoaderTask.cancel(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onFileContentsLoaded(fileContent: String) {
        val ctx = context ?: return
        var content = fileContent
        if (!content.isEmpty()) {
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
