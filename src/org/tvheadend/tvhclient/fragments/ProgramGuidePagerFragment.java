package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.ProgramGuideTimeDialogAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.model.ProgramGuideTimeDialogItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class ProgramGuidePagerFragment extends Fragment implements FragmentControlInterface {

    @SuppressWarnings("unused")
    private final static String TAG = ProgramGuidePagerFragment.class.getSimpleName();

    private Activity activity;
    private static ViewPager viewPager = null;
    private ProgramGuidePagerAdapter adapter = null;

    // The dialog that allows the user to select a certain time frame
    private AlertDialog programGuideTimeDialog;

    // This is the width in pixels from the icon in the program_guide_list.xml
    // We need to subtract this value from the window width to get the real
    // usable width. The same values is also used in the
    // ProgramGuideListFragment class.
    private final static int LAYOUT_ICON_OFFSET = 66;

    // The ratio how many minutes a pixel represents on the screen.
    private static float pixelsPerMinute;
    private static int displayWidth;

    // The time frame (start and end times) that shall be shown in a single fragment.  
    private static List<Long> startTimes = new ArrayList<Long>();
    private static List<Long> endTimes = new ArrayList<Long>();
    private static int daysToShow;
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
        viewPager = (ViewPager) v.findViewById(R.id.pager);
        return v;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Calculate the max number of fragments in the view pager 
        calcFragmentCount();
        calcProgramGuideTimeslots();
        createProgramGuideTimeDialog();

        // Calculates the available display width of one minute in pixels. This
        // depends how wide the screen is and how many hours shall be shown in
        // one screen.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        displayWidth = displaymetrics.widthPixels;
        pixelsPerMinute = ((float) (displayWidth - LAYOUT_ICON_OFFSET) / (60.0f * (float) hoursToShow));

        // Show the list of channels on the left side
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.BUNDLE_SHOWS_ONLY_CHANNELS, true);
        Fragment f = Fragment.instantiate(getActivity(), ChannelListFragment.class.getName());
        f.setArguments(bundle);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.program_guide_channel_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        adapter = new ProgramGuidePagerAdapter(getChildFragmentManager(), activity, fragmentCount);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        adapter.notifyDataSetChanged();

        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu if no genre colors shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsGuidePref", false);
        (menu.findItem(R.id.menu_genre_color_info_epg)).setVisible(showGenreColors);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.epg_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_timeframe:
            programGuideTimeDialog.show();
            return true;
            
        case R.id.menu_genre_color_info_epg:
            Utils.showGenreColorDialog(activity);
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
    private void createProgramGuideTimeDialog() {
        // Fill the list for the adapter
        List<ProgramGuideTimeDialogItem> times = new ArrayList<ProgramGuideTimeDialogItem>(); 
        for (int i = 0; i < fragmentCount; ++i) {
            ProgramGuideTimeDialogItem item = new ProgramGuideTimeDialogItem();
            item.start = startTimes.get(i);
            item.end = endTimes.get(i);
            times.add(item);
        }

        // Create the dialog and set the adapter
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.select_timeframe);
        builder.setAdapter(
                new ProgramGuideTimeDialogAdapter(activity, times),
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int pos) {
                        viewPager.setCurrentItem(pos);
                    }
                });
        programGuideTimeDialog = builder.create();
    }

    /**
     * Calculates the number of fragments in the view pager. This depends on how
     * many days shall be shown of the program guide and how many hours shall be
     * visible per fragment.
     */
    private void calcFragmentCount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        daysToShow = Integer.parseInt(prefs.getString("epgMaxDays", Constants.EPG_DEFAULT_MAX_DAYS));
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
            if (f instanceof ProgramGuideListFragment && f instanceof FragmentControlInterface) {
                ((FragmentControlInterface) f).reloadData();
            }
        }
        final Fragment cf = getChildFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf instanceof ChannelListFragment && cf instanceof FragmentControlInterface) {
            ((FragmentControlInterface) cf).reloadData();
        }
    }

    @Override
    public void setInitialSelection(final int position) {
        // NOP
    }

    @Override
    public void setSelection(final int position, final int offset) {
        // The main activity has only access to this fragment, but not the child
        // fragments which this fragment is controlling. Forward the scrolling
        // positions and offsets to all fragments in the pager and to the
        // channel list fragment.
        for (int i = 0; i < fragmentCount; ++i) {
            Fragment f = getChildFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + adapter.getItemId(i));
            if (f instanceof ProgramGuideListFragment && f instanceof FragmentControlInterface) {
                ((FragmentControlInterface) f).setSelection(position, offset);
            }
        }
        final Fragment cf = getChildFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf instanceof ChannelListFragment && cf instanceof FragmentControlInterface) {
            ((FragmentControlInterface) cf).setSelection(position, offset);
        }
    }

    @Override
    public Object getSelectedItem() {
        return null;
    }

    @Override
    public int getItemCount() {
        int count = 0;
        final Fragment cf = getChildFragmentManager().findFragmentById(R.id.program_guide_channel_fragment);
        if (cf instanceof ChannelListFragment && cf instanceof FragmentControlInterface) {
            count = ((FragmentControlInterface) cf).getItemCount();
        }
        return count;
    }
}
