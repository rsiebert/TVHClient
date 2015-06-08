package org.tvheadend.tvhclient.fragments;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.TimerRecording;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

public class TimerRecordingAddFragment extends DialogFragment implements HTSListener {

    private final static String TAG = TimerRecordingAddFragment.class.getSimpleName();

    private Activity activity;
    private TimerRecording rec;
    private Toolbar toolbar;

    private CheckBox isEnabled;
    private TextView priority;
    private LinearLayout daysOfWeekLayout;
    private ToggleButton[] daysOfWeekButtons = new ToggleButton[7];
    private TextView startTime;
    private TextView stopTime;
    private EditText title;
    private EditText name;
    private TextView channelName;

    private long priorityValue;
    private long startTimeValue;
    private long stopTimeValue;
    private long daysOfWeekValue;
    private String titleValue;
    private String nameValue;
    private boolean enabledValue;
    private int channelSelectionValue;

    String[] channelList;
    String[] priorityList;

    private TVHClientApplication app;

    public static TimerRecordingAddFragment newInstance(Bundle args) {
        TimerRecordingAddFragment f = new TimerRecordingAddFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
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
        outState.putString("nameValue", nameValue);
        outState.putBoolean("enabledValue", enabledValue);
        outState.putInt("channelNameValue", channelSelectionValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

    	// Create the list of channels that the user can select
        channelList = new String[app.getChannels().size()+1];
        channelList[0] = activity.getString(R.string.no_channel);
        for (int i = 0; i < app.getChannels().size(); i++) {
        	channelList[i+1] = app.getChannels().get(i).name;
        }

        // Sort the channels in the list by name
        Arrays.sort(channelList, new Comparator<String>() {
            public int compare(String x, String y) {
                if (x != null && y != null) {
                    if (y.equals(activity.getString(R.string.no_channel))) {
                        return 1;
                    } else {
                        return x.toLowerCase(Locale.US).compareTo(
                                y.toLowerCase(Locale.US));
                    }
                }
                return 0;
            }
        });

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);

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
            rec = app.getTimerRecording(recId);
            if (rec != null) {
                priorityValue = rec.priority;
                startTimeValue = rec.start;
                stopTimeValue = rec.stop;
                daysOfWeekValue = rec.daysOfWeek;
                titleValue = rec.title;
                nameValue = rec.name;
                enabledValue = rec.enabled;

                // The default value is no channel
                channelSelectionValue = 0;
                // Get the position of the given channel in the channelList
                if (rec.channel != null) {
                    for (int i = 0; i < channelList.length; i++) {
                        if (channelList[i].equals(rec.channel.name)) {
                            channelSelectionValue = i;
                            break;
                        }
                    }   
                }
            } else {
                // No recording was given, set default values
                Calendar cal = Calendar.getInstance();
                long currentTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                priorityValue = 2;
                startTimeValue = currentTime;
                stopTimeValue = currentTime + 30;
                daysOfWeekValue = 127;
                titleValue = "";
                nameValue = "";
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
            nameValue = savedInstanceState.getString("nameValue");
            enabledValue = savedInstanceState.getBoolean("enabledValue");
            channelSelectionValue = savedInstanceState.getInt("channelNameValue");
        }

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.timer_recording_add_layout, container, false);
        channelName = (TextView) v.findViewById(R.id.channel);
        isEnabled = (CheckBox) v.findViewById(R.id.is_enabled);
        title = (EditText) v.findViewById(R.id.title);
        name = (EditText) v.findViewById(R.id.name);

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
        priority = (TextView) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        isEnabled.setChecked(enabledValue);
        isEnabled.setVisibility(app.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_ENABLED ? View.VISIBLE : View.GONE);

        title.setText(titleValue);
        name.setText(nameValue);

        channelName.setText(channelList[channelSelectionValue]);
        channelName.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				new MaterialDialog.Builder(activity)
	            .title(R.string.select_channel)
	            .items(channelList)
	            .itemsCallbackSingleChoice(channelSelectionValue, new MaterialDialog.ListCallbackSingleChoice() {
	                @Override
	                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        channelName.setText(channelList[which]);
                        channelSelectionValue = which;
	                    return true;
	                }
	            })
	            .show();
			}
        });

        priority.setText(priorityList[(int) priorityValue]);
        priority.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				new MaterialDialog.Builder(activity)
	            .title(R.string.select_priority)
	            .items(priorityList)
	            .itemsCallbackSingleChoice((int) priorityValue, new MaterialDialog.ListCallbackSingleChoice() {
	                @Override
	                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        priority.setText(priorityList[which]);
                        priorityValue = which;
	                    return true;
	                }
	            })
	            .show();
			}
        });

        startTime.setText(Utils.getTimeStringFromValue(startTimeValue));
        // Show the time picker dialog so the user can select a new starting time
        startTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour = (int) (startTimeValue / 60);
                int minute = (int) (startTimeValue % 60);

                TimePickerDialog mTimePicker;
                mTimePicker = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                        // Save the given value in seconds. This values will be passed to the server
                        startTimeValue = (long) (selectedHour * 60 + selectedMinute);
                        startTime.setText(Utils.getTimeStringFromValue(startTimeValue));
                    }
                }, hour, minute, true, false);

                mTimePicker.setCloseOnSingleTapMinute(false);
                mTimePicker.show(getChildFragmentManager(), "");
            }
        });

        stopTime.setText(Utils.getTimeStringFromValue(stopTimeValue));
        // Show the time picker dialog so the user can select a new starting time
        stopTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour = (int) (stopTimeValue / 60);
                int minute = (int) (stopTimeValue % 60);

                TimePickerDialog mTimePicker;
                mTimePicker = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                        // Save the given value in seconds. This values will be passed to the server
                        stopTimeValue = (long) (selectedHour * 60 + selectedMinute);
                        stopTime.setText(Utils.getTimeStringFromValue(stopTimeValue));
                    }
                }, hour, minute, true, false);

                mTimePicker.setCloseOnSingleTapMinute(false);
                mTimePicker.show(getChildFragmentManager(), "");
            }
        });

        // Set the correct days as checked or not depending on the given value.
        // For each day shift the daysOfWeekValue by one to the right and check
        // if the bit at this position is one. 
        for (int i = 0; i < 7; i++) {
            int checked = (((int) daysOfWeekValue >> i) & 1);
            daysOfWeekButtons[i].setChecked(checked == 1);
        }

        if (toolbar != null) {
            toolbar.setTitle(rec != null ? R.string.edit_timer_recording : R.string.add_timer_recording);
            toolbar.inflateMenu(R.menu.save_cancel_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
        }
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(false);
        }
    }

	@Override
	public void onResume() {
		super.onResume();
		getDialog().setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)) {
					getDialog().setOnKeyListener(null);
					cancel();
				}
				return false;
			}
		});
		app.addListener(this);
	}

	@Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
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
        nameValue = name.getText().toString();
        enabledValue = isEnabled.isChecked();
        daysOfWeekValue = getDayOfWeekValue();
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
            Toast.makeText(activity, getString(R.string.error_empty_title),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // The channel must be set
        if (channelSelectionValue == 0) {
            Toast.makeText(activity, getString(R.string.error_no_channel),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // If the timer recording has been edited, remove it before adding it
        // again with the updated values. This is required because the API does
        // not provide an edit service call. When the confirmation that the
        // recording was received, add the edited one. This is done in the
        // onMessage method. 
        if (rec != null && rec.id != null && rec.id.length() > 0) {
            Intent intent = new Intent(activity, HTSService.class);
            intent.setAction(Constants.ACTION_DELETE_TIMER_REC_ENTRY);
            intent.putExtra("id", rec.id);
            activity.startService(intent);
        } else {
            addTimerRecording();
        }
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private void cancel() {
        // Show confirmation dialog to cancel
        new MaterialDialog.Builder(activity)
                .content(R.string.cancel_add_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        if (getDialog() != null) {
                            getDialog().dismiss();
                        }
                    }
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.cancel();
                    }
                }).show();
    }

    /**
     * Returns a number where each bit position is one day. If the bit position
     * is one then the day was selected.
     * 
     * @return Number with the selected day on each bit position
     */
    private long getDayOfWeekValue() {
        long value = 0;
        for (int i = 0; i < 7; i++) {
            if (daysOfWeekButtons[i].isChecked()) {
                value += (1 << i);
            }
        }
        return value;
    }

    @Override
    public void onMessage(String action, Object obj) {
        if (action.equals(Constants.ACTION_TIMER_DVR_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    addTimerRecording();
                }
            });
        }
    }

    /**
     * Adds a new timer recording with the given values. This method is also
     * called when a recording is being edited. It adds a recording with edited
     * values which was previously removed.
     */
    private void addTimerRecording() {
     
        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_ADD_TIMER_REC_ENTRY);
        intent.putExtra("title", titleValue);
        intent.putExtra("name", nameValue);
        intent.putExtra("start", startTimeValue);
        intent.putExtra("stop", stopTimeValue);
        intent.putExtra("daysOfWeek", daysOfWeekValue);
        intent.putExtra("priority", priorityValue);
        intent.putExtra("enabled", (long) (enabledValue ? 1 : 0));

        // The id must be passed on to the server, not the name. So go through
        // all available channels and get the id for the selected channel name.
        for (Channel c : app.getChannels()) {
            if (c.name.equals(channelName.getText().toString())) {
                intent.putExtra("channelId", c.id);
                break;
            }
        }

        // Add the recording profile if available and enabled
        final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
        final Profile p = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
        if (p != null 
                && p.enabled
                && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            intent.putExtra("configName", p.name);
        }

        activity.startService(intent);

        if (getDialog() != null) {
            ((FragmentStatusInterface) activity).listDataInvalid(TAG);
            getDialog().dismiss();
        }
    }
}
