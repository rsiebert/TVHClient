package org.tvheadend.tvhclient.ui.features.information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.HideNavigationDrawerInterface
import org.tvheadend.tvhclient.ui.common.interfaces.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber

/**
 * Shows the changelog as a list of version headings and change items.
 *
 * The entries are read from the `assets/changelog.json` asset by [ChangeLogLoader] and rendered by
 * a [RecyclerView]. The former implementation parsed a marker based text file into an HTML string
 * by hand and rendered that in a `WebView`.
 */
class ChangeLogFragment : Fragment(), BackPressedInterface, HideNavigationDrawerInterface {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val adapter = ChangeLogRecyclerViewAdapter()
    private var showFullChangeLog = false
    private var versionName: String = ""

    private var recyclerView: RecyclerView? = null
    private var loadingView: ProgressBar? = null
    private var errorView: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.changelog_fragment, container, false)
        recyclerView = view.findViewById(R.id.changelog_list)
        loadingView = view.findViewById(R.id.loading_view)
        errorView = view.findViewById(R.id.errorTextView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is ToolbarInterface) {
            (activity as ToolbarInterface).setTitle(getString(R.string.pref_changelog))
        }

        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        showFullChangeLog = savedInstanceState?.getBoolean("showFullChangelog")
                ?: arguments?.getBoolean("showFullChangelog", true)
                ?: true
        versionName = savedInstanceState?.getString("versionNameForChangelog")
                ?: arguments?.getString("versionNameForChangelog", BuildConfig.VERSION_NAME)
                ?: BuildConfig.VERSION_NAME
        setHasOptionsMenu(true)

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = adapter

        Timber.d("Showing changelog, show full changelog: $showFullChangeLog")
        loadChangeLog()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("showFullChangelog", showFullChangeLog)
        outState.putString("versionNameForChangelog", versionName)
    }

    private fun loadChangeLog() {
        val loader = ChangeLogLoader(requireContext())
        scope.launch {
            val loaded = loader.load()
            if (!isAdded) {
                return@launch
            }
            showEntries(selectEntries(loaded, versionName, showFullChangeLog))

            // Record that the changelog has been shown so it is not shown again on the next start.
            // This used to happen in onBackPressed(), which misses every other way of leaving the
            // screen and therefore showed the changelog again after e.g. using the navigation menu.
            if (loaded.isNotEmpty()) {
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit { putString("versionNameForChangelog", BuildConfig.VERSION_NAME) }
            }
        }
    }

    private fun showEntries(entries: List<ChangeLogEntry>) {
        loadingView?.gone()

        val hasEntries = entries.isNotEmpty()
        recyclerView?.visibleOrGone(hasEntries)
        errorView?.visibleOrGone(!hasEntries)

        if (hasEntries) {
            adapter.submitRows(toRows(entries))
        } else {
            Timber.d("No changelog entries to show for version $versionName")
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.forEach { it.isVisible = false }
        menu.findItem(R.id.menu_show_full_changelog)?.isVisible = !showFullChangeLog
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.changelog_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.menu_show_full_changelog -> {
                showFullChangeLog = true
                activity?.invalidateOptionsMenu()
                loadChangeLog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        activity?.supportFragmentManager?.popBackStack()
    }

    companion object {

        fun newInstance(versionName: String = BuildConfig.VERSION_NAME, showFullChangelog: Boolean = true): ChangeLogFragment {
            val f = ChangeLogFragment()
            val args = Bundle()
            args.putBoolean("showFullChangelog", showFullChangelog)
            args.putString("versionNameForChangelog", versionName)
            f.arguments = args
            return f
        }
    }
}
