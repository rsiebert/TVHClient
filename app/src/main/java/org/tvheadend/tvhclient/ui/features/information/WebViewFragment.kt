package org.tvheadend.tvhclient.ui.features.information

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.FileContentsLoadedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.settings.RemoveFragmentFromBackstackInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber

open class WebViewFragment : Fragment(), FileContentsLoadedInterface {

    var website: String = ""
    private var webView: WebView? = null
    private var loadingView: ProgressBar? = null
    private var errorTextView: TextView? = null
    private lateinit var fileContentLoader: FileContentLoader
    lateinit var toolbarInterface: ToolbarInterface

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(if (Build.VERSION.SDK_INT in 21..25) R.layout.webview_fragment_for_lollipop else R.layout.webview_fragment, container, false)
        webView = view.findViewById(R.id.webview)
        loadingView = view.findViewById(R.id.loading_view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }
        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        fileContentLoader = FileContentLoader(requireContext(), "en", this)
        // Make the background transparent to remove flickering. This avoids seeing
        // the default theme background color before the stylesheets are loaded.
        webView?.setBackgroundColor(Color.argb(0, 0, 0, 0))
        webView?.gone()

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        fileContentLoader.start(website)
    }

    override fun onPause() {
        super.onPause()
        fileContentLoader.stop()
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
                if (activity is RemoveFragmentFromBackstackInterface) {
                    (activity as RemoveFragmentFromBackstackInterface).removeFragmentFromBackstack()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onFileContentsLoaded(content: String) {
        Timber.d("File contents loaded")
        loadingView?.gone()
        errorTextView?.visibleOrGone(content.isEmpty())

        if (content.isNotEmpty()) {
            webView?.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null)
            webView?.visible()
        }
    }
}
