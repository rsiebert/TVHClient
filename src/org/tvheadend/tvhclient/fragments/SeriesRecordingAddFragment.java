package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.SeriesRecording;

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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SeriesRecordingAddFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private final static String TAG = SeriesRecordingAddFragment.class.getSimpleName();

    private Activity activity;
    private SeriesRecording rec;
    private Toolbar toolbar;

    private CheckBox isEnabled;
    private Spinner priority;
    private CheckBox monday;
    private CheckBox tuesday;
    private CheckBox wednesday;
    private CheckBox thursday;
    private CheckBox friday;
    private CheckBox saturday;
    private CheckBox sunday;
    private EditText minDuration;
    private EditText maxDuration;
    private EditText startTime;
    private EditText stopTime;
    private EditText title;
    private Spinner channelName;

    private long priorityValue;
    private long minDurationValue;
    private long maxDurationValue;
    private long startTimeValue;
    private long stopTimeValue;
    private long daysOfWeekValue;
    private String titleValue;
    private boolean enabledValue;
    private int channelSelectionValue;

    public static SeriesRecordingAddFragment newInstance(Bundle args) {
        SeriesRecordingAddFragment f = new SeriesRecordingAddFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("priorityValue", priorityValue);
        outState.putLong("minDurationValue", minDurationValue);
        outState.putLong("maxDurationValue", maxDurationValue);
        outState.putLong("startTimeValue", startTimeValue);
        outState.putLong("stopTimeValue", stopTimeValue);
        outState.putLong("daysOfWeekValue", getDayOfWeekValue());
        outState.putString("titleValue", titleValue);
        outState.putBoolean("enabledValue", enabledValue);
        outState.putInt("channelNameValue", channelSelectionValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.series_recording_add_layout, container, false);
        channelName = (Spinner) v.findViewById(R.id.channel);
        isEnabled = (CheckBox) v.findViewById(R.id.is_enabled);
        title = (EditText) v.findViewById(R.id.title);
        minDuration = (EditText) v.findViewById(R.id.minimum_duration);
        maxDuration = (EditText) v.findViewById(R.id.maximum_duration);
        monday = (CheckBox) v.findViewById(R.id.monday);
        tuesday = (CheckBox) v.findViewById(R.id.tuesday);
        wednesday = (CheckBox) v.findViewById(R.id.wednesday);
        thursday = (CheckBox) v.findViewById(R.id.thursday);
        friday = (CheckBox) v.findViewById(R.id.friday);
        saturday = (CheckBox) v.findViewById(R.id.saturday);
        sunday = (CheckBox) v.findViewById(R.id.sunday);
        startTime = (EditText) v.findViewById(R.id.start_extra);
        stopTime = (EditText) v.findViewById(R.id.stop_extra);
        priority = (Spinner) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

        if (savedInstanceState == null) {
            String recId = "";
            Bundle bundle = getArguments();
            if (bundle != null) {
                recId = bundle.getString(Constants.BUNDLE_SERIES_RECORDING_ID);
            }

            // Get the recording so we can show its details
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            rec = app.getSeriesRecording(recId);
            if (rec != null) {
                priorityValue = rec.priority;
                minDurationValue = rec.minDuration;
                maxDurationValue = rec.maxDuration;
                startTimeValue = rec.start;
                stopTimeValue = rec.start;
                daysOfWeekValue = rec.daysOfWeek;
                titleValue = rec.title;
                enabledValue = rec.enabled;

                int pos = app.getChannels().indexOf(rec.channel);
                channelSelectionValue = (pos >= 0 ? pos : 0);
            } else {
                priorityValue = 2;
                minDurationValue = 0;
                maxDurationValue = 0;
                startTimeValue = 0;
                stopTimeValue = 0;
                daysOfWeekValue = 127;
                titleValue = "";
                enabledValue = true;
                channelSelectionValue = 0;
            }
        } else {
            priorityValue = savedInstanceState.getLong("priorityValue");
            minDurationValue = savedInstanceState.getLong("minDurationValue");
            maxDurationValue = savedInstanceState.getLong("maxDurationValue");
            startTimeValue = savedInstanceState.getLong("startTimeValue");
            stopTimeValue = savedInstanceState.getLong("stopTimeValue");
            daysOfWeekValue = savedInstanceState.getLong("daysOfWeekValue");
            titleValue = savedInstanceState.getString("titleValue");
            enabledValue = savedInstanceState.getBoolean("enabledValue");
            channelSelectionValue = savedInstanceState.getInt("channelNameValue");
        }

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
            channelName.setSelection(channelSelectionValue);
        }
        if (priority != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity, R.array.dvr_priorities, android.R.layout.simple_spinner_item);
            priority.setAdapter(adapter);
            priority.setSelection((int) priorityValue);
        }
        if (minDuration != null) {
            minDuration.setText(String.valueOf(minDurationValue));
        }
        if (maxDuration != null) {
            maxDuration.setText(String.valueOf(maxDurationValue));
        }
        if (startTime != null) {
            startTime.setText(String.valueOf(startTimeValue));
        }
        if (stopTime != null) {
            stopTime.setText(String.valueOf(stopTimeValue));
        }
        if (monday != null) {
            monday.setChecked((daysOfWeekValue >> 0) == 127);
        }
        if (tuesday != null) {
            tuesday.setChecked((daysOfWeekValue >> 1) == 63);
        }
        if (wednesday != null) {
            wednesday.setChecked((daysOfWeekValue >> 2) == 31);
        }
        if (thursday != null) {
            thursday.setChecked((daysOfWeekValue >> 3) == 15);
        }
        if (friday != null) {
            friday.setChecked((daysOfWeekValue >> 4) == 7);
        }
        if (saturday != null) {
            saturday.setChecked((daysOfWeekValue >> 5) == 3);
        }
        if (sunday != null) {
            sunday.setChecked((daysOfWeekValue >> 6) == 1);
        }
        if (title != null) {
            title.setText(titleValue);
        }
        if (isEnabled != null) {
            isEnabled.setChecked(enabledValue);
        }
        if (getDialog() != null) {
            getDialog().setTitle(R.string.add_series_recording);
        }
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
            // Inflate a menu to be displayed in the toolbar
            toolbar.inflateMenu(R.menu.save_cancel_menu);
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
            save();
            return true;

        case R.id.menu_cancel:
            cancel();
            return true;
        }
        return false;
    }

    /**
     * 
     */
    private void save() {
        // title is mandatory and must be set
        if (title.length() == 0) {
            Toast.makeText(activity,
                    getString(R.string.series_recording_add_error),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_ADD_SERIES_DVR_ENTRY);
        intent.putExtra("title", title.getText().toString());

        try {
            long min = Long.valueOf(minDuration.getText().toString());
            intent.putExtra("minDuration", min);
        } catch (NumberFormatException ex) {
            intent.putExtra("minDuration", 60);
        }
        try {
            long max = Long.valueOf(maxDuration.getText().toString());
            intent.putExtra("maxDuration", max);
        } catch (NumberFormatException ex) {
            intent.putExtra("maxDuration", 120);
        }
        try {
            long max = Long.valueOf(startTime.getText().toString());
            intent.putExtra("startExtra", max);
        } catch (NumberFormatException ex) {
            intent.putExtra("startExtra", 0);
        }
        try {
            long max = Long.valueOf(stopTime.getText().toString());
            intent.putExtra("stopExtra", max);
        } catch (NumberFormatException ex) {
            intent.putExtra("stopExtra", 0);
        }

        String cname = (String) channelName.getSelectedItem();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        for (Channel c : app.getChannels()) {
            if (c.name.equals(cname)) {
                intent.putExtra("channelId", c.id);
                break;
            }
        }

        intent.putExtra("daysOfWeek", getDayOfWeekValue());
        intent.putExtra("priority", (long) priority.getSelectedItemPosition());
        intent.putExtra("enabled", (long) ((isEnabled.isChecked() ? 1 : 0)));
        activity.startService(intent);

        if (getDialog() != null) {
            getDialog().dismiss();
        }
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

        // Define the action of the yes button
        builder.setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * 
     * @return
     */
    public long getDayOfWeekValue() {
        long value = 0;
        if (monday.isChecked()) {
            value += (1 << 0);
        }
        if (tuesday.isChecked()) {
            value += (1 << 1);
        }
        if (wednesday.isChecked()) {
            value += (1 << 2);
        }
        if (thursday.isChecked()) {
            value += (1 << 3);
        }
        if (friday.isChecked()) {
            value += (1 << 4);
        }
        if (saturday.isChecked()) {
            value += (1 << 5);
        }
        if (sunday.isChecked()) {
            value += (1 << 6);
        }
        return value;
    }
}
