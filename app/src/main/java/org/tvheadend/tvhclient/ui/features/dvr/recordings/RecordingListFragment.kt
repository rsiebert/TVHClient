package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recyclerview_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.download.DownloadPermissionGrantedInterface
import org.tvheadend.tvhclient.ui.features.download.DownloadRecordingManager
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

abstract class RecordingListFragment : BaseFragment(), RecyclerViewClickCallback, SearchRequestInterface, DownloadPermissionGrantedInterface, Filter.FilterListener {

    lateinit var recordingViewModel: RecordingViewModel
    lateinit var recyclerViewAdapter: RecordingRecyclerViewAdapter
    private var selectedListPosition: Int = 0
    var searchQuery: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recyclerview_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recordingViewModel = ViewModelProviders.of(activity!!).get(RecordingViewModel::class.java)

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0)
            searchQuery = savedInstanceState.getString(SearchManager.QUERY) ?: ""
        } else {
            selectedListPosition = 0
            searchQuery = arguments?.getString(SearchManager.QUERY) ?: ""
        }

        recyclerViewAdapter = RecordingRecyclerViewAdapter(isDualPane, this, htspVersion)
        recycler_view.layoutManager = LinearLayoutManager(activity)
        recycler_view.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        recycler_view.itemAnimator = DefaultItemAnimator()
        recycler_view.adapter = recyclerViewAdapter

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
                activity?.startActivity(intent)
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

    private fun showRecordingDetails(position: Int) {
        selectedListPosition = position
        recyclerViewAdapter.setPosition(position)

        val recording = recyclerViewAdapter.getItem(position)
        if (recording == null || !isVisible) {
            return
        }

        val fm = activity?.supportFragmentManager
        if (!isDualPane) {
            val fragment = RecordingDetailsFragment.newInstance(recording.id)
            fm?.beginTransaction()?.also {
                it.replace(R.id.main, fragment)
                it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                it.addToBackStack(null)
                it.commit()
            }
        } else {
            // Check what fragment is currently shown, replace if needed.
            var fragment = activity?.supportFragmentManager?.findFragmentById(R.id.details)
            if (fragment !is RecordingDetailsFragment || fragment.shownDvrId != recording.id) {
                // Make new fragment to show this selection.
                fragment = RecordingDetailsFragment.newInstance(recording.id)
                fm?.beginTransaction()?.also {
                    it.replace(R.id.details, fragment)
                    it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    it.commit()
                }
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val ctx = context ?: return
        val recording = recyclerViewAdapter.getItem(position) ?: return

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.recordings_popup_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)
        prepareSearchMenu(popupMenu.menu, recording.title, isNetworkAvailable)
        prepareMenu(ctx, popupMenu.menu, null, recording, isNetworkAvailable, htspVersion, isUnlocked)

        popupMenu.setOnMenuItemClickListener { item ->
            if (onMenuSelected(ctx, item.itemId, recording.title)) {
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
                    activity?.let {
                        DownloadRecordingManager(it, connection, serverStatus, recording)
                    }
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_edit -> {
                    val intent = Intent(activity, RecordingAddEditActivity::class.java)
                    intent.putExtra("id", recording.id)
                    intent.putExtra("type", "recording")
                    activity?.startActivity(intent)
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_disable -> {
                    enableScheduledRecording(recording, false)
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_enable -> {
                    enableScheduledRecording(recording, true)
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
        Timber.d("Long click on item $position")
        showPopupMenu(view, position)
        return true
    }

    fun updateUI(stringId: Int) {
        recycler_view?.visible()
        progress_bar?.gone()

        if (searchQuery.isEmpty()) {
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        } else {
            toolbarInterface.setSubtitle(resources.getQuantityString(stringId, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        }

        if (isDualPane && recyclerViewAdapter.itemCount > 0) {
            showRecordingDetails(selectedListPosition)
        }
        // Invalidate the menu so that the search menu item is shown in
        // case the adapter contains items now.
        activity?.invalidateOptionsMenu()
    }

    override fun downloadRecording() {
        val rec = recyclerViewAdapter.getItem(selectedListPosition) ?: return
        activity?.let {
            DownloadRecordingManager(it, connection, serverStatus, rec)
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
        return if (searchQuery.isNotEmpty()) {
            searchQuery = ""
            recyclerViewAdapter.filter.filter("", this)
            true
        } else {
            false
        }
    }

    abstract override fun getQueryHint(): String

    private fun enableScheduledRecording(recording: Recording, enabled: Boolean) {
        val intent = recordingViewModel.getIntentData(recording)
        intent.action = "updateDvrEntry"
        intent.putExtra("id", recordingViewModel.recording.id)
        intent.putExtra("enabled", if (enabled) 1 else 0)
        activity?.startService(intent)
    }
}
