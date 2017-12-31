package org.tvheadend.tvhclient.ui.epg;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.utils.callbacks.ChannelTagSelectionCallback;
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.data.model.ProgramGuideTimeDialogItem;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProgramGuidePagerFragment extends Fragment implements FragmentControlInterface, ChannelTagSelectionCallback {

    private Activity activity;
    private ViewPager viewPager = null;
    private ProgramGuidePagerAdapter adapter = null;
    private MenuUtils menuUtils;

    // This is the width in pixels from the icon in the program_guide_list.xml
    // We need to subtract this value from the window width to get the real
    // usable width. The same values is also used in the
    // ProgramGuideListFragment class.
    private final static int LAYOUT_ICON_OFFSET = 72;

    // The ratio how many minutes a pixel represents on the screen.
    private static float pixelsPerMinute;
    private static int displayWidth;

    // The time frame (start and end times) that shall be shown in a single fragment.  
    private static final List<Long> startTimes = new ArrayList<>();
    private static final List<Long> endTimes = new ArrayList<>();
    private static int hoursToShow;
    private static int fragmentCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.program_guide_pager, container, false);
        viewPager = v.findViewById(R.id.pager);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();

        menuUtils = new MenuUtils(getActivity());

        // Calculate the max number of fragments in the view pager 
        calcFragmentCount();
        calcProgramGuideTimeslots();

        // Calculates the available display width of one minute in pixels. This
        // depends how wide the screen is and how many hours shall be shown in
        // one screen.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        displayWidth = displaymetrics.widthPixels;
        pixelsPerMinute = ((float) (displayWidth - LAYOUT_ICON_OFFSET) / (60.0f * (float) hoursToShow));

        // Show the list of channels on the left side
        Bundle bundle = new Bundle();
        Fragment f = Fragment.instantiate(getActivity(), ProgramGuideChannelListFragment.class.getName());
        f.setArguments(bundle);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.program_guide_channel_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();

        adapter = new ProgramGuidePagerAdapter(getChildFragmentManager(), activity, fragmentCount);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        adapter.notifyDataSetChanged();

        setHasOptionsMenu(true);
    }

    @SuppressLint({ "InlinedApi", "NewApi" })
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsChannelsPref", false);
        (menu.findItem(R.id.menu_genre_color_info_channels)).setVisible(showGenreColors);

        (menu.findItem(R.id.menu_timeframe)).setVisible(TVHClientApplication.getInstance().isUnlocked());

        // Prevent the channel tag menu item from going into the overlay menu
        if (prefs.getBoolean("visibleMenuIconTagsPref", true)) {
            menu.findItem(R.id.menu_tags).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        // Prevent the time frame menu item from going into the overlay menu
        if (prefs.getBoolean("visibleMenuIconTagsPref", true)) {
            menu.findItem(R.id.menu_timeframe).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.epg_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tags:
                ChannelTag tag = Utils.getChannelTag(activity);
                menuUtils.handleMenuTagsSelection((tag != null ? tag.tagId : -1), this);
                return true;

            case R.id.menu_timeframe:
                showProgramGuideTimeDialog();
                return true;

            case R.id.menu_genre_color_info_channels:
                menuUtils.handleMenuGenreColorSelection();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Prepares a dialog that shows the available dates and times the user can
     * choose from. In here the data for the adapter is created and the dialog
     * prepared which can be shown later.
     */
    private void showProgramGuideTimeDialog() {
        // Fill the list for the adapter
        List<ProgramGuideTimeDialogItem> times = new ArrayList<>();
        for (int i = 0; i < fragmentCount; ++i) {
            ProgramGuideTimeDialogItem item = new ProgramGuideTimeDialogItem();
            item.start = startTimes.get(i);
            item.end = endTimes.get(i);
            times.add(item);
        }
        // The dialog that allows the user to select a certain time frame
        final ProgramGuideTimeDialogAdapter timeAdapter = new ProgramGuideTimeDialogAdapter(times);
        final MaterialDialog programGuideTimeDialog = new MaterialDialog.Builder(activity)
                .title(R.string.tags)
                .adapter(timeAdapter, null)
                .build();

        // Set the callback to handle clicks. This needs to be done after the
        // dialog creation so that the inner method has access to the dialog variable
        timeAdapter.setCallback(new ProgramGuideTimeDialogAdapter.Callback() {
            @Override
            public void onItemClicked(int which) {
                viewPager.setCurrentItem(which);
                programGuideTimeDialog.dismiss();
            }
        });
        programGuideTimeDialog.show();
    }

    /**
     * Calculates the number of fragments in the view pager. This depends on how
     * many days shall be shown of the program guide and how many hours shall be
     * visible per fragment.
     */
    private void calcFragmentCount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        int daysToShow = Integer.parseInt(prefs.getString("epgMaxDays", Constants.EPG_DEFAULT_MAX_DAYS));
        hoursToShow = Integer.parseInt(prefs.getString("epgHoursVisible", Constants.EPG_DEFAULT_HOURS_VISIBLE));
        fragmentCount = (daysToShow * (24 / hoursToShow));
    }

    /**
     * Calculates the day, start and end hours that are valid for the fragment
     * at the given position. This will be saved in the lists to avoid
     * calculating this for every fragment again and so we can update the data
     * when the settings have changed.
     */
    private void calcProgramGuideTimeslots() {
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

    /**
     * Pager adapter that is responsible for creating the fragments and passing
     * the required information to them when the user swipes the screen.
     * 
     * @author rsiebert
     * 
     */
    private static class ProgramGuidePagerAdapter extends FragmentPagerAdapter {
        private int count = 0;

        public ProgramGuidePagerAdapter(FragmentManager fm, Activity activity, int fragmentCount) {
            super(fm);
            this.count = fragmentCount;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new ProgramGuideListFragment();
            Bundle bundle = new Bundle();
            bundle.putLong(Constants.BUNDLE_EPG_START_TIME, startTimes.get(position));
            bundle.putLong(Constants.BUNDLE_EPG_END_TIME, endTimes.get(position));
            // Used to only show the vertical current time indication in the first fragment
            bundle.putInt(Constants.BUNDLE_EPG_INDEX, position);
            // Required to know how many pixels of the display are remaining
            bundle.putInt(Constants.BUNDLE_EPG_DISPLAY_WIDTH, displayWidth);
            // Required so that the correct width of each program guide entry
            // can be calculated. Also used to determine the exact position of
            // the vertical current time indication
            bundle.putFloat(Constants.BUNDLE_EPG_PIXELS_PER_MINUTE, pixelsPerMinute);
            fragment.setArguments(bundle);
            return fragment;
        }
        
        @Override
        public int getCount() {
            return count;
        }
    }

    @Override
    public void reloadData() {
        // The main activity has only access to this fragment, but not the child
        // fragments which this fragment is controlling. Forward the reload
        // command to all fragments in the pager and to the channel list
        // fragment.
        for (int i = 0; i < fragmentCount; ++i) {
            Fragment f = getChildFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + adapter.getItemId(i));
            if (f instanceof ProgramGuideListFragment) {
                ((FragmentControlInterface) f).reloadData();
            }
        }
        final Fragment cf = getChildFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf instanceof ProgramGuideChannelListFragment) {
            ((FragmentControlInterface) cf).reloadData();
        }
    }

    @Override
    public void setSelection(final int position, final int offset) {
        // The main activity has only access to this fragment, but not the child
        // fragments which this fragment is controlling. Forward the scrolling
        // positions and offsets to all fragments in the pager and to the
        // channel list fragment.
        for (int i = 0; i < fragmentCount; ++i) {
            Fragment f = getChildFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + adapter.getItemId(i));
            if (f instanceof FragmentControlInterface) {
                ((FragmentControlInterface) f).setSelection(position, offset);
            }
        }
        final Fragment cf = getChildFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf instanceof FragmentControlInterface) {
            ((FragmentControlInterface) cf).setSelection(position, offset);
        }
    }

    @Override
    public void onChannelTagIdSelected(int which) {
        Utils.setChannelTagId(activity, which);

        // Inform the channel list fragment to clear all data from its
        // channel list and show only the channels with the selected tag
        final Fragment cf = getActivity().getSupportFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf instanceof ProgramGuideChannelListFragment) {
            ((FragmentControlInterface) cf).reloadData();
        }
        // Additionally inform the program guide fragment to clear all data
        // from its list and show only the programs of the channels that are
        // part of the selected tag
        final Fragment pgf = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (pgf instanceof ProgramGuidePagerFragment) {
            ((FragmentControlInterface) pgf).reloadData();
        }
    }
}
