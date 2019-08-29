package org.tvheadend.tvhclient.ui.features.startup

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.webview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.tasks.HtmlFileLoaderTask
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.getThemeId
import timber.log.Timber

class StartupPrivacyPolicyFragment : Fragment(), BackPressedInterface, HtmlFileLoaderTask.Listener {

    private lateinit var htmlFileLoaderTask: HtmlFileLoaderTask
    private var website: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.webview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            val toolbarInterface = activity as ToolbarInterface
            toolbarInterface.setTitle(getString(R.string.pref_privacy_policy))
            toolbarInterface.setSubtitle("")
        }
        setHasOptionsMenu(true)

        // Make the background transparent to remove flickering.
        // This avoids seeing the default theme background color
        // before the stylesheets are loaded.
        webview.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webview.gone()

        website = "privacy_policy"
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.accept_reject_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_accept -> {
                acceptPrivacyPolicy()
                true
            }
            R.id.menu_reject -> {
                rejectPrivacyPolicy()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun rejectPrivacyPolicy() {
        Timber.d("Privacy policy was rejected")
        activity?.finish()
    }

    private fun acceptPrivacyPolicy() {
        Timber.d("Privacy policy was accepted")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putBoolean("showPrivacyPolicy", false)
        editor.apply()

        activity.let {
            if (it is RemoveFragmentFromBackstackInterface) {
                it.removeFragmentFromBackstack()
            }
        }
    }

    override fun onBackPressed() {
        context?.let {
            MaterialDialog(it).show {
                message(text = "Do you accept the privacy policy")
                positiveButton(text = "Accept") {
                    acceptPrivacyPolicy()
                }
                negativeButton(text = "Reject") {
                    rejectPrivacyPolicy()
                }
            }
        }
    }

    override fun onFileContentsLoaded(fileContent: String) {
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

            webview.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null)
            loading_view.gone()
            webview.visible()
        }
    }
}