package org.tvheadend.tvhclient.ui.features.changelog

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.*
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.tasks.HtmlFileLoaderTask

class ChangeLogFragment : Fragment(), BackPressedInterface, HtmlFileLoaderTask.Listener {

    @BindView(R.id.webview)
    lateinit var webView: WebView
    @BindView(R.id.loading)
    lateinit var progressBar: ProgressBar

    private lateinit var unbinder: Unbinder
    private var showFullChangeLog = false
    private var versionName: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.webview_fragment, null)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
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

        showChangelog(showFullChangeLog)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showFullChangelog", showFullChangeLog)
        outState.putString("versionNameForChangelog", versionName)
    }

    private fun showChangelog(showFullChangeLog: Boolean) {
        // Make the background transparent to remove flickering. This avoids
        // seeing the default theme background color before the stylesheets are loaded.
        webView.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        ChangeLogLoaderTask(context, versionName, this).execute(showFullChangeLog)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_full_changelog).isVisible = !showFullChangeLog
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.changelog_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_full_changelog -> {
                ChangeLogLoaderTask(context, versionName, this).execute(true)
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

        activity?.setResult(Activity.RESULT_OK, null)
        activity?.finish()
    }

    override fun onFileContentsLoaded(fileContent: String) {
        if (!TextUtils.isEmpty(fileContent) && isVisible) {
            webView.loadDataWithBaseURL("file:///android_asset/", fileContent, "text/html", "utf-8", null)
            webView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }
}
