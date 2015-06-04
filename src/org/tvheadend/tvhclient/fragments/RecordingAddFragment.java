package org.tvheadend.tvhclient.fragments;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

@SuppressWarnings("deprecation")
public class RecordingAddFragment extends DialogFragment implements OnClickListener {

    private final static String TAG = RecordingAddFragment.class.getSimpleName();

    private ActionBarActivity activity;
    private Recording rec;
    private Toolbar toolbar;

    private TextView startTime;
    private TextView stopTime;
    private TextView startDate;
    private TextView stopDate;
    private TextView priority;
    private EditText startExtra;
    private EditText stopExtra;
    private EditText title;
    private EditText description;
    private TextView channelName;

    // Extra pre- and postrecording times in seconds
    private long startExtraValue;
    private long stopExtraValue;

    // Start and end recording times. They are saved in the calendar object and
    // will be converted to milliseconds when saved during an orientation change
    // and converted to seconds before passed to the server.
    private Calendar startValue = Calendar.getInstance();
    private Calendar stopValue = Calendar.getInstance();
    private long priorityValue;
    private String titleValue;
    private String descriptionValue;
    private int channelSelectionValue;

    String[] channelList;
    String[] priorityList;

    private TVHClientApplication app;

    private static final int DEFAULT_START_EXTRA = 2;
    private static final int DEFAULT_STOP_EXTRA = 2;

    public static RecordingAddFragment newInstance(Bundle args) {
        RecordingAddFragment f = new RecordingAddFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
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
        outState.putLong("startTimeValue", startValue.getTimeInMillis());
        outState.putLong("stopTimeValue", stopValue.getTimeInMillis());
        outState.putLong("startExtraValue", startExtraValue);
        outState.putLong("stopExtraValue", stopExtraValue);
        outState.putString("titleValue", titleValue);
        outState.putString("descriptionValue", descriptionValue);
        outState.putInt("channelNameValue", channelSelectionValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Create the list of channels that the user can select
        channelList = new String[app.getChannels().size()];
        for (int i = 0; i < app.getChannels().size(); i++) {
            channelList[i] = app.getChannels().get(i).name;
        }

        // Sort the channels in the list by name
        Arrays.sort(channelList, new Comparator<String>() {
            public int compare(String x, String y) {
                return x.toLowerCase(Locale.US).compareTo(
                        y.toLowerCase(Locale.US));
            }
        });

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);

        // If the savedInstanceState is null then the fragment was created for
        // the first time. Either get the given id to edit the recording or
        // create new one. Otherwise an orientation change has occurred and the
        // saved values must be applied to the user input elements.
        if (savedInstanceState == null) {
            long recId = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                recId = bundle.getLong(Constants.BUNDLE_RECORDING_ID);
            }

            // Get the recording so we can show its detail
            rec = app.getRecording(recId);
            if (rec != null) {
                priorityValue = rec.priority;
                startExtraValue = rec.startExtra;
                stopExtraValue = rec.stopExtra;
                startValue.setTimeInMillis(rec.start.getTime());
                stopValue.setTimeInMillis(rec.stop.getTime());
                titleValue = rec.title;
                descriptionValue = rec.description;

                // Get the position of the given channel in the channelList 
                channelSelectionValue = 0;
                for (int i = 0; i < channelList.length; i++) {
                    if (channelList[i].equals(rec.channel.name)) {
                        channelSelectionValue = i;
                        break;
                    }
                }
            } else {
                priorityValue = 2;
                startExtraValue = 0;
                stopExtraValue = 0;
                stopValue.setTimeInMillis(startValue.getTimeInMillis() + 30 * 60000); // add 30 min
                titleValue = "";
                descriptionValue = "";
                channelSelectionValue = 0;
            }
        } else {
            // Restore the values before the orientation change
            priorityValue = savedInstanceState.getLong("priorityValue");
            startExtraValue = savedInstanceState.getLong("startExtraValue");
            stopExtraValue = savedInstanceState.getLong("stopExtraValue");
            startValue.setTimeInMillis(savedInstanceState.getLong("startTimeValue"));
            stopValue.setTimeInMillis(savedInstanceState.getLong("stopTimeValue"));
            titleValue = savedInstanceState.getString("titleValue");
            descriptionValue = savedInstanceState.getString("descriptionValue");
            channelSelectionValue = savedInstanceState.getInt("channelNameValue");
        }

        // Assume a new recording shall be added. If a recording was given then
        // show the layouts to edit it. If the recording is already being
        // recorded show only the title, stop and extra stop times.
        int layout = R.layout.recording_add_layout;
        if (rec != null) {
            layout = (rec.isRecording() ? R.layout.recording_edit_recording_layout
                    : R.layout.recording_edit_scheduled_layout);
        }
        View v = inflater.inflate(layout, container, false);

        // Initialize all the widgets from the layout
        title = (EditText) v.findViewById(R.id.title);
        description = (EditText) v.findViewById(R.id.description);
        channelName = (TextView) v.findViewById(R.id.channel);
        startExtra = (EditText) v.findViewById(R.id.start_extra);
        stopExtra = (EditText) v.findViewById(R.id.stop_extra);
        startTime = (TextView) v.findViewById(R.id.start_time);
        stopTime = (TextView) v.findViewById(R.id.stop_time);
        startDate = (TextView) v.findViewById(R.id.start_date);
        stopDate = (TextView) v.findViewById(R.id.stop_date);
        priority = (TextView) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (title != null) {
            title.setVisibility(app.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_TITLE ? View.VISIBLE : View.GONE);
            title.setText(titleValue);
        }
        if (description != null) {
            description.setVisibility(app.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DESCRIPTION ? View.VISIBLE : View.GONE);
            description.setText(descriptionValue);
        }

        if (channelName != null) {
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
        }

        if (priority != null) {
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
        }

        // Set the start and stop times and dates. Get the correct values from
        // the calendar object and add the listener that will show the required
        // date or time pickers. 
        if (startTime != null) {
            setTimeStringFromDate(startTime, startValue);
            startTime.setOnClickListener(this);
        }
        if (startDate != null) {
            setDateStringFromDate(startDate, startValue);
            startDate.setOnClickListener(this);
        }
        if (stopTime != null) {
            setTimeStringFromDate(stopTime, stopValue);
            stopTime.setOnClickListener(this);
        }
        if (stopDate != null) {
            setDateStringFromDate(stopDate, stopValue);
            stopDate.setOnClickListener(this);
        }

        // Add the additional pre- and postrecording values in minutes
        if (startExtra != null) {
            startExtra.setText(String.valueOf(startExtraValue));
        }
        if (stopExtra != null) {
            stopExtra.setText(String.valueOf(stopExtraValue));
        }

        // Add the title and menu items to the toolbar
        if (toolbar != null) {
            toolbar.setTitle(rec != null ? R.string.edit_recording : R.string.add_recording);
            toolbar.inflateMenu(R.menu.save_cancel_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
        }

        // Prevent the dialog from closing if the user clicks outside of it 
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
     * an orientation change or when the recording shall be saved.
     */
    private void getValues() {
        try {
            if (startExtra != null) {
                startExtraValue = Long.valueOf(startExtra.getText().toString());
            }
        } catch (NumberFormatException ex) {
            startExtraValue = DEFAULT_START_EXTRA;
        }
        try {
            if (stopExtra != null) {
                stopExtraValue = Long.valueOf(stopExtra.getText().toString());
            }
        } catch (NumberFormatException ex) {
            stopExtraValue = DEFAULT_STOP_EXTRA;
        }
        if (title != null) {
            titleValue = title.getText().toString();
        }
        if (description != null) {
            descriptionValue = description.getText().toString();
        }
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

        Intent intent = new Intent(activity, HTSService.class);

        // If the recording id was provided, then an existing recording was
        // edited and not a new one was added
        if (rec != null && rec.id > 0) {
            intent.setAction(Constants.ACTION_UPDATE_DVR_ENTRY);
            intent.putExtra("id", rec.id);
        } else {
            intent.setAction(Constants.ACTION_ADD_DVR_ENTRY);

            // Add the recording profile if available and enabled
            final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
            final Profile p = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
            if (p != null 
                    && p.enabled
                    && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                    && app.isUnlocked()) {
                intent.putExtra("configName", p.name);
            }
            // The id must be passed on to the server, not the name. So go through
            // all available channels and get the id for the selected channel name.
            for (Channel c : app.getChannels()) {
                if (c.name.equals(channelName.getText().toString())) {
                    intent.putExtra("channelId", c.id);
                    break;
                }
            }
        }

        intent.putExtra("title", titleValue);
        intent.putExtra("start", startValue.getTimeInMillis() / 1000); // Pass on seconds not milliseconds
        intent.putExtra("stop", stopValue.getTimeInMillis() / 1000); // Pass on seconds not milliseconds
        intent.putExtra("startExtra", startExtraValue);
        intent.putExtra("stopExtra", stopExtraValue);
        intent.putExtra("description", descriptionValue);
        intent.putExtra("priority", priorityValue);

        // Pass on the information if a recording is already recording. When it
        // is already being recorded only the title, stop and stopExtra will be
        // accepted. All other entries will be ignored and not sent
        if (rec != null) {
            intent.putExtra("isRecording", rec.isRecording());
        }

        activity.startService(intent);

        if (getDialog() != null) {
            ((FragmentStatusInterface) activity).listDataInvalid(TAG);
            getDialog().dismiss();
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
                .content(R.string.cancel_edit_recording)
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

    @Override
    public void onClick(View v) {
        
        DatePickerDialog datePicker;
        TimePickerDialog timePicker;
        
        switch (v.getId()) {
        case R.id.start_time:
            timePicker = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(RadialPickerLayout timePicker, int hour, int minute) {
                    startValue.set(Calendar.HOUR_OF_DAY, hour);
                    startValue.set(Calendar.MINUTE, minute);
                    setTimeStringFromDate(startTime, startValue);
                }
            }, 
            startValue.get(Calendar.HOUR_OF_DAY), 
            startValue.get(Calendar.MINUTE), true, false);

            timePicker.setCloseOnSingleTapMinute(false);
            timePicker.show(getChildFragmentManager(), "");
            break;

        case R.id.start_date:
            datePicker = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                    startValue.set(Calendar.DAY_OF_MONTH, day);
                    startValue.set(Calendar.MONTH, month);
                    startValue.set(Calendar.YEAR, year);
                    setDateStringFromDate(startDate, startValue);
                }
            }, 
            startValue.get(Calendar.YEAR), 
            startValue.get(Calendar.MONTH), 
            startValue.get(Calendar.DAY_OF_MONTH), false);

            datePicker.setCloseOnSingleTapDay(false);
            datePicker.show(getChildFragmentManager(), "");
            break;

        case R.id.stop_time:
            timePicker = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(RadialPickerLayout timePicker, int hour, int minute) {
                    stopValue.set(Calendar.HOUR_OF_DAY, hour);
                    stopValue.set(Calendar.MINUTE, minute);
                    setTimeStringFromDate(stopTime, stopValue);
                }
            }, 
            stopValue.get(Calendar.HOUR_OF_DAY), 
            stopValue.get(Calendar.MINUTE), true, false);

            timePicker.setCloseOnSingleTapMinute(false);
            timePicker.show(getChildFragmentManager(), "");
            break;

        case R.id.stop_date:
            datePicker = DatePickerDialog.newInstance(new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                    stopValue.set(Calendar.DAY_OF_MONTH, day);
                    stopValue.set(Calendar.MONTH, month);
                    stopValue.set(Calendar.YEAR, year);
                    setDateStringFromDate(stopDate, stopValue);
                }
            }, 
            stopValue.get(Calendar.YEAR), 
            stopValue.get(Calendar.MONTH), 
            stopValue.get(Calendar.DAY_OF_MONTH), false);

            datePicker.setCloseOnSingleTapDay(false);
            datePicker.show(getChildFragmentManager(), "");
            break;
        }
    }

    /**
     * 
     * @param v
     * @param cal
     */
    private void setDateStringFromDate(TextView v, Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        v.setText(((day < 10) ? "0" + day : day) + "."
                + ((month < 10) ? "0" + month : month) + "." + year);
    }

    /**
     * 
     * @param v
     * @param cal
     */
    private void setTimeStringFromDate(TextView v, Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        v.setText(((hour < 10) ? "0" + hour : hour) + ":"
                + ((minute < 10) ? "0" + minute : minute));
    }
}
