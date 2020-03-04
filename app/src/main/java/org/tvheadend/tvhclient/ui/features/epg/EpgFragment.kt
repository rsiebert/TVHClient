package org.tvheadend.tvhclient.ui.features.epg

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.Filter
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.epg_fragment.*
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseFragment
import org.tvheadend.tvhclient.ui.base.LayoutControlInterface
import org.tvheadend.tvhclient.ui.common.*
import org.tvheadend.tvhclient.ui.common.interfaces.ChannelTagIdsSelectedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ChannelTimeSelectedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SearchRequestInterface
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

class EpgFragment : BaseFragment(), EpgScrollInterface, RecyclerViewClickInterface, ChannelTimeSelectedInterface, ChannelTagIdsSelectedInterface, Filter.FilterListener, SearchRequestInterface {

    private lateinit var epgViewModel: EpgViewModel
    private lateinit var channelListRecyclerViewAdapter: EpgChannelListRecyclerViewAdapter
    private lateinit var channelListRecyclerViewLayoutManager: LinearLayoutManager
    private lateinit var viewPagerAdapter: EpgViewPagerAdapter

    private var enableScrolling = true
    private var programIdToBeEditedWhenBeingRecorded = 0
    private var channelTags: List<ChannelTag> = ArrayList()
    private var channelCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.epg_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        epgViewModel = ViewModelProviders.of(activity!!).get(EpgViewModel::class.java)

        if (activity is LayoutControlInterface) {
            (activity as LayoutControlInterface).forceSingleScreenLayout()
        }

        channelListRecyclerViewAdapter = EpgChannelListRecyclerViewAdapter(epgViewModel, this)
        channelListRecyclerViewLayoutManager = LinearLayoutManager(activity)
        channel_list_recycler_view.layoutManager = channelListRecyclerViewLayoutManager
        channel_list_recycler_view.adapter = channelListRecyclerViewAdapter
        channel_list_recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != SCROLL_STATE_IDLE) {
                    enableScrolling = true
                } else if (enableScrolling) {
                    enableScrolling = false
                    this@EpgFragment.onScrollStateChanged()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (enableScrolling) {
                    val position = channelListRecyclerViewLayoutManager.findFirstVisibleItemPosition()
                    val v = channelListRecyclerViewLayoutManager.getChildAt(0)
                    val offset = if (v == null) 0 else v.top - recyclerView.paddingTop
                    onScroll(position, offset)
                }
            }
        })

        viewPagerAdapter = EpgViewPagerAdapter(this, epgViewModel)
        program_list_viewpager.adapter = viewPagerAdapter
        program_list_viewpager.offscreenPageLimit = 2
        program_list_viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                for (i in position - 1..position + 1) {
                    val fragment = viewPagerAdapter.getRegisteredFragment(i)
                    if (i != position && fragment is EpgScrollInterface) {
                        fragment.onScroll(epgViewModel.verticalScrollPosition, epgViewModel.verticalScrollOffset)
                    }
                }
            }
        })

        // Calculates the available display width of one minute in pixels. This depends
        // how wide the screen is and how many hours shall be shown in one screen.
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        epgViewModel.displayWidth = displayMetrics.widthPixels

        Timber.d("Observing channel tags")
        epgViewModel.channelTags.observe(viewLifecycleOwner, Observer { tags ->
            if (tags != null) {
                Timber.d("View model returned ${tags.size} channel tags")
                channelTags = tags
            }
        })

        Timber.d("Observing epg channels")
        epgViewModel.epgChannels.observe(viewLifecycleOwner, Observer { channels ->

            progress_bar?.gone()
            channel_list_recycler_view?.visible()
            program_list_viewpager?.visible()

            if (channels != null) {
                Timber.d("View model returned ${channels.size} epg channels")
                channelListRecyclerViewAdapter.addItems(channels)
            }
            // Show either all channels or the name of the selected
            // channel tag and the channel count in the toolbar
            context?.let {
                val toolbarTitle = epgViewModel.getSelectedChannelTagName(it)
                toolbarInterface.setTitle(toolbarTitle)
                toolbarInterface.setSubtitle(resources.getQuantityString(R.plurals.items,
                        channelListRecyclerViewAdapter.itemCount, channelListRecyclerViewAdapter.itemCount))
            }
        })

        Timber.d("Observing trigger to reload epg data")
        epgViewModel.viewAndEpgDataIsInvalid.observe(viewLifecycleOwner, Observer { reload ->
            Timber.d("Trigger to reload epg data has changed to $reload")
            if (reload) {
                viewPagerAdapter.notifyDataSetChanged()
            }
        })

        Timber.d("Observing epg data")
        epgViewModel.epgData.observe(viewLifecycleOwner, Observer { data ->
            data?.forEach {
                Timber.d("Loaded ${it.value.size} programs for channel ${it.key}")
            }
        })

        // Observe all recordings here in case a recording shall be edited right after it was added.
        // This needs to be done in this fragment because the popup menu handling is also done here.
        Timber.d("Observing recordings")
        epgViewModel.recordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings != null) {
                Timber.d("View model returned ${recordings.size} recordings")
                for (recording in recordings) {
                    // Show the edit recording screen of the scheduled recording
                    // in case the user has selected the record and edit menu item.
                    if (recording.eventId == programIdToBeEditedWhenBeingRecorded && programIdToBeEditedWhenBeingRecorded > 0) {
                        programIdToBeEditedWhenBeingRecorded = 0
                        editSelectedRecording(activity!!, recording.id)
                        break
                    }
                }
            }
        })

        epgViewModel.channelCount.observe(viewLifecycleOwner, Observer { count ->
            channelCount = count
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.channel_list_options_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", resources.getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled))
        val showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled", resources.getBoolean(R.bool.pref_default_channel_tag_menu_enabled))

        menu.findItem(R.id.menu_genre_color_information)?.isVisible = showGenreColors
        menu.findItem(R.id.menu_search_channels)?.isVisible = false

        // Prevent the channel tag menu item from going into the overlay menu
        if (showChannelTagMenu) {
            menu.findItem(R.id.menu_channel_tags)?.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.menu_channel_tags -> showChannelTagSelectionDialog(ctx, channelTags.toMutableList(), channelCount, this)
            R.id.menu_program_timeframe -> showProgramTimeframeSelectionDialog(ctx, epgViewModel.selectedTimeOffset, epgViewModel.hoursToShow, (24 / epgViewModel.hoursToShow) * epgViewModel.daysToShow, this)
            R.id.menu_genre_color_information -> showGenreColorDialog(ctx)
            R.id.menu_channel_sort_order -> showChannelSortOrderSelectionDialog(ctx)
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onTimeSelected(which: Int) {
        epgViewModel.selectedTimeOffset = which
        program_list_viewpager?.currentItem = which

        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        var timeInMillis = System.currentTimeMillis()
        timeInMillis += (1000 * 60 * 60 * which * epgViewModel.hoursToShow).toLong()
        epgViewModel.setSelectedTime(timeInMillis)
    }

    override fun onChannelTagIdsSelected(ids: Set<Int>) {
        channel_list_recycler_view?.gone()
        program_list_viewpager?.gone()
        progress_bar?.visible()
        epgViewModel.setSelectedChannelTagIds(ids)
    }

    override fun onClick(view: View, position: Int) {
        if ((view.id == R.id.icon || view.id == R.id.icon_text)
                && Integer.valueOf(sharedPreferences.getString("channel_icon_action", resources.getString(R.string.pref_default_channel_icon_action))!!) > 0
                && isConnectionToServerAvailable) {
            channelListRecyclerViewAdapter.getItem(position)?.let {
                playOrCastChannel(view.context, it.id, isUnlocked)
            }
        }
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        // NOP
        return true
    }

    internal fun showPopupMenu(view: View, program: EpgProgram?) {
        program ?: return
        val ctx = context ?: return
        val recording = epgViewModel.getRecordingById(program.eventId)

        val popupMenu = PopupMenu(ctx, view)
        popupMenu.menuInflater.inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.menu)
        popupMenu.menuInflater.inflate(R.menu.external_search_options_menu, popupMenu.menu)

        preparePopupOrToolbarRecordingMenu(ctx, popupMenu.menu, program.recording, isConnectionToServerAvailable, htspVersion, isUnlocked)
        preparePopupOrToolbarSearchMenu(popupMenu.menu, program.title, isConnectionToServerAvailable)
        preparePopupOrToolbarMiscMenu(ctx, popupMenu.menu, program, isConnectionToServerAvailable, isUnlocked)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_stop_recording -> return@setOnMenuItemClickListener showConfirmationToStopSelectedRecording(ctx, recording, null)
                R.id.menu_cancel_recording -> return@setOnMenuItemClickListener showConfirmationToCancelSelectedRecording(ctx, recording, null)
                R.id.menu_remove_recording -> return@setOnMenuItemClickListener showConfirmationToRemoveSelectedRecording(ctx, recording, null)
                R.id.menu_record_program -> return@setOnMenuItemClickListener recordSelectedProgram(ctx, program.eventId, epgViewModel.getRecordingProfile(), htspVersion)
                R.id.menu_record_program_and_edit -> {
                    programIdToBeEditedWhenBeingRecorded = program.eventId
                    return@setOnMenuItemClickListener recordSelectedProgram(ctx, program.eventId, epgViewModel.getRecordingProfile(), htspVersion)
                }
                R.id.menu_record_program_with_custom_profile -> return@setOnMenuItemClickListener recordSelectedProgramWithCustomProfile(ctx, program.eventId, program.channelId, epgViewModel.getRecordingProfileNames(), epgViewModel.getRecordingProfile())
                R.id.menu_record_program_as_series_recording -> return@setOnMenuItemClickListener recordSelectedProgramAsSeriesRecording(ctx, program.title, epgViewModel.getRecordingProfile(), htspVersion)
                R.id.menu_play -> return@setOnMenuItemClickListener playSelectedChannel(ctx, program.channelId, isUnlocked)
                R.id.menu_cast -> return@setOnMenuItemClickListener castSelectedChannel(ctx, program.channelId)

                R.id.menu_search_imdb -> return@setOnMenuItemClickListener searchTitleOnImdbWebsite(ctx, program.title)
                R.id.menu_search_fileaffinity -> return@setOnMenuItemClickListener searchTitleOnFileAffinityWebsite(ctx, program.title)
                R.id.menu_search_youtube -> return@setOnMenuItemClickListener searchTitleOnYoutube(ctx, program.title)
                R.id.menu_search_google -> return@setOnMenuItemClickListener searchTitleOnGoogle(ctx, program.title)
                R.id.menu_search_epg -> return@setOnMenuItemClickListener searchTitleInTheLocalDatabase(activity!!, baseViewModel, program.title, program.channelId)

                R.id.menu_add_notification -> return@setOnMenuItemClickListener addNotificationProgramIsAboutToStart(ctx, program, epgViewModel.getRecordingProfile())
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onFilterComplete(i: Int) {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        context?.let {
            val toolbarTitle = epgViewModel.getSelectedChannelTagName(it)
            toolbarInterface.setTitle(toolbarTitle)
            toolbarInterface.setSubtitle(it.resources.getQuantityString(R.plurals.results,
                    channelListRecyclerViewAdapter.itemCount, channelListRecyclerViewAdapter.itemCount))
        }
    }

    override fun onScroll(position: Int, offset: Int) {
        epgViewModel.verticalScrollPosition = position
        epgViewModel.verticalScrollOffset = offset
        startScrolling()
    }

    override fun onScrollStateChanged() {
        startScrolling()
    }

    /**
     * The channel list fragment and all program list fragment in the viewpager fragments
     * will be scrolled to the saved position and offset from the view model.
     * Only already existing fragments in the view pager will be scrolled
     */
    private fun startScrolling() {
        val position = epgViewModel.verticalScrollPosition
        val offset = epgViewModel.verticalScrollOffset

        channelListRecyclerViewLayoutManager.scrollToPositionWithOffset(position, offset)

        for (i in 0 until viewPagerAdapter.itemCount) {
            val fragment = viewPagerAdapter.getRegisteredFragment(i)
            if (fragment is EpgScrollInterface) {
                fragment.onScroll(position, offset)
            }
        }
    }

    override fun onSearchRequested(query: String) {
        // NOP
    }

    override fun onSearchResultsCleared() {
        // NOP
    }

    override fun getQueryHint(): String {
        return getString(R.string.search_program_guide)
    }

    private class EpgViewPagerAdapter internal constructor(fragment: Fragment, private val viewModel: EpgViewModel) : FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment {
            val fragment = EpgViewPagerFragment.newInstance(position)
            viewModel.registeredEpgFragments.put(position, fragment)
            return fragment
        }

        internal fun getRegisteredFragment(position: Int): Fragment? {
            return viewModel.registeredEpgFragments.get(position)
        }

        override fun getItemCount(): Int {
            return viewModel.viewPagerFragmentCount.value ?: 0
        }
    }
}
