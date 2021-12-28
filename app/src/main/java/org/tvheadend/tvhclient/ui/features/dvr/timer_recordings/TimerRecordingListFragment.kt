package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.os.Bundle
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.tvheadend.data.entity.TimerRecording
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecyclerviewFragmentBinding
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SearchRequestInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class TimerRecordingListFragment : BaseFragment(), RecyclerViewClickInterface, SearchRequestInterface, Filter.FilterListener {

    private lateinit var binding: RecyclerviewFragmentBinding
    private lateinit var timerRecordingViewModel: TimerRecordingViewModel
    private lateinit var recyclerViewAdapter: TimerRecordingRecyclerViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RecyclerviewFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timerRecordingViewModel = ViewModelProvider(requireActivity())[TimerRecordingViewModel::class.java]

        arguments?.let {
            timerRecordingViewModel.selectedListPosition = it.getInt("listPosition")
        }

        recyclerViewAdapter = TimerRecordingRecyclerViewAdapter(isDualPane, this, htspVersion)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = recyclerViewAdapter
        binding.recyclerView.gone()
        binding.searchProgress.visibleOrGone(baseViewModel.isSearchActive)

        timerRecordingViewModel.recordings.observe(viewLifecycleOwner,  { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addItems(recordings)
                observeSearchQuery()
            }

            binding.recyclerView.visible()
            showStatusInToolbar()
            activity?.invalidateOptionsMenu()

            if (isDualPane && recyclerViewAdapter.itemCount > 0) {
                showRecordingDetails(timerRecordingViewModel.selectedListPosition)
            }
        })
    }

    private fun observeSearchQuery() {
        Timber.d("Observing search query")
        baseViewModel.searchQueryLiveData.observe(viewLifecycleOwner,  { query ->
            if (query.isNotEmpty()) {
                Timber.d("View model returned search query '$query'")
                onSearchRequested(query)
            } else {
                Timber.d("View model returned empty search query")
                onSearchResultsCleared()
            }
        })
    }

    private fun showStatusInToolbar() {
        context?.let {
            if (!baseViewModel.isSearchActive) {
                toolbarInterface.setTitle(getString(R.string.timer_recordings))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.items, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            } else {
                toolbarInterface.setTitle(getString(R.string.search_results))
                toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.timer_recordings, recyclerViewAdapter.itemCount, recyclerViewAdapter.itemCount))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_add_recording -> return addNewTimerRecording(requireActivity())
            R.id.menu_remove_all_recordings -> showConfirmationToRemoveAllTimerRecordings(ctx, CopyOnWriteArrayList(recyclerViewAdapter.items))
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.recording_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // Enable the remove all recordings menu if there are at least 2 recordings available
        if (sharedPreferences.getBoolean("delete_all_recordings_menu_enabled", resources.getBoolean(R.bool.pref_default_delete_all_recordings_menu_enabled))
                && recyclerViewAdapter.itemCount > 1
                && isConnectionToServerAvailable) {
            menu.findItem(R.id.menu_remove_all_recordings)?.isVisible = true
        }
        menu.findItem(R.id.menu_add_recording)?.isVisible = isConnectionToServerAvailable
        menu.findItem(R.id.menu_search)?.isVisible = recyclerViewAdapter.itemCount > 0
        menu.findItem(R.id.media_route_menu_item)?.isVisible = false
    }

    private fun showRecordingDetails(position: Int) {
        timerRecordingViewModel.selectedListPosition = position
        recyclerViewAdapter.setPosition(position)

        val recording = recyclerViewAdapter.getItem(position)
        if (recording == null || !isVisible) {
            return
        }

        val fm = activity?.supportFragmentManager
        if (!isDualPane) {
            val fragment = TimerRecordingDetailsFragment.newInstance(recording.id)
            fm?.beginTransaction()?.also {
                it.replace(R.id.main, fragment)
                it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                it.addToBackStack(null)
                it.commit()
            }
        } else {
            var fragment = activity?.supportFragmentManager?.findFragmentById(R.id.details)
            if (fragment !is TimerRecordingDetailsFragment) {
                fragment = TimerRecordingDetailsFragment.newInstance(recording.id)

                // Check the lifecycle state to avoid committing the transaction
                // after the onSaveInstance method was already called which would
                // trigger an illegal state exception.
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    fm?.beginTransaction()?.also {
                        it.replace(R.id.details, fragment)
                        it.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        it.commit()
                    }
                }
            } else if (timerRecordingViewModel.currentIdLiveData.value != recording.id) {
                timerRecordingViewModel.currentIdLiveData.value = recording.id
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val ctx = context ?: return
        val timerRecording = recyclerViewAdapter.getItem(position) ?: return

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.timer_recordings_popup_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        preparePopupOrToolbarSearchMenu(popupMenu.menu, timerRecording.title, isConnectionToServerAvailable)
        popupMenu.menu.findItem(R.id.menu_disable_recording)?.isVisible = htspVersion >= 19 && timerRecording.isEnabled
        popupMenu.menu.findItem(R.id.menu_enable_recording)?.isVisible = htspVersion >= 19 && !timerRecording.isEnabled

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit_recording -> return@setOnMenuItemClickListener editSelectedTimerRecording(requireActivity(), timerRecording.id)
                R.id.menu_remove_recording -> return@setOnMenuItemClickListener showConfirmationToRemoveSelectedTimerRecording(ctx, timerRecording, null)
                R.id.menu_disable_recording -> return@setOnMenuItemClickListener enableTimerRecording(timerRecording, false)
                R.id.menu_enable_recording -> return@setOnMenuItemClickListener enableTimerRecording(timerRecording, true)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, timerRecording.title)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, timerRecording.title)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, timerRecording.title)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, timerRecording.title)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(requireActivity(), baseViewModel, timerRecording.title)
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    private fun enableTimerRecording(timerRecording: TimerRecording, enabled: Boolean): Boolean {
        val intent = timerRecordingViewModel.getIntentData(requireContext(), timerRecording)
        intent.action = "updateTimerecEntry"
        intent.putExtra("id", timerRecording.id)
        intent.putExtra("enabled", if (enabled) 1 else 0)
        activity?.startService(intent)
        return true
    }

    override fun onClick(view: View, position: Int) {
        showRecordingDetails(position)
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        showPopupMenu(view, position)
        return true
    }

    override fun onFilterComplete(i: Int) {
        binding.searchProgress.gone()
        showStatusInToolbar()
        // Preselect the first result item in the details screen
        if (isDualPane) {
            when {
                recyclerViewAdapter.itemCount > timerRecordingViewModel.selectedListPosition -> {
                    showRecordingDetails(timerRecordingViewModel.selectedListPosition)
                }
                recyclerViewAdapter.itemCount <= timerRecordingViewModel.selectedListPosition -> {
                    showRecordingDetails(0)
                }
                recyclerViewAdapter.itemCount == 0 -> {
                    removeDetailsFragment()
                }
            }
        }
    }

    override fun onSearchRequested(query: String) {
        recyclerViewAdapter.filter.filter(query, this)
    }

    override fun onSearchResultsCleared() {
        recyclerViewAdapter.filter.filter("", this)
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_timer_recordings)
    }
}
