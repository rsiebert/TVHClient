package org.tvheadend.tvhclient.features.epg;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.features.search.SearchActivity;
import org.tvheadend.tvhclient.features.search.SearchRequestInterface;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTimeSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class ProgramGuideFragment extends BaseFragment implements EpgScrollInterface, RecyclerViewClickCallback, ChannelTimeSelectionCallback, ChannelTagSelectionCallback, Filter.FilterListener, ViewPager.OnPageChangeListener, SearchRequestInterface {

    @BindView(R.id.channel_list_recycler_view)
    RecyclerView channelListRecyclerView;
    @BindView(R.id.program_list_viewpager)
    ViewPager programViewPager;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private Unbinder unbinder;
    private int selectedTimeOffset;
    private String searchQuery;
    private EpgViewModel viewModel;
    private EpgChannelListRecyclerViewAdapter channelListRecyclerViewAdapter;

    private final List<Long> startTimes = new ArrayList<>();
    private final List<Long> endTimes = new ArrayList<>();
    private int daysToShow;
    private int hoursToShow;
    private int fragmentCount;
    private EpgViewPagerAdapter viewPagerAdapter;
    private boolean enableScrolling = true;
    private LinearLayoutManager channelListRecyclerViewLayoutManager;
    private int programIdToBeEditedWhenBeingRecorded = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.epg_main_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        forceSingleScreenLayout();

        if (savedInstanceState != null) {
            selectedTimeOffset = savedInstanceState.getInt("timeOffset");
            searchQuery = savedInstanceState.getString(SearchManager.QUERY);
        } else {
            selectedTimeOffset = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                searchQuery = bundle.getString(SearchManager.QUERY);
            }
        }

        // Calculates the number of fragments in the view pager. This depends on how many days
        // shall be shown of the program guide and how many hours shall be visible per fragment.
        hoursToShow = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", "4"));
        daysToShow = Integer.parseInt(sharedPreferences.getString("days_of_epg_data", "7"));
        fragmentCount = (daysToShow * (24 / hoursToShow));

        calculateViewPagerFragmentStartAndEndTimes();

        channelListRecyclerViewAdapter = new EpgChannelListRecyclerViewAdapter(activity, this);
        channelListRecyclerViewLayoutManager = new LinearLayoutManager(activity);
        channelListRecyclerView.setLayoutManager(channelListRecyclerViewLayoutManager);
        channelListRecyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        channelListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        channelListRecyclerView.setAdapter(channelListRecyclerViewAdapter);
        channelListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != SCROLL_STATE_IDLE) {
                    enableScrolling = true;
                } else if (enableScrolling) {
                    enableScrolling = false;
                    ProgramGuideFragment.this.onScrollStateChanged();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (enableScrolling) {
                    int position = channelListRecyclerViewLayoutManager.findFirstVisibleItemPosition();
                    View v = channelListRecyclerViewLayoutManager.getChildAt(0);
                    int offset = (v == null) ? 0 : v.getTop() - recyclerView.getPaddingTop();
                    onScroll(position, offset);
                }
            }
        });

        viewPagerAdapter = new EpgViewPagerAdapter(getChildFragmentManager(), startTimes, endTimes, fragmentCount, searchQuery);
        programViewPager.setAdapter(viewPagerAdapter);
        programViewPager.setOffscreenPageLimit(2);
        programViewPager.addOnPageChangeListener(this);

        viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);
        viewModel.getChannelSubsets().observe(this, channels -> {

            progressBar.setVisibility(View.GONE);
            channelListRecyclerView.setVisibility(View.VISIBLE);
            programViewPager.setVisibility(View.VISIBLE);

            if (channels != null) {
                channelListRecyclerViewAdapter.addItems(channels);
            }
            // Show either all channels or the name of the selected
            // channel tag and the channel count in the toolbar
            ChannelTag channelTag = viewModel.getChannelTag();
            toolbarInterface.setTitle((channelTag == null) ? getString(R.string.all_channels) : channelTag.getTagName());
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items,
                    channelListRecyclerViewAdapter.getItemCount(), channelListRecyclerViewAdapter.getItemCount()));
        });

        // Observe all recordings here in case a recording shall be edited right after it was added.
        // This needs to be done in this fragment because the popup menu handling is also done here.
        viewModel.getAllRecordings().observe(this, recordings -> {
            if (recordings != null) {
                for (Recording recording : recordings) {
                    // Show the edit recording screen of the scheduled recording
                    // in case the user has selected the record and edit menu item.
                    if (recording.getEventId() == programIdToBeEditedWhenBeingRecorded
                            && programIdToBeEditedWhenBeingRecorded > 0) {
                        programIdToBeEditedWhenBeingRecorded = 0;
                        Intent intent = new Intent(activity, RecordingAddEditActivity.class);
                        intent.putExtra("id", recording.getId());
                        intent.putExtra("type", "recording");
                        activity.startActivity(intent);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("timeOffset", selectedTimeOffset);
        outState.putString(SearchManager.QUERY, searchQuery);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.checkAndUpdateChannels();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", false);
        final boolean showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled", true);

        menu.findItem(R.id.menu_genre_color_info_channels).setVisible(showGenreColors);
        menu.findItem(R.id.menu_timeframe).setVisible(isUnlocked);

        // Prevent the channel tag menu item from going into the overlay menu
        if (showChannelTagMenu) {
            menu.findItem(R.id.menu_tags).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                return menuUtils.handleMenuChannelTagsSelection(viewModel.getChannelTagId(), this);

            case R.id.menu_timeframe:
                return menuUtils.handleMenuTimeSelection(selectedTimeOffset, hoursToShow, (hoursToShow * daysToShow), this);

            case R.id.menu_genre_color_info_channels:
                return menuUtils.handleMenuGenreColorSelection();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTimeSelected(int which) {
        selectedTimeOffset = which;
        programViewPager.setCurrentItem(which);
    }

    @Override
    public void onChannelTagIdSelected(int id) {
        channelListRecyclerView.setVisibility(View.GONE);
        programViewPager.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        viewModel.setChannelTagId(id);
    }

    @Override
    public void onClick(View view, int position) {
        if (view.getId() == R.id.icon || view.getId() == R.id.icon_text) {
            ChannelSubset channel = channelListRecyclerViewAdapter.getItem(position);
            int channelIconAction = Integer.valueOf(sharedPreferences.getString("channel_icon_action", "0"));
            if (channelIconAction  == 1) {
                menuUtils.handleMenuPlayChannel(channel.getId());
            } else if (channelIconAction == 2) {
                menuUtils.handleMenuCast("channelId", channel.getId());
            }
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        // NOP
    }


    public void showPopupMenu(View view, Program program) {
        if (activity == null || program == null) {
            return;
        }

        Recording recording = appRepository.getRecordingData().getItemByEventId(program.getEventId());

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());
        menuUtils.onPreparePopupMenu(popupMenu.getMenu(), program, program.getRecording(), isNetworkAvailable);
        menuUtils.onPreparePopupSearchMenu(popupMenu.getMenu(), isNetworkAvailable);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_search_imdb:
                    return menuUtils.handleMenuSearchImdbWebsite(program.getTitle());

                case R.id.menu_search_fileaffinity:
                    return menuUtils.handleMenuSearchFileAffinityWebsite(program.getTitle());

                case R.id.menu_search_youtube:
                    return menuUtils.handleMenuSearchYoutube(program.getTitle());

                case R.id.menu_search_google:
                    return menuUtils.handleMenuSearchGoogle(program.getTitle());

                case R.id.menu_search_epg:
                    return menuUtils.handleMenuSearchEpgSelection(program.getTitle(), program.getChannelId());

                case R.id.menu_record_stop:
                    return menuUtils.handleMenuStopRecordingSelection(recording, null);

                case R.id.menu_record_cancel:
                    return menuUtils.handleMenuCancelRecordingSelection(recording, null);

                case R.id.menu_record_remove:
                    return menuUtils.handleMenuRemoveRecordingSelection(recording, null);

                case R.id.menu_record_once:
                    return menuUtils.handleMenuRecordSelection(program.getEventId());

                case R.id.menu_record_once_and_edit:
                    programIdToBeEditedWhenBeingRecorded = program.getEventId();
                    return menuUtils.handleMenuRecordSelection(program.getEventId());

                case R.id.menu_record_once_custom_profile:
                    return menuUtils.handleMenuCustomRecordSelection(program.getEventId(), program.getChannelId());

                case R.id.menu_record_series:
                    return menuUtils.handleMenuSeriesRecordSelection(program.getTitle());

                case R.id.menu_play:
                    return menuUtils.handleMenuPlayChannel(program.getChannelId());

                case R.id.menu_cast:
                    return menuUtils.handleMenuCast("channelId", program.getChannelId());

                case R.id.menu_add_notification:
                    return menuUtils.handleMenuAddNotificationSelection(program);

                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onFilterComplete(int i) {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        ChannelTag channelTag = viewModel.getChannelTag();
        toolbarInterface.setTitle((channelTag == null) ? getString(R.string.all_channels) : channelTag.getTagName());
        toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.results,
                channelListRecyclerViewAdapter.getItemCount(), channelListRecyclerViewAdapter.getItemCount()));
    }

    /**
     * Calculates the day, start and end hours that are valid for the fragment
     * at the given position. This will be saved in the lists to avoid
     * calculating this for every fragment again and so we can update the data
     * when the settings have changed.
     */
    private void calculateViewPagerFragmentStartAndEndTimes() {
        // Clear the old arrays and initialize
        // the variables to start fresh
        startTimes.clear();
        endTimes.clear();

        // Get the current time in milliseconds
        Time now = new Time(Time.getCurrentTimezone());
        now.setToNow();

        // Get the current time in milliseconds without the seconds but in 30
        // minute slots. If the current time is later then 16:30 start from
        // 16:30 otherwise from 16:00.
        Calendar calendarStartTime = Calendar.getInstance();
        int minutes = (now.minute > 30) ? 30 : 0;
        calendarStartTime.set(now.year, now.month, now.monthDay, now.hour, minutes, 0);
        long startTime = calendarStartTime.getTimeInMillis();

        // Get the offset time in milliseconds without the minutes and seconds
        long offsetTime = hoursToShow * 60 * 60 * 1000;

        // Set the start and end times for each fragment
        for (int i = 0; i < fragmentCount; ++i) {
            startTimes.add(startTime);
            endTimes.add(startTime + offsetTime - 1);
            startTime += offsetTime;
        }
    }

    @Override
    public void onScroll(int position, int offset) {
        viewModel.setVerticalScrollPosition(position);
        viewModel.setVerticalScrollOffset(offset);
        startScrolling();
    }

    @Override
    public void onScrollStateChanged() {
        startScrolling();
    }

    /**
     * Scrolls the channel list and the program lists in all available
     * fragments from the viewpager to the saved position and offset.
     * When the user swipes the viewpager to show a new fragment it will
     * not be scrolled here but in the onPageSelected method
     */
    private void startScrolling() {
        int position = viewModel.getVerticalScrollPosition();
        int offset = viewModel.getVerticalScrollOffset();

        channelListRecyclerViewLayoutManager.scrollToPositionWithOffset(position, offset);

        for (int i = 0; i < viewPagerAdapter.getRegisteredFragmentCount(); i++) {
            Fragment fragment = viewPagerAdapter.getRegisteredFragment(i);
            if (fragment instanceof EpgScrollInterface) {
                ((EpgScrollInterface) fragment).onScroll(position, offset);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // NOP
    }

    @Override
    public void onPageSelected(int position) {
        // When a fragment was selected new fragment could have been created by the viewpager.
        // Scrolls the channel list and the program lists in all available fragments
        // except the currently visible one from the viewpager to the saved position and offset.
        for (int i = (position - 1); i <= (position + 1); i++) {
            Fragment fragment = viewPagerAdapter.getRegisteredFragment(i);
            if (i != position
                    && fragment instanceof EpgScrollInterface) {
                ((EpgScrollInterface) fragment).onScroll(viewModel.getVerticalScrollPosition(), viewModel.getVerticalScrollOffset());
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // TOP
    }

    @Override
    public void onSearchRequested(String query) {
        // Start searching for programs on all channels
        Intent searchIntent = new Intent(activity, SearchActivity.class);
        searchIntent.putExtra(SearchManager.QUERY, query);
        searchIntent.setAction(Intent.ACTION_SEARCH);
        searchIntent.putExtra("type", "program_guide");
        startActivity(searchIntent);
    }

    @Override
    public boolean onSearchResultsCleared() {
        return false;
    }

    private static class EpgViewPagerAdapter extends FragmentStatePagerAdapter {

        private final SparseArray<Fragment> registeredFragments = new SparseArray<>();
        private final int fragmentCount;
        private final List<Long> startTimes;
        private final List<Long> endTimes;
        private final String searchQuery;

        EpgViewPagerAdapter(FragmentManager fragmentManager, List<Long> startTimes, List<Long> endTimes, int fragmentCount, String searchQuery) {
            super(fragmentManager);
            this.startTimes = startTimes;
            this.endTimes = endTimes;
            this.fragmentCount = fragmentCount;
            this.searchQuery = searchQuery;
        }

        @Override
        public Fragment getItem(int position) {
            boolean showTimeIndication = (position == 0);
            return EpgViewPagerFragment.newInstance(
                    startTimes.get(position),
                    endTimes.get(position),
                    showTimeIndication,
                    searchQuery);
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        @Override
        public int getCount() {
            return fragmentCount;
        }

        int getRegisteredFragmentCount() {
            return registeredFragments.size();
        }
    }
}
