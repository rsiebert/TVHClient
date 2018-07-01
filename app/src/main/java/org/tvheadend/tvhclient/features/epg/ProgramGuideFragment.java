package org.tvheadend.tvhclient.features.epg;

import android.arch.lifecycle.ViewModelProviders;
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
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTimeSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecyclerViewClickCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO Align with channel list fragment later

public class ProgramGuideFragment extends BaseFragment implements RecyclerViewClickCallback, ChannelTimeSelectionCallback, ChannelTagSelectionCallback, Filter.FilterListener {

    @BindView(R.id.channel_list_recycler_view)
    RecyclerView channelListRecyclerView;
    @BindView(R.id.program_list_viewpager)
    ViewPager programViewPager;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private Unbinder unbinder;
    private int selectedTimeOffset;
    private int selectedListPosition;
    private String searchQuery;
    private EpgViewModel viewModel;
    private EpgChannelListRecyclerViewAdapter channelListRecyclerViewAdapter;

    private final List<Long> startTimes = new ArrayList<>();
    private final List<Long> endTimes = new ArrayList<>();
    private int hoursToShow;
    private int fragmentCount;

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

        if (savedInstanceState != null) {
            selectedListPosition = savedInstanceState.getInt("listPosition", 0);
            selectedTimeOffset = savedInstanceState.getInt("timeOffset");
        } else {
            selectedListPosition = 0;
            selectedTimeOffset = 0;
        }

        // Calculates the available display width of one minute in pixels. This depends
        // how wide the screen is and how many hours shall be shown in one screen.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int displayWidth = displaymetrics.widthPixels;

        calculateViewPagerFragmentCount();
        calculateViewPagerFragmentStartAndEndTimes();

        // Calculate the ratio how many minutes a pixel represents on the screen.
        int viewPagerWidth = programViewPager.getLayoutParams().width;
        float pixelsPerMinute = ((float) (displayWidth - viewPagerWidth) / (60.0f * (float) hoursToShow));

        channelListRecyclerViewAdapter = new EpgChannelListRecyclerViewAdapter(activity, this);
        channelListRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        channelListRecyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        channelListRecyclerView.setItemAnimator(new DefaultItemAnimator());
        channelListRecyclerView.setAdapter(channelListRecyclerViewAdapter);

        EpgViewPagerAdapter viewPagerAdapter = new EpgViewPagerAdapter(activity.getSupportFragmentManager(), startTimes, endTimes, pixelsPerMinute, displayWidth, fragmentCount);
        programViewPager.setAdapter(viewPagerAdapter);
        programViewPager.setOffscreenPageLimit(1);

        viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);
        viewModel.getChannels().observe(this, channels -> {

            progressBar.setVisibility(View.GONE);
            channelListRecyclerView.setVisibility(View.VISIBLE);
            programViewPager.setVisibility(View.VISIBLE);

            channelListRecyclerViewAdapter.addItems(channels);
            if (!TextUtils.isEmpty(searchQuery)) {
                channelListRecyclerViewAdapter.getFilter().filter(searchQuery, this);
            }

            // Show either all channels or the name of the selected
            // channel tag and the channel count in the toolbar
            ChannelTag channelTag = viewModel.getChannelTag();
            toolbarInterface.setTitle((channelTag == null) ? getString(R.string.all_channels) : channelTag.getTagName());
            toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.items,
                    channelListRecyclerViewAdapter.getItemCount(), channelListRecyclerViewAdapter.getItemCount()));
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", selectedListPosition);
        outState.putInt("timeOffset", selectedTimeOffset);
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
        menu.findItem(R.id.menu_search).setVisible(false);

        // Prevent the channel tag menu item from going into the overlay menu
        if (showChannelTagMenu) {
            menu.findItem(R.id.menu_tags).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                menuUtils.handleMenuChannelTagsSelection(viewModel.getChannelTagId(), this);
                return true;
            case R.id.menu_timeframe:
                menuUtils.handleMenuTimeSelection(selectedTimeOffset, this);
                return true;
            case R.id.menu_genre_color_info_channels:
                menuUtils.handleMenuGenreColorSelection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTimeSelected(int which) {
        selectedTimeOffset = which;

        channelListRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Add the selected list index as extra hours to the current time.
        // If the first index was selected then use the current time.
        Calendar c = Calendar.getInstance();
        if (which > 0) {
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) + which);
            viewModel.setSelectedTime(c.getTimeInMillis());
        } else {
            viewModel.setSelectedTime(new Date().getTime());
        }
    }

    @Override
    public void onChannelTagIdSelected(int id) {
        viewModel.setChannelTagId(id);
        channelListRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view, int position) {
        //TODO show program details
    }

    @Override
    public void onLongClick(View view, int position) {
        //TODO showPopupMenu(view);
    }

    @Override
    public void onFilterComplete(int i) {
        // Show either all channels or the name of the selected
        // channel tag and the channel count in the toolbar
        ChannelTag channelTag = viewModel.getChannelTag();
        toolbarInterface.setTitle((channelTag == null) ? getString(R.string.all_channels) : channelTag.getTagName());
        toolbarInterface.setSubtitle(getResources().getQuantityString(R.plurals.results,
                channelListRecyclerViewAdapter.getItemCount(), channelListRecyclerViewAdapter.getItemCount()));
    }

    /**
     * Calculates the number of fragments in the view pager. This depends on how
     * many days shall be shown of the program guide and how many hours shall be
     * visible per fragment.
     */
    private void calculateViewPagerFragmentCount() {
        int daysToShow = Integer.parseInt(sharedPreferences.getString("days_of_epg_data", "7"));
        hoursToShow = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", "4"));
        fragmentCount = (daysToShow * (24 / hoursToShow));
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

    private static class EpgViewPagerAdapter extends FragmentStatePagerAdapter {

        private final List<Long> startTimes;
        private final List<Long> endTimes;
        private float pixelsPerMinute;
        private int displayWidth;
        private int fragmentCount;

        EpgViewPagerAdapter(FragmentManager fragmentManager, List<Long> startTimes, List<Long> endTimes, float pixelsPerMinute, int displayWidth, int fragmentCount) {
            super(fragmentManager);
            this.startTimes = startTimes;
            this.endTimes = endTimes;
            this.pixelsPerMinute = pixelsPerMinute;
            this.displayWidth = displayWidth;
            this.fragmentCount = fragmentCount;
        }

        @Override
        public Fragment getItem(int position) {
            boolean showTimeIndication = (position == 0);
            return EpgViewPagerFragment.newInstance(
                    startTimes.get(position),
                    endTimes.get(position),
                    pixelsPerMinute,
                    displayWidth,
                    showTimeIndication);

        }

        @Override
        public int getCount() {
            return fragmentCount;
        }
    }
}
