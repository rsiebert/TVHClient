package org.tvheadend.tvhclient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.interfaces.ProgramLoadingInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ProgramGuideTabsActivity extends ActionBarActivity implements HTSListener, ProgramLoadingInterface {

    @SuppressWarnings("unused")
    private final static String TAG = ProgramGuideTabsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private ProgramGuidePagerAdapter adapter = null;
    public List<Channel> channelLoadingList = new ArrayList<Channel>();
    // Indicates that a loading is in progress, the next channel can only be loaded when this is false 
    private boolean isLoadingChannels = false;
    // The dialog that allows the user to select a certain time frame
    private AlertDialog programGuideTimeDialog;
    // The time frame (start and end times) that shall be shown in a single fragment.  
    private static List<Long> startTimes = new ArrayList<Long>();
    private static List<Long> endTimes = new ArrayList<Long>();
    private static int daysToShow;
    private static int hoursToShow;
    private static int fragmentCount;
    private static ViewPager viewPager = null;
    private static int scrollingSelectionIndex = 0;
    // Amount of programs of a channel that shall be loaded from the server 
    private static int programsToLoad = 20;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.program_guide_pager);

        // Calculate the max number of fragments in the view pager 
        calcFragmentCount();
        calcProgramGuideTimeslots();
        createProgramGuideTimeDialog();

        // Add the channel list fragment to the list view on the left side
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment channelFrag = getSupportFragmentManager().findFragmentByTag("channel_icon_list");
        if (channelFrag == null) {
            
            // Pass the correct layout information to the fragment. Here we only
            // want to show the channel icons, nothing more.
            Bundle args = new Bundle();
            args.putInt("viewLayout", R.layout.program_guide_pager);
            args.putInt("adapterLayout", R.layout.program_guide_channel_item);
            args.putBoolean("disableMenus", true);
            Fragment fragment = Fragment.instantiate(this, ChannelListFragment.class.getName());
            fragment.setArguments(args);
            ft.add(android.R.id.content, fragment, "channel_icon_list");
        } else {
            ft.attach(channelFrag);
        }
        ft.commit();

        // setup action bar for tabs
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.program_guide);
        actionBar.setSubtitle(R.string.loading);
        
        adapter = new ProgramGuidePagerAdapter(getSupportFragmentManager(), this);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        adapter.notifyDataSetChanged();

        // Restore the previously selected tab. This is usually required when
        // the user has rotated the screen.
        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt("selected_program_guide_tab_index", 0);
            viewPager.setCurrentItem(index);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int index = viewPager.getCurrentItem();
        outState.putInt("selected_program_guide_tab_index", index);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.addListener(this);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListener(this);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu == null) {
            return true;
        }
        // Disable the refresh menu if no connection is 
        // available or the loading process is already active
        MenuItem item = menu.findItem(R.id.menu_refresh);
        if (item != null) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            if (app != null && DatabaseHelper.getInstance() != null) {
                item.setVisible(DatabaseHelper.getInstance().getSelectedConnection() != null && !app.isLoading());
            }
        }
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;

        case R.id.menu_timeframe:
            // Show the dialog to select the time
            programGuideTimeDialog.show();
            return true;
            
        case R.id.menu_settings:
            // Start the settings activity 
            intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, Utils.getResultCode(R.id.menu_settings));
            return true;

        case R.id.menu_refresh:
            Utils.connect(this, true);
            return true;

        case R.id.menu_connections:
            // Show the manage connections activity where 
            // the user can choose a connection
            intent = new Intent(this, SettingsManageConnectionsActivity.class);
            startActivityForResult(intent, Utils.getResultCode(R.id.menu_connections));
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_timeframe);
        builder.setAdapter(new ProgramGuideTimeDialogAdapter(this, times), 
                           new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                viewPager.setCurrentItem(pos);
            }
        });
        programGuideTimeDialog = builder.create();
    }

    /**
     * Pager adapter that is responsible for handling the fragment that will be shown when the user swipes the screen.
     * @author rsiebert
     *
     */
    private static class ProgramGuidePagerAdapter extends FragmentPagerAdapter {
        public ProgramGuidePagerAdapter(FragmentManager fm, Context context) {
            super(fm);
        }
        
        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new ProgramGuideListFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("tabIndex", position);
            bundle.putInt("hoursToShow", hoursToShow);
            bundle.putLong("startTime", startTimes.get(position));
            bundle.putLong("endTime", endTimes.get(position));
            fragment.setArguments(bundle);
            return fragment;
        }
        
        @Override
        public int getCount() {
            return fragmentCount;
        }
    }

    /**
     * When the user has scrolled within one fragment, the other available
     * fragments in the view pager must be scrolled to the same position. Calls
     * the scrollListViewTo method on every available fragment that the view 
     * pager contains.
     */
    
    public void onScrollChanged(int index) {
        scrollingSelectionIndex = index;
        for (int i = 0; i < fragmentCount; ++i) {
            ProgramGuideListFragment f = (ProgramGuideListFragment) getSupportFragmentManager().findFragmentByTag(
                    "android:switcher:" + viewPager.getId() + ":" + adapter.getItemId(i));
            if (f != null) {
                f.scrollListViewTo(scrollingSelectionIndex);
            }
        }
        
        ChannelListFragment f = (ChannelListFragment) getSupportFragmentManager().findFragmentByTag("channel_icon_list");
        if (f != null) {
            f.scrollListViewTo(scrollingSelectionIndex);
        }
    }

    
    public void onScrollPositionChanged(int index, int pos) {
        ChannelListFragment f = (ChannelListFragment) getSupportFragmentManager().findFragmentByTag("channel_icon_list");
        if (f != null) {
            f.scrollListViewToPosition(index, pos);
        }
    }

    /**
     * Reloads all data if the connection details have changed, a new one was
     * created or if the number of hours per time slot have changed.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Utils.getResultCode(R.id.menu_connections)) {
            if (resultCode == RESULT_OK) {
                Utils.connect(this, data.getBooleanExtra("reconnect", false));
            }
        } else if (requestCode == Utils.getResultCode(R.id.menu_settings)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            programsToLoad = Integer.parseInt(prefs.getString("programsToLoad", "10"));
            // Only recalculate the times when they have changed
            if (daysToShow != Integer.parseInt(prefs.getString("epgMaxDays", "7"))
                    || hoursToShow != Integer.parseInt(prefs.getString("epgHoursVisible", "4"))) {

                // Restart this activity
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        }
    }

    /**
     * Calculates the number of fragments in the view pager.
     * This depends on how many days shall be shown of the program 
     * guide and how many hours shall be visible per fragment.
     */
    private void calcFragmentCount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        daysToShow = Integer.parseInt(prefs.getString("epgMaxDays", "7"));
        hoursToShow = Integer.parseInt(prefs.getString("epgHoursVisible", "4"));
        fragmentCount = (daysToShow * (24 / hoursToShow));
    }
    
    /**
     * Calculates the day, start and end hours that are valid for the fragment
     * at the given position. This will be saved in the lists to avoid 
     * calculating this for every fragment again and so we can update the
     * data when the settings have changed.
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

    public void setActionBarTitle(final String title) {
        actionBar.setTitle(title);
    }

    public void setActionBarSubtitle(final String subtitle) {
        actionBar.setSubtitle(subtitle);
    }

    /**
     * Returns the index of of the first list item that is visible. The method
     * used is listView.getFirstVisiblePosition(). This can be used to jump to
     * certain positions in the list. No smooth scrolling is possible with this.
     * 
     * @return
     */
    public int getScrollingSelectionIndex() {
        return scrollingSelectionIndex;
    }

    /**
     * When a program still fits into the current time slot but there is no more
     * program guide data available then this method is called from the
     * ProgramGuideItemView class. A channel can't be added twice to the loading
     * list to avoid loading the same or too many data.
     */
    public void loadMorePrograms(int fragmentId, Channel channel) {
        if (channel == null || channelLoadingList.contains(channel)) {
            return;
        }
        channelLoadingList.add(channel);
        startLoadingPrograms();
    }

    /**
     * Calls the method to actually load the program guide data from the first
     * channel in the list.
     */
    private void startLoadingPrograms() {
        if (!channelLoadingList.isEmpty() && !isLoadingChannels ) {
        	isLoadingChannels = true;
            actionBar.setSubtitle(getString(R.string.loading));
            Utils.loadMorePrograms(this, programsToLoad, channelLoadingList.get(0));
        }
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(final String action, final Object obj) {
        if (action.equals(TVHClientApplication.ACTION_CHANNEL_UPDATE)) {
            runOnUiThread(new Runnable() {
                public void run() {
                	final Channel channel = (Channel) obj;
                    channelLoadingList.remove(channel);
                    isLoadingChannels = false;
                    startLoadingPrograms();
                }
            });
        } 
    }
    
    /**
     * The class the is used by the list in the ProgramGuideTimeDialogAdapter
     * class. It contains the values that can be shown in the dialog
     * 
     * @author rsiebert
     * 
     */
    private class ProgramGuideTimeDialogItem {
        public long start;
        public long end;
    }
    
    /**
     * A private custom adapter that contains the list of
     * ProgramGuideTimeDialogItem. This is pretty much only a list of dates and
     * times that can be shown from a dialog. This is used to improve the look
     * of the dialog. A simple adapter with two line does not provide the amount
     * of styling flexibility.
     * 
     * @author rsiebert
     * 
     */
    private class ProgramGuideTimeDialogAdapter extends ArrayAdapter<ProgramGuideTimeDialogItem> {

        private LayoutInflater inflater;
        public ViewHolder holder = null;
        
        public ProgramGuideTimeDialogAdapter(FragmentActivity activity, final List<ProgramGuideTimeDialogItem> times) {
            super(activity, R.layout.program_guide_time_dialog, times);
            this.inflater = activity.getLayoutInflater();
        }

        public class ViewHolder {
            public TextView date1;
            public TextView date2;
            public TextView time;
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.program_guide_time_dialog, null);
                holder = new ViewHolder();
                holder.date1 = (TextView) view.findViewById(R.id.date1);
                holder.date2 = (TextView) view.findViewById(R.id.date2);
                holder.time = (TextView) view.findViewById(R.id.time);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ProgramGuideTimeDialogItem item = getItem(position);
            if (item != null) {
                // Get the date objects from the millisecond values
                final Date startDate = new Date(item.start);
                final Date endDate = new Date(item.end);

                // Convert the dates into a nice string representation
                final SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm", Locale.US);
                holder.time.setText(sdf1.format(startDate) + " - " + sdf1.format(endDate));

                final SimpleDateFormat sdf2 = new SimpleDateFormat("dd:MM:yyyy", Locale.US);
                Utils.setDate(holder.date1, startDate);
                holder.date2.setText(sdf2.format(startDate));
                
                if (holder.date1.getText().equals(holder.date2.getText())) {
                    holder.date2.setVisibility(View.GONE);
                }
            }
            return view;
        }
    }
}
