package org.tvheadend.tvhclient.ui.features.information

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.util.getThemeId
import org.tvheadend.tvhclient.ui.common.tasks.HtmlFileLoaderTask
import java.util.regex.Pattern

open class WebViewFragment : BaseFragment(), HtmlFileLoaderTask.Listener {

    @BindView(R.id.webview)
    lateinit var webView: WebView
    @BindView(R.id.loading)
    lateinit var progressBar: ProgressBar

    private lateinit var htmlFileLoaderTask: HtmlFileLoaderTask
    private lateinit var website: String
    lateinit var unbinder: Unbinder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.webview_fragment, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        forceSingleScreenLayout()

        // Make the background transparent to remove flickering. This avoids
        // seeing the default theme                                                                background color before the stylesheets are loaded.
        webView.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webView.visibility = View.GONE

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
        htmlFileLoaderTask = HtmlFileLoaderTask(activity, website, "en", this)
        htmlFileLoaderTask.execute()
    }

    override fun onPause() {
        super.onPause()
        htmlFileLoaderTask.cancel(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onFileContentsLoaded(fileContent: String) {
        var content = fileContent
        if (!TextUtils.isEmpty(content)) {
            if (content.contains("styles_light.css")) {
                content = if (getThemeId(activity) == R.style.CustomTheme_Light) {
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

            webView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null)
            progressBar.visibility = View.GONE
            webView.visibility = View.VISIBLE
        }
    }
}
