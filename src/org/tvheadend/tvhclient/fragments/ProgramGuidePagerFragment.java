package org.tvheadend.tvhclient.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ProgramGuidePagerFragment  extends Fragment implements FragmentControlInterface {

    private final static String TAG = ProgramGuidePagerFragment.class.getSimpleName();

    private Activity activity;
    private static ViewPager viewPager = null;
    private ProgramGuidePagerAdapter adapter = null;

    // The dialog that allows the user to select a certain time frame
    private AlertDialog programGuideTimeDialog;
    
    // The time frame (start and end times) that shall be shown in a single fragment.  
    private static List<Long> startTimes = new ArrayList<Long>();
    private static List<Long> endTimes = new ArrayList<Long>();
    private static int daysToShow;
    private static int hoursToShow;
    private static int fragmentCount;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView");
        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
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
        Log.d(TAG, "onAttach");
        this.activity = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");

        // Calculate the max number of fragments in the view pager 
        calcFragmentCount();
        calcProgramGuideTimeslots();
        createProgramGuideTimeDialog();

        adapter = new ProgramGuidePagerAdapter(getChildFragmentManager(), activity);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        adapter.notifyDataSetChanged();
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
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
            // Show the dialog to select the time
            programGuideTimeDialog.show();
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
        builder.setAdapter(new ProgramGuideTimeDialogAdapter(activity, times), 
                           new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                viewPager.setCurrentItem(pos);
            }
        });
        programGuideTimeDialog = builder.create();
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
        
        public ProgramGuideTimeDialogAdapter(Activity activity, final List<ProgramGuideTimeDialogItem> times) {
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
    
    /**
     * Calculates the number of fragments in the view pager.
     * This depends on how many days shall be shown of the program 
     * guide and how many hours shall be visible per fragment.
     */
    private void calcFragmentCount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
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

    @Override
    public void reloadData() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setInitialSelection(final int position) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSelection(final int position) {
        // Get the currently visible fragment
        Fragment f = getChildFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":0");
        if (f instanceof ProgramGuideListFragment && f instanceof FragmentControlInterface) {
            ((FragmentControlInterface) f).setSelection(position);
        }
    }

    @Override
    public void setSelectionFromTop(final int position, final int offset) {
        // Get all currently visible fragments
        for (int i = 0; i < fragmentCount; ++i) {
            Fragment f = getChildFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + adapter.getItemId(i));
            if (f instanceof ProgramGuideListFragment && f instanceof FragmentControlInterface) {
                ((FragmentControlInterface) f).setSelectionFromTop(position, offset);
            }
        }
    }
}
