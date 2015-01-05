package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.ProgramGuideTimeDialogAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.ProgramGuideTimeDialogItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ProgramGuidePagerFragment extends Fragment implements HTSListener, FragmentControlInterface {

    private final static String TAG = ProgramGuidePagerFragment.class.getSimpleName();

    private Activity activity;
    private FragmentStatusInterface fragmentStatusInterface;
    private static ViewPager viewPager = null;
    private ProgramGuidePagerAdapter adapter = null;

    // The dialog that allows the user to select a certain time frame
    private AlertDialog programGuideTimeDialog;

    ArrayAdapter<ChannelTag> tagAdapter;
    private AlertDialog tagDialog;
    private Toolbar toolbar;
    
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
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        return v;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        if (!app.isLoading()) {
            populateTagList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onDetach() {
        fragmentStatusInterface = null;
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        // Calculate the max number of fragments in the view pager 
        calcFragmentCount();
        calcProgramGuideTimeslots();
        createProgramGuideTimeDialog();

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

        // Create the dialog with the available channel tags
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_tags);
        tagAdapter = new ArrayAdapter<ChannelTag>(activity,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<ChannelTag>());
        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                Utils.setChannelTagId(pos);
                if (fragmentStatusInterface != null) {
                    fragmentStatusInterface.channelTagChanged(TAG);
                }
            }
        });
        tagDialog = builder.create();

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onToolbarItemSelected(item);
            }
        });

        // Inflate a menu to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.epg_menu);
        onPrepareToolbarMenu(toolbar.getMenu());
    }

    /**
     * 
     * @param menu
     */
    private void onPrepareToolbarMenu(Menu menu) {
        // Hide the genre color menu if no genre colors shall be shown
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final boolean showGenreColors = prefs.getBoolean("showGenreColorsGuidePref", false);
        (menu.findItem(R.id.menu_genre_color_info_epg)).setVisible(showGenreColors);
    }

    /**
     * 
     * @param item
     * @return
     */
    private boolean onToolbarItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            activity.onSearchRequested();
            return true;

        case R.id.menu_timeframe:
            programGuideTimeDialog.show();
            return true;

        case R.id.menu_tags:
            tagDialog.show();
            return true;

        case R.id.menu_genre_color_info_channels:
            Utils.showGenreColorDialog(activity);
            return true;

        case R.id.menu_refresh:
            fragmentStatusInterface.reloadData(TAG);
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

        public ProgramGuidePagerAdapter(FragmentManager fm, Context context, int fragmentCount) {
            super(fm);
            count = fragmentCount;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new ProgramGuideListFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.BUNDLE_EPG_INDEX, position);
            bundle.putInt(Constants.BUNDLE_EPG_HOURS_TO_SHOW, hoursToShow);
            bundle.putLong(Constants.BUNDLE_EPG_START_TIME, startTimes.get(position));
            bundle.putLong(Constants.BUNDLE_EPG_END_TIME, endTimes.get(position));
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

    /**
     * Fills the tag adapter with the available channel tags.
     */
    public void populateTagList() {
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        ChannelTag currentTag = Utils.getChannelTag(app);
        int channelCount = 0;
        for (Channel ch : app.getChannels()) {
            if (currentTag == null || ch.hasTag(currentTag.id)) {
                channelCount++;
            }
        }
        
        tagAdapter.clear();
        for (ChannelTag t : app.getChannelTags()) {
            tagAdapter.add(t);
        }
        // Inform the activity to show the currently visible number of the
        // channels that are in the selected channel tag and that the channel
        // list has been filled with data.
        if (toolbar != null) {
            toolbar.setTitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.name);
            toolbar.setSubtitle(channelCount + " " + getString(R.string.items));
            // If activated show the the channel tag icon
            if (Utils.showChannelIcons(activity) && Utils.showChannelTagIcon(activity)
                    && currentTag != null 
                    && currentTag.id != 0) {
                toolbar.setNavigationIcon(new BitmapDrawable(getResources(), currentTag.iconBitmap));
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_launcher);
            }
        }
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    if (loading) {
                        if (toolbar != null) {
                            toolbar.setSubtitle(R.string.loading);
                        }
                    } else {
                        populateTagList();
                    }
                }
            });
        }
    }

    /**
     * 
     * @param title
     */
    public void setToolbarSubtitle(String title) {
        if (toolbar != null) {
            toolbar.setSubtitle(title);
        }
    }
}
