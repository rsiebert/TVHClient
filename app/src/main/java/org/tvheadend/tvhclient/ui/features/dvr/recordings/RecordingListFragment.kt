package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.download.DownloadRecordingManager
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import java.util.concurrent.CopyOnWriteArrayList

abstract class RecordingListFragment : BaseFragment(), RecyclerViewClickCallback, SearchRequestInterface, DownloadPermissionGrantedInterface, Filter.FilterListener {

    lateinit var viewModel: RecordingViewModel
    lateinit var recyclerViewAdapter: RecordingRecyclerViewAdapter
    var selectedListPosition: Int = 0
    var searchQuery: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0)
            searchQuery = savedInstanceState.getString(SearchManager.QUERY) ?: ""
        } else {
            selectedListPosition = 0
            searchQuery = arguments?.getString(SearchManager.QUERY) ?: ""
        }

        recyclerViewAdapter = RecordingRecyclerViewAdapter(isDualPane, this, htspVersion)
        recycler_view.layoutManager = LinearLayoutManager(activity.applicationContext)
        recycler_view.addItemDecoration(DividerItemDecoration(activity.applicationContext, LinearLayoutManager.VERTICAL))
        recycler_view.itemAnimator = DefaultItemAnimator()
        recycler_view.adapter = recyclerViewAdapter
        viewModel = ViewModelProviders.of(activity).get(RecordingViewModel::class.java)

        recycler_view.gone()
        progress_bar.visible()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("listPosition", selectedListPosition)
        outState.putString(SearchManager.QUERY, searchQuery)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add -> {
                val intent = Intent(activity, RecordingAddEditActivity::class.java)
                intent.putExtra("type", "recording")
                activity.startActivity(intent)
                true
            }
            R.id.menu_record_remove_all -> {
                val list = CopyOnWriteArrayList(recyclerViewAdapter.items)
                menuUtils.handleMenuRemoveAllRecordingsSelection(list)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.recording_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        if (sharedPreferences.getBoolean("delete_all_recordings_menu_enabled", resources.getBoolean(R.bool.pref_default_delete_all_recordings_menu_enabled))
                && recyclerViewAdapter.itemCount > 1
                && isNetworkAvailable) {
            menu.findItem(R.id.menu_record_remove_all)?.isVisible = true
        }
        // Hide the casting icon as a default.
        menu.findItem(R.id.media_route_menu_item)?.isVisible = false
        // Do not show the search menu when no recordings are available
        menu.findItem(R.id.menu_search)?.isVisible = recyclerViewAdapter.itemCount > 0
    }

    internal fun showRecordingDetails(position: Int) {
        selectedListPosition = position
        recyclerViewAdapter.setPosition(position)

        val recording = recyclerViewAdapter.getItem(position)
        if (recording == null || !isVisible
                || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        if (!isDualPane) {
            val fragment = RecordingDetailsFragment.newInstance(recording.id)
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.replace(R.id.main, fragment)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            ft.addToBackStack(null)
            ft.commit()
        } else {
            // Check what fragment is currently shown, replace if needed.
            var fragment = activity.supportFragmentManager.findFragmentById(R.id.details)
            if (fragment !is RecordingDetailsFragment || fragment.shownDvrId != recording.id) {
                // Make new fragment to show this selection.
                fragment = RecordingDetailsFragment.newInstance(recording.id)
                val ft = activity.supportFragmentManager.beginTransaction()
                ft.replace(R.id.details, fragment)
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                ft.commit()
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val recording = recyclerViewAdapter.getItem(position)
        if (activity == null || recording == null) {
            return
        }
        val popupMenu = PopupMenu(activity, view)
        popupMenu.menuInflater.inflate(R.menu.recordings_popup_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)
        prepareSearchMenu(popupMenu.menu, recording.title, isNetworkAvailable)
        prepareMenu(activity, popupMenu.menu, null, recording, isNetworkAvailable, htspVersion, isUnlocked)

        popupMenu.setOnMenuItemClickListener { item ->
            if (onMenuSelected(activity, item.itemId, recording.title)) {
                return@setOnMenuItemClickListener true
            }
            when (item.itemId) {
                R.id.menu_record_stop ->
                    return@setOnMenuItemClickListener menuUtils.handleMenuStopRecordingSelection(recording, null)
                R.id.menu_record_cancel ->
                    return@setOnMenuItemClickListener menuUtils.handleMenuCancelRecordingSelection(recording, null)
                R.id.menu_record_remove ->
                    return@setOnMenuItemClickListener menuUtils.handleMenuRemoveRecordingSelection(recording, null)
                R.id.menu_play ->
                    return@setOnMenuItemClickListener menuUtils.handleMenuPlayRecording(recording.id)
                R.id.menu_cast ->
                    return@setOnMenuItemClickListener menuUtils.handleMenuCast("dvrId", recording.id)
                R.id.menu_download -> {
                    DownloadRecordingManager(activity, recording.id)
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_edit -> {
                    val intent = Intent(activity, RecordingAddEditActivity::class.java)
                    intent.putExtra("id", recording.id)
                    intent.putExtra("type", "recording")
                    activity.startActivity(intent)
                    return@setOnMenuItemClickListener true
                }
                else ->
                    return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onClick(view: View, position: Int) {
        selectedListPosition = position
        if (view.id == R.id.icon || view.id == R.id.icon_text) {
            if (recyclerViewAdapter.itemCount > 0) {
                val recording = recyclerViewAdapter.getItem(position)
                menuUtils.handleMenuPlayRecordingIcon(recording!!.id)
            }
        } else {
            showRecordingDetails(position)
        }
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    override fun downloadRecording() {
        val recording = recyclerViewAdapter.getItem(selectedListPosition)
        recording?.let {
            DownloadRecordingManager(activity, it.id)
        }
    }

    override fun onFilterComplete(i: Int) {
        // Preselect the first result item in the details screen
        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showRecordingDetails(0)
        }
    }

    override fun onSearchRequested(query: String) {
        searchQuery = query
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared(): Boolean {
        return if (!searchQuery.isEmpty()) {
            searchQuery = ""
            recyclerViewAdapter.filter.filter("", this)
            true
        } else {
            false
        }
    }

    abstract override fun getQueryHint(): String
}
