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
import android.util.DisplayMetrics;
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
        getValues();
        outState.putLong("priorityValue", priorityValue);
        outState.putLong("startTimeValue", startTimeValue);
        outState.putLong("stopTimeValue", stopTimeValue);
        outState.putLong("daysOfWeekValue", daysOfWeekValue);
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

        // For the shown days in each toggle button the array with the short
        // names is used. If the screen width is not large enough then the short
        // names of all seven days would not fit. Therefore reduce the number of
        // shown letters for each day depending on the screen width.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        final int displayWidth = displaymetrics.widthPixels;

        daysOfWeekLayout = (LinearLayout) v.findViewById(R.id.days_of_week_layout);
        String[] shortDays = getResources().getStringArray(R.array.day_short_names);
        for (int i = 0; i < 7; i++) {
            final ToggleButton dayButton = (ToggleButton) inflater.inflate(R.layout.day_toggle_button, daysOfWeekLayout, false);

            // Show only one character on width below 800, two characters below
            // 1000 and all characters on all remaining ones
            if (displayWidth < 800) {
                dayButton.setTextOn(shortDays[i].subSequence(0, 1));
                dayButton.setTextOff(shortDays[i].subSequence(0, 1));
            } else if (displayWidth < 1000) {
                dayButton.setTextOn(shortDays[i].subSequence(0, 2));
                dayButton.setTextOff(shortDays[i].subSequence(0, 2));
            } else {
                dayButton.setTextOn(shortDays[i]);
                dayButton.setTextOff(shortDays[i]);
            }

            // Add the button to the layout and store it in the list to have
            // access to it later
            daysOfWeekLayout.addView(dayButton);
            daysOfWeekButtons[i] = dayButton;
        }

        startTime = (TextView) v.findViewById(R.id.start_time);
        stopTime = (TextView) v.findViewById(R.id.stop_time);
        priority = (Spinner) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

        // If the savedInstanceState is null then the fragment was created for
        // the first time. Either get the given id to edit the recording or
        // create new one. Otherwise an orientation change has occurred and the
        // saved values must be applied to the user input elements.
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
                // No recording was given, set default values
                priorityValue = 2;
                startTimeValue = 0;
                stopTimeValue = 0;
                daysOfWeekValue = 127;
                titleValue = "";
                enabledValue = true;
                channelSelectionValue = 0;
            }
        } else {
            // Restore the values before the orientation change
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
        if (title != null) {
            title.setText(titleValue);
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
            startTime.setText(getTimeStringFromValue(startTimeValue));
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
                            startTime.setText(getTimeStringFromValue(startTimeValue));
                        }
                    }, hour, minute, true);
                    mTimePicker.setTitle(R.string.select_start_time);
                    mTimePicker.show();
                }
            });
        }
        if (stopTime != null) {
            stopTime.setText(getTimeStringFromValue(stopTimeValue));
            // Show the time picker dialog so the user can select a new starting time
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
                            stopTime.setText(getTimeStringFromValue(stopTimeValue));
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
        for (int i = 0; i < 7; i++) {
            int checked = (((int) daysOfWeekValue >> i) & 1);
            daysOfWeekButtons[i].setChecked(checked == 1);
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
        if (getDialog() != null) {
            getDialog().setTitle(R.string.add_timer_recording);
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
     * Retrieves and checks the values from the user input elements and stores
     * them in internal variables. These are used to remember the values during
     * an orientation change or when the recording shall be saved. The values
     * from the time pickers are not saved again, because they are saved on
     * every new time selection.
     */
    private void getValues() {
        titleValue = title.getText().toString();
        daysOfWeekValue = getDayOfWeekValue();
        priorityValue = priority.getSelectedItemPosition();
        enabledValue = isEnabled.isChecked();
        channelSelectionValue = channelName.getSelectedItemPosition();
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        getValues();

        // The title must not be empty
        if (titleValue.length() == 0) {
            Toast.makeText(activity,
                    getString(R.string.time_recording_add_error) + "\n" +
                    getString(R.string.error_empty_title),
                    Toast.LENGTH_LONG).show();
            return;
        }
        // The stop time must be later then the start time 
        if (startTimeValue >= stopTimeValue) {
            Toast.makeText(activity,
                    getString(R.string.time_recording_add_error) + "\n" +
                    getString(R.string.error_start_stop_time),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_ADD_TIMER_REC_ENTRY);
        intent.putExtra("title", titleValue);
        intent.putExtra("start", startTimeValue);
        intent.putExtra("stop", stopTimeValue);
        intent.putExtra("daysOfWeek", daysOfWeekValue);
        intent.putExtra("priority", priorityValue);
        intent.putExtra("enabled", (long) (enabledValue ? 1 : 0));

        // The id must be passed on to the server, not the name. So go through
        // all available channels and get the id for the selected channel name.
        String cname = (String) channelName.getSelectedItem();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        for (Channel c : app.getChannels()) {
            if (c.name.equals(cname)) {
                intent.putExtra("channelId", c.id);
                break;
            }
        }

        activity.startService(intent);

        if (getDialog() != null) {
            getDialog().dismiss();
        }
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
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
     * Returns a number where each bit position is one day. If the bit position
     * is one then the day was selected.
     * 
     * @return Number with the selected day on each bit position
     */
    public long getDayOfWeekValue() {
        long value = 0;
        for (int i = 0; i < 7; i++) {
            if (daysOfWeekButtons[i].isChecked()) {
                value += (1 << i);
            }
        }
        return value;
    }

    /**
     * Set the time from the long value. Prepend leading zeros to the hours or
     * minutes in case they are lower then ten.
     * 
     * @return time in hh:mm format
     */
    private String getTimeStringFromValue(long time) {
        String minutes = String.valueOf(time % 60);
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        String hours = String.valueOf(time / 60);
        if (hours.length() == 1) {
            hours = "0" + hours;
        }
        return (hours + ":" + minutes);
    }
}
