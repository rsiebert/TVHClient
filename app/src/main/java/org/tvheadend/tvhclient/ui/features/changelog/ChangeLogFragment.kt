package org.tvheadend.tvhclient.ui.features.changelog

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.*
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.webview_fragment.*
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.gone
import org.tvheadend.tvhclient.ui.common.tasks.HtmlFileLoaderTask
import org.tvheadend.tvhclient.ui.common.visible

class ChangeLogFragment : Fragment(), BackPressedInterface, HtmlFileLoaderTask.Listener {

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
        webview.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webview.gone()
        loading_view.visible()

        ChangeLogLoaderTask(context, versionName, this).execute(showFullChangeLog)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.menu_full_changelog)?.isVisible = !showFullChangeLog
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
            webview.loadDataWithBaseURL("file:///android_asset/", fileContent, "text/html", "utf-8", null)
            webview.visible()
            loading_view.gone()
        }
    }
}
