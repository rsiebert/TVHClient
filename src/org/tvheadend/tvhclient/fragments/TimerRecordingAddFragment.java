package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

public class TimerRecordingAddFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private final static String TAG = TimerRecordingAddFragment.class.getSimpleName();

    private ActionBarActivity activity;
    private TimerRecording rec;
    private Toolbar toolbar;

    private CheckBox isEnabled;
    private Spinner priority;
    private LinearLayout daysOfWeekLayout;
    private ToggleButton[] daysOfWeekButtons = new ToggleButton[7];
    private TextView startTime;
    private TextView stopTime;
    private EditText title;
    private Spinner channelName;

    private long priorityValue;
    private long startTimeValue;
    private long stopTimeValue;
    private long daysOfWeekValue;
    private String titleValue;
    private boolean enabledValue;
    private int channelSelectionValue;

    public static TimerRecordingAddFragment newInstance(Bundle args) {
        TimerRecordingAddFragment f = new TimerRecordingAddFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
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
        View v = inflater.inflate(R.layout.timer_recording_add_layout, container, false);
        channelName = (Spinner) v.findViewById(R.id.channel);
        isEnabled = (CheckBox) v.findViewById(R.id.is_enabled);
        title = (EditText) v.findViewById(R.id.title);

        daysOfWeekLayout = (LinearLayout) v.findViewById(R.id.days_of_week_layout);
        String[] shortDays = getResources().getStringArray(R.array.day_short_names);
        for (int i = 0; i < 7; i++) {
            final ToggleButton dayButton = (ToggleButton) inflater.inflate(R.layout.day_toggle_button, daysOfWeekLayout, false);
            dayButton.setTextOn(shortDays[i]);
            dayButton.setTextOff(shortDays[i]);
            daysOfWeekLayout.addView(dayButton);
            daysOfWeekButtons[i] = dayButton;
        }

        startTime = (TextView) v.findViewById(R.id.start_time);
        stopTime = (TextView) v.findViewById(R.id.stop_time);
        priority = (Spinner) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

        if (savedInstanceState == null) {
            String recId = "";
            Bundle bundle = getArguments();
            if (bundle != null) {
                recId = bundle.getString(Constants.BUNDLE_TIMER_RECORDING_ID);
            }

            // Get the recording so we can show its details
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            rec = app.getTimerRecording(recId);
            if (rec != null) {
                priorityValue = rec.priority;
                startTimeValue = rec.start;
                stopTimeValue = rec.start;
                daysOfWeekValue = rec.daysOfWeek;
                titleValue = rec.title;
                enabledValue = rec.enabled;

                int pos = app.getChannels().indexOf(rec.channel);
                channelSelectionValue = (pos >= 0 ? pos : 0);
            } else {
                priorityValue = 2;
                startTimeValue = 0;
                stopTimeValue = 0;
                daysOfWeekValue = 127;
                titleValue = "";
                enabledValue = true;
                channelSelectionValue = 0;
            }
        } else {
            priorityValue = savedInstanceState.getLong("priorityValue");
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

        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        if (isEnabled != null) {
            isEnabled.setVisibility((app.getProtocolVersion() >= 18) ? View.VISIBLE : View.GONE);
            isEnabled.setChecked(enabledValue);
        }
        if (channelName != null) {
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

        if (startTime != null) {
            // Set the time from the long value. Prepend leading zeros to the
            // hours or minutes in case they are lower then ten.
            String minutes = String.valueOf(startTimeValue % 60);
            if (minutes.length() == 1) {
                minutes = "0" + minutes;
            }
            String hours = String.valueOf(startTimeValue / 60);
            if (hours.length() == 1) {
                hours = "0" + hours;
            }
            startTime.setText(hours + ":" + minutes);

            // Show the time picker dialog so the user can select a new starting time
            startTime.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Calendar time = Calendar.getInstance();
                    int hour = time.get(Calendar.HOUR_OF_DAY);
                    int minute = time.get(Calendar.MINUTE);
                    TimePickerDialog mTimePicker;
                    mTimePicker = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                            // Save the given value in seconds. This values will be passed to the server
                            startTimeValue = (long) (selectedHour * 60 + selectedMinute);
                            // Set the time from the two values. Prepend leading zeros to the
                            // hours or minutes in case they are lower then ten.
                            String hours = selectedHour < 10 ? ("0" + String.valueOf(selectedHour)) : String.valueOf(selectedHour);
                            String minutes = selectedHour < 10 ? ("0" + String.valueOf(selectedMinute)) : String.valueOf(selectedMinute);
                            startTime.setText(hours + ":" + minutes);
                        }
                    }, hour, minute, true);
                    mTimePicker.setTitle(R.string.select_start_time);
                    mTimePicker.show();
                }
            });
        }
        if (stopTime != null) {
            // Set the time from the long value. Prepend leading zeros to the
            // hours or minutes in case they are lower then ten.
            String minutes = String.valueOf(stopTimeValue % 60);
            if (minutes.length() == 1) {
                minutes = "0" + minutes;
            }
            String hours = String.valueOf(stopTimeValue / 60);
            if (hours.length() == 1) {
                hours = "0" + hours;
            }
            stopTime.setText(hours + ":" + minutes);
            stopTime.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Calendar time = Calendar.getInstance();
                    int hour = time.get(Calendar.HOUR_OF_DAY);
                    int minute = time.get(Calendar.MINUTE);
                    TimePickerDialog mTimePicker;
                    mTimePicker = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                            // Save the given value in seconds. This values will be passed to the server
                            stopTimeValue = (long) (selectedHour * 60 + selectedMinute);
                            // Set the time from the two values. Prepend leading zeros to the
                            // hours or minutes in case they are lower then ten.
                            String hours = selectedHour < 10 ? ("0" + String.valueOf(selectedHour)) : String.valueOf(selectedHour);
                            String minutes = selectedHour < 10 ? ("0" + String.valueOf(selectedMinute)) : String.valueOf(selectedMinute);
                            stopTime.setText(hours + ":" + minutes);
                        }
                    }, hour, minute, true);
                    mTimePicker.setTitle(R.string.select_stop_time);
                    mTimePicker.show();
                }
            });
        }

        // Set the correct days as checked or not depending on the given value.
        // For each day shift the daysOfWeekValue by one to the right and check
        // if the bit at this position is one. 
        int result = 127;
        int position = 0;
        for (int i = 0; i < 7; i++) {
            daysOfWeekButtons[i].setChecked((daysOfWeekValue >> position) == result);
            position++;
            result = result / 2;
        }

        if (title != null) {
            title.setText(titleValue);
        }
        if (getDialog() != null) {
            getDialog().setTitle(R.string.add_timer_recording);
        }
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.save_cancel_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
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
                    getString(R.string.timer_recording_add_error),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_ADD_TIMER_REC_ENTRY);
        intent.putExtra("title", title.getText().toString());
        intent.putExtra("start", startTimeValue);
        intent.putExtra("stop", stopTimeValue);

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
        int position = 0;
        for (int i = 0; i < 7; i++) {
            if (daysOfWeekButtons[i].isChecked()) {
                value += (1 << position);
                position++;
            }
        }
        return value;
    }
}
