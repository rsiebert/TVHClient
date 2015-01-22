/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.MultiSpinner;
import org.tvheadend.tvhclient.MultiSpinner.MultiSpinnerListener;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class TimerRecordingAddFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private final static String TAG = TimerRecordingAddFragment.class.getSimpleName();

    private Activity activity;
    private TimerRecording rec;

    private CheckBox isEnabled;
    private EditText retention;
    private Spinner priority;
    private MultiSpinner daysOfWeek;
    private Spinner startTime;
    private Spinner stopTime;
    private EditText title;
    private EditText name;
    private EditText directory;
    private Spinner channelName;

    private Toolbar toolbar;

    private long priorityValue;
    private int startTimeValue;
    private int stopTimeValue;
    private int daysOfWeekValue;
    private String titleValue;
    private String nameValue;
    private String directoryValue;
    private int retentionValue;
    private boolean enabledValue;
    private int channelSelectionValue;

    public static TimerRecordingAddFragment newInstance(Bundle args) {
        TimerRecordingAddFragment f = new TimerRecordingAddFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        priorityValue = 2;
        startTimeValue = 720;
        stopTimeValue = 780;
        daysOfWeekValue = 127;
        titleValue = "Time-%x-%R";
        nameValue = "";
        directoryValue = "";
        retentionValue = 0;
        enabledValue = true;
        channelSelectionValue = 0;
        
        if (savedInstanceState != null) {
            priorityValue = savedInstanceState.getLong("priorityValue");
            startTimeValue = savedInstanceState.getInt("startTimeValue");
            stopTimeValue = savedInstanceState.getInt("stopTimeValue");
            daysOfWeekValue = savedInstanceState.getInt("daysOfWeekValue");
            titleValue = savedInstanceState.getString("titleValue");
            nameValue = savedInstanceState.getString("nameValue");
            directoryValue = savedInstanceState.getString("directoryValue");
            retentionValue = savedInstanceState.getInt("retentionValue");
            enabledValue = savedInstanceState.getBoolean("enabledValue");
            channelSelectionValue = savedInstanceState.getInt("channelNameValue");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("priorityValue", priorityValue);
        outState.putInt("startTimeValue", startTimeValue);
        outState.putInt("stopTimeValue", stopTimeValue);
        outState.putInt("daysOfWeekValue", daysOfWeekValue);
        outState.putString("titleValue", titleValue);
        outState.putString("nameValue", nameValue);
        outState.putString("directoryValue", directoryValue);
        outState.putInt("retentionValue", retentionValue);
        outState.putBoolean("enabledValue", enabledValue);
        outState.putInt("channelNameValue", channelSelectionValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (getDialog() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        String recId = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getString(Constants.BUNDLE_TIMER_RECORDING_ID);
        }

        // Get the recording so we can show its details
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        rec = app.getTimerRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.timer_recording_add_layout, container, false);
        channelName = (Spinner) v.findViewById(R.id.channel);
        isEnabled = (CheckBox) v.findViewById(R.id.is_enabled);
        title = (EditText) v.findViewById(R.id.title);
        name = (EditText) v.findViewById(R.id.name);
        retention = (EditText) v.findViewById(R.id.retention);
        daysOfWeek = (MultiSpinner) v.findViewById(R.id.days_of_week);
        startTime = (Spinner) v.findViewById(R.id.start_time);
        stopTime = (Spinner) v.findViewById(R.id.stop_time);
        priority = (Spinner) v.findViewById(R.id.priority);
        directory = (EditText) v.findViewById(R.id.directory);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        
        if (channelName != null) {
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            List<String> channels = new ArrayList<String>();
            for (Channel c : app.getChannels()) {
                channels.add(c.name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, channels);
            channelName.setAdapter(adapter);

            if (rec != null && rec.channel != null) {
                int pos = adapter.getPosition(rec.channel.name);
                channelName.setSelection(pos);
            } else {
                channelName.setSelection(channelSelectionValue);
            }
        }


        if (priority != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity, R.array.dvr_priorities, android.R.layout.simple_spinner_item);
            priority.setAdapter(adapter);
            priority.setSelection((int) (rec != null ? rec.priority : priorityValue));
        }

        
        if (daysOfWeek != null) {
            MultiSpinnerListener msl = new MultiSpinnerListener() {
                @Override
                public void onItemsSelected(boolean[] checked) {
                    // NOP
                }
            };

            long dow = (rec != null) ? rec.daysOfWeek : daysOfWeekValue;
            List<String> items = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.day_long_names)));
            List<String> textItems = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.day_short_names)));
            daysOfWeek.setItems(items, textItems, dow, msl);
        }
        
        if (startTime != null && stopTime != null) {
            // Converts the value in minutes into a time format
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.US);
            
            // Fill the list with the time values from 0:00 to 23:50
            List<String> timeList = new ArrayList<String>();
            for (int i = 0; i <= 1430; i += 10) {
                String time = formatter.format(new Date(i * 60L * 1000L));
                timeList.add(time);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, timeList);
            startTime.setAdapter(adapter);
            stopTime.setAdapter(adapter);

            if (rec != null) {
                String start = formatter.format(new Date(rec.start * 60L * 1000L));
                String stop = formatter.format(new Date(rec.stop * 60L * 1000L));
                startTime.setSelection(adapter.getPosition(start));
                stopTime.setSelection(adapter.getPosition(stop));
            } else {
                String start = formatter.format(new Date(startTimeValue * 60L * 1000L));
                String stop = formatter.format(new Date(stopTimeValue * 60L * 1000L));
                startTime.setSelection(adapter.getPosition(start));
                stopTime.setSelection(adapter.getPosition(stop));    
            }
        }

        if (title != null) {
            title.setText(rec != null ? rec.title : titleValue);
        }
        if (name != null) {
            name.setText(rec != null ? rec.name : nameValue);
        }
        if (directory != null) {
            directory.setText(rec != null ? rec.directory : directoryValue);
        }
        if (retention != null) {
            retention.setText(String.valueOf(rec != null ? rec.retention : retentionValue));
        }
        if (isEnabled != null) {
            isEnabled.setChecked(rec != null ? rec.enabled : enabledValue);
        }
        
        if (toolbar != null) {
            if (rec != null) {
                toolbar.setTitle((rec.name.length() > 0) ? rec.name : rec.title);
            }
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
            // Inflate a menu to be displayed in the toolbar
            toolbar.inflateMenu(R.menu.save_cancel_menu);
            toolbar.setTitle(R.string.add_recording);
        }
    }

    /**
     * 
     * @param item
     * @return
     */
    protected boolean onToolbarItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_save:
            Intent intent = new Intent(activity, HTSService.class);
            intent.setAction(Constants.ACTION_ADD_TIMER_REC_ENTRY);

            intent.putExtra("title", title.getText().toString());
            intent.putExtra("start", (long) (startTime.getSelectedItemPosition() * 10));
            intent.putExtra("stop", (long) (stopTime.getSelectedItemPosition() * 10));

            String cname = (String) channelName.getSelectedItem();
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            for (Channel c : app.getChannels()) {
                if (c.name.equals(cname)) {
                    intent.putExtra("channelId", c.id);
                    break;
                }
            }

            intent.putExtra("configName", "");
            intent.putExtra("retention", Long.valueOf(retention.getText().toString()));
            intent.putExtra("daysOfWeek", (long) daysOfWeek.getSpinnerValue());
            intent.putExtra("priority", (long) priority.getSelectedItemPosition());
            intent.putExtra("enabled", (long) ((isEnabled.isChecked() ? 1 : 0)));
            intent.putExtra("name", name.getText().toString());
            intent.putExtra("directory", directory.getText().toString());
            activity.startService(intent);

            getDialog().dismiss();
            return true;

        case R.id.menu_cancel:
            cancel();
            return true;
        }
        return false;
    }
    
    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the connection. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    public void cancel() {
        // Show confirmation dialog to cancel
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(getString(R.string.cancel_add_recording));
        builder.setTitle(getString(R.string.menu_cancel));

        // Define the action of the yes button
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                getDialog().dismiss();
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
