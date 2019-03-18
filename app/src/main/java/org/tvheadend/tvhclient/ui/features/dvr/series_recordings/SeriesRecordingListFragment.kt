package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Filter
import android.widget.ProgressBar
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface
import org.tvheadend.tvhclient.ui.common.onMenuSelected
import org.tvheadend.tvhclient.ui.common.prepareSearchMenu
import java.util.concurrent.CopyOnWriteArrayList

class SeriesRecordingListFragment : BaseFragment(), RecyclerViewClickCallback, SearchRequestInterface, Filter.FilterListener {

    @BindView(R.id.recycler_view)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.progress_bar)
    lateinit var progressBar: ProgressBar
    private var selectedListPosition: Int = 0

    private lateinit var unbinder: Unbinder
    private lateinit var recyclerViewAdapter: SeriesRecordingRecyclerViewAdapter
    private var searchQuery: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.recyclerview_fragment, container, false)
        unbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.adapter = null
        unbinder.unbind()
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

        toolbarInterface.setTitle(if (TextUtils.isEmpty(searchQuery))
            getString(R.string.series_recordings)
        else
            getString(R.string.search_results))

        recyclerViewAdapter = SeriesRecordingRecyclerViewAdapter(isDualPane, this, htspVersion)
        recyclerView.layoutManager = LinearLayoutManager(activity.applicationContext)
        recyclerView.addItemDecoration(DividerItemDecoration(activity.applicationContext, LinearLayoutManager.VERTICAL))
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = recyclerViewAdapter

        recyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        val viewModel = ViewModelProviders.of(activity).get(SeriesRecordingViewModel::class.java)
        viewModel.recordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
            }
            recyclerView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE

            if (TextUtils.isEmpty(searchQuery)) {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showRecordingDetails(selectedListPosition)
            }
            // Invalidate the menu so that the search menu item is shown in
            // case the adapter contains items now.
            activity.invalidateOptionsMenu()
        })
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
                intent.putExtra("type", "series_recording")
                activity.startActivity(intent)
                true
            }
            R.id.menu_record_remove_all -> {
                val list = CopyOnWriteArrayList(recyclerViewAdapter.items)
                menuUtils.handleMenuRemoveAllSeriesRecordingSelection(list)
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
            menu.findItem(R.id.menu_record_remove_all).isVisible = true
        }

        menu.findItem(R.id.menu_add).isVisible = isUnlocked && isNetworkAvailable
        menu.findItem(R.id.menu_search).isVisible = recyclerViewAdapter.itemCount > 0
        menu.findItem(R.id.media_route_menu_item).isVisible = false
    }

    private fun showRecordingDetails(position: Int) {
        selectedListPosition = position
        recyclerViewAdapter.setPosition(position)

        val recording = recyclerViewAdapter.getItem(position)
        if (recording == null || !isVisible
                || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        if (!isDualPane) {
            val fragment = SeriesRecordingDetailsFragment.newInstance(recording.id)
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.replace(R.id.main, fragment)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            ft.addToBackStack(null)
            ft.commit()
        } else {
            // Check what fragment is currently shown, replace if needed.
            var fragment = activity.supportFragmentManager.findFragmentById(R.id.details)
            if (fragment !is SeriesRecordingDetailsFragment || fragment.shownId == recording.id) {
                // Make new fragment to show this selection.
                fragment = SeriesRecordingDetailsFragment.newInstance(recording.id)
                val ft = activity.supportFragmentManager.beginTransaction()
                ft.replace(R.id.details, fragment)
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                ft.commit()
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val seriesRecording = recyclerViewAdapter.getItem(position)
        if (getActivity() == null || seriesRecording == null) {
            return
        }
        val popupMenu = PopupMenu(getActivity()!!, view)
        popupMenu.menuInflater.inflate(R.menu.series_recordings_popup_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        prepareSearchMenu(popupMenu.menu, seriesRecording.title, isNetworkAvailable)
        popupMenu.menu.findItem(R.id.menu_edit).isVisible = isUnlocked

        popupMenu.setOnMenuItemClickListener { item ->
            if (onMenuSelected(activity, item.itemId, seriesRecording.title)) {
                return@setOnMenuItemClickListener true
            }
            when (item.itemId) {
                R.id.menu_edit -> {
                    val intent = Intent(getActivity(), RecordingAddEditActivity::class.java)
                    intent.putExtra("id", seriesRecording.id)
                    intent.putExtra("type", "series_recording")
                    getActivity()!!.startActivity(intent)
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_record_remove -> {
                    return@setOnMenuItemClickListener menuUtils.handleMenuRemoveSeriesRecordingSelection(seriesRecording, null)
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onClick(view: View, position: Int) {
        showRecordingDetails(position)
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    override fun onFilterComplete(i: Int) {
        if (TextUtils.isEmpty(searchQuery)) {
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        } else {
            toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.series_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
        }
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
        return if (!TextUtils.isEmpty(searchQuery)) {
            searchQuery = ""
            recyclerViewAdapter.filter.filter("", this)
            true
        } else {
            false
        }
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_series_recordings)
    }
}
