package org.tvheadend.tvhclient.ui.features.epg;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
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
import org.tvheadend.tvhclient.domain.entity.ChannelTag;
import org.tvheadend.tvhclient.domain.entity.EpgChannel;
import org.tvheadend.tvhclient.domain.entity.EpgProgram;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.ui.base.BaseFragment;
import org.tvheadend.tvhclient.ui.base.callbacks.RecyclerViewClickCallback;
import org.tvheadend.tvhclient.util.menu.PopupMenuUtil;
import org.tvheadend.tvhclient.util.menu.SearchMenuUtils;
import org.tvheadend.tvhclient.ui.features.channels.ChannelDisplayOptionListener;
import org.tvheadend.tvhclient.ui.features.dialogs.ChannelTagSelectionDialog;
import org.tvheadend.tvhclient.ui.features.dialogs.GenreColorDialog;
import org.tvheadend.tvhclient.ui.features.dvr.RecordingAddEditActivity;
import org.tvheadend.tvhclient.ui.features.search.SearchActivity;
import org.tvheadend.tvhclient.ui.features.search.SearchRequestInterface;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

public class ProgramGuideFragment extends BaseFragment implements EpgScrollInterface, RecyclerViewClickCallback, ChannelDisplayOptionListener, Filter.FilterListener, ViewPager.OnPageChangeListener, SearchRequestInterface {

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
    private List<ChannelTag> channelTags;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.epg_main_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        channelListRecyclerView.setAdapter(null);
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
        //noinspection ConstantConditions
        hoursToShow = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", getResources().getString(R.string.pref_default_hours_of_epg_data_per_screen)));
        // The defined value should not be zero due to checking the value
        // in the settings. Check it anyway to prevent a divide by zero.
        if (hoursToShow == 0) {
            hoursToShow++;
        }
        //noinspection ConstantConditions
        daysToShow = Integer.parseInt(sharedPreferences.getString("days_of_epg_data", getResources().getString(R.string.pref_default_days_of_epg_data)));
        fragmentCount = (daysToShow * (24 / hoursToShow));

        calculateViewPagerFragmentStartAndEndTimes();

        channelListRecyclerViewAdapter = new EpgChannelListRecyclerViewAdapter(this);
        channelListRecyclerViewLayoutManager = new LinearLayoutManager(activity);
        channelListRecyclerView.setLayoutManager(channelListRecyclerViewLayoutManager);
        channelListRecyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        channelListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        channelListRecyclerView.setAdapter(channelListRecyclerViewAdapter);
        channelListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != SCROLL_STATE_IDLE) {
                    enableScrolling = true;
                } else if (enableScrolling) {
                    enableScrolling = false;
                    ProgramGuideFragment.this.onScrollStateChanged();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
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

        Timber.d("Observing channel tags");
        viewModel.getChannelTags().observe(getViewLifecycleOwner(), tags -> {
            if (tags != null) {
                Timber.d("View model returned " + tags.size() + " channel tags");
                channelTags = tags;
            }
        });

        Timber.d("Observing epg channels");
        viewModel.getEpgChannels().observe(getViewLifecycleOwner(), channels -> {

            progressBar.setVisibility(View.GONE);
            channelListRecyclerView.setVisibility(View.VISIBLE);
            programViewPager.setVisibility(View.VISIBLE);

            if (channels != null) {
                Timber.d("View model returned " + channels.size() + " epg channels");
                channelListRecyclerViewAdapter.addItems(channels);
            }
            // Show either all channels or the name of the selected
            // channel tag and the channel count in the toolbar
            String toolbarTitle = viewModel.getSelectedChannelTagName(getContext());
            toolbarInterface.setTitle(toolbarTitle);
            toolbarInterface.setSubtitle(activity.getResources().getQuantityString(R.plurals.items,
                    channelListRecyclerViewAdapter.getItemCount(), channelListRecyclerViewAdapter.getItemCount()));
        });

        // Observe all recordings here in case a recording shall be edited right after it was added.
        // This needs to be done in this fragment because the popup menu handling is also done here.
        Timber.d("Observing recordings");
        viewModel.getAllRecordings().observe(getViewLifecycleOwner(), recordings -> {
            if (recordings != null) {
                Timber.d("View model returned " + recordings.size() + " recordings");
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
        // When the user returns from the settings only the onResume method is called, not the
        // onActivityCreated, so we need to check if any values that affect the representation
        // of the channel list have changed.
        //noinspection ConstantConditions
        viewModel.setChannelSortOrder(Integer.valueOf(sharedPreferences.getString("channel_sort_order", getResources().getString(R.string.pref_default_channel_sort_order))));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.channel_list_options_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final boolean showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", getResources().getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled));
        final boolean showChannelTagMenu = sharedPreferences.getBoolean("channel_tag_menu_enabled", getResources().getBoolean(R.bool.pref_default_channel_tag_menu_enabled));

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
                return ChannelTagSelectionDialog.showDialog(activity, channelTags, appRepository.getChannelData().getItems().size(), this);

            case R.id.menu_timeframe:
                return menuUtils.handleMenuTimeSelection(selectedTimeOffset, hoursToShow, (hoursToShow * daysToShow), this);

            case R.id.menu_genre_color_info_channels:
                return GenreColorDialog.showDialog(activity);

            case R.id.menu_sort_order:
                return menuUtils.handleMenuChannelSortOrderSelection(this);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTimeSelected(int which) {
        selectedTimeOffset = which;
        programViewPager.setCurrentItem(which);

        // TODO check if this is required
        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        long timeInMillis = System.currentTimeMillis();
        timeInMillis += (1000 * 60 * 60 * which * hoursToShow);
        viewModel.setSelectedTime(timeInMillis);
    }

    @Override
    public void onChannelTagIdsSelected(Set<Integer> ids) {
        channelListRecyclerView.setVisibility(View.GONE);
        programViewPager.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        viewModel.setSelectedChannelTagIds(ids);
    }

    @Override
    public void onChannelSortOrderSelected(int id) {
        viewModel.setChannelSortOrder(id);
    }

    @Override
    public void onClick(View view, int position) {
        if (view.getId() == R.id.icon || view.getId() == R.id.icon_text) {
            if (isNetworkAvailable) {
                EpgChannel channel = channelListRecyclerViewAdapter.getItem(position);
                menuUtils.handleMenuPlayChannelIcon(channel.getId());
            }
        }
    }

    @Override
    public boolean onLongClick(View view, int position) {
        // NOP
        return true;
    }

    void showPopupMenu(View view, EpgProgram program) {
        if (activity == null || program == null) {
            return;
        }

        Recording recording = appRepository.getRecordingData().getItemByEventId(program.getEventId());

        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.getMenuInflater().inflate(R.menu.program_popup_and_toolbar_menu, popupMenu.getMenu());
        popupMenu.getMenuInflater().inflate(R.menu.external_search_options_menu, popupMenu.getMenu());

        PopupMenuUtil.prepareMenu(activity, popupMenu.getMenu(), program, program.getRecording(), isNetworkAvailable, htspVersion, isUnlocked);
        PopupMenuUtil.prepareSearchMenu(popupMenu.getMenu(), program.getTitle(), isNetworkAvailable);

        popupMenu.setOnMenuItemClickListener(item -> {
            if (SearchMenuUtils.onMenuSelected(activity, item.getItemId(), program.getTitle())) {
                return true;
            }
            switch (item.getItemId()) {
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
        String toolbarTitle = viewModel.getSelectedChannelTagName(getContext());
        toolbarInterface.setTitle(toolbarTitle);
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

    @Override
    public String getQueryHint() {
        return getString(R.string.search_program_guide);
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
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
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
