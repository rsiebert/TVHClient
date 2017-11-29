package org.tvheadend.tvhclient.fragments.recordings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;

public class RecordingAddFragment extends DialogFragment implements OnClickListener {

    private final static String TAG = RecordingAddFragment.class.getSimpleName();

    private AppCompatActivity activity;
    private Recording rec;
    private Toolbar toolbar;

    private TextView startTime;
    private TextView stopTime;
    private TextView startDate;
    private TextView stopDate;
    private CheckBox isEnabled;
    private TextView priority;
    private EditText startExtra;
    private EditText stopExtra;
    private EditText title;
    private TextView titelLabel;
    private EditText subtitle;
    private TextView subtitleLabel;
    private EditText description;
    private TextView descriptionLabel;
    private TextView channelName;
    private TextView dvrConfigName;
    private TextView dvrConfigNameLabel;

    // Extra pre- and postrecording times in seconds
    private long startExtraValue;
    private long stopExtraValue;

    // Start and end recording times. They are saved in the calendar object and
    // will be converted to milliseconds when saved during an orientation change
    // and converted to seconds before passed to the server.
    private final Calendar startValue = Calendar.getInstance();
    private final Calendar stopValue = Calendar.getInstance();

    private long priorityValue;
    private String titleValue;
    private String subtitleValue;
    private String descriptionValue;
    private int channelSelectionValue;
    private int dvrConfigNameValue;
    private boolean enabledValue;

    private String[] channelList;
    private String[] priorityList;
    private String[] dvrConfigList;

    private TVHClientApplication app;
    private DatabaseHelper databaseHelper;

    private static final int DEFAULT_START_EXTRA = 2;
    private static final int DEFAULT_STOP_EXTRA = 2;
    private Logger logger;
    private DataStorage dataStorage;

    public static RecordingAddFragment newInstance() {
        return new RecordingAddFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getDialog() != null && getDialog().getWindow() != null) {
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
        outState.putString("subtitleValue", subtitleValue);
        outState.putString("descriptionValue", descriptionValue);
        outState.putInt("channelNameValue", channelSelectionValue);
        outState.putInt("configNameValue", dvrConfigNameValue);
        outState.putBoolean("enabledValue", enabledValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

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
        isEnabled = (CheckBox) v.findViewById(R.id.is_enabled);
        title = (EditText) v.findViewById(R.id.title);
        titelLabel = (TextView) v.findViewById(R.id.title_label);
        subtitle = (EditText) v.findViewById(R.id.subtitle);
        subtitleLabel = (TextView) v.findViewById(R.id.subtitle_label);
        description = (EditText) v.findViewById(R.id.description);
        descriptionLabel = (TextView) v.findViewById(R.id.description_label);
        channelName = (TextView) v.findViewById(R.id.channel);
        startExtra = (EditText) v.findViewById(R.id.start_extra);
        stopExtra = (EditText) v.findViewById(R.id.stop_extra);
        startTime = (TextView) v.findViewById(R.id.start_time);
        stopTime = (TextView) v.findViewById(R.id.stop_time);
        startDate = (TextView) v.findViewById(R.id.start_date);
        stopDate = (TextView) v.findViewById(R.id.stop_date);
        priority = (TextView) v.findViewById(R.id.priority);
        dvrConfigName = (TextView) v.findViewById(R.id.dvr_config);
        dvrConfigNameLabel = (TextView) v.findViewById(R.id.dvr_config_label);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        app = TVHClientApplication.getInstance();
        databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        logger = Logger.getInstance();
        dataStorage = DataStorage.getInstance();

        // Create the list of channels that the user can select
        channelList = new String[dataStorage.getChannels().size()];
        for (int i = 0; i < dataStorage.getChannels().size(); i++) {
            channelList[i] = dataStorage.getChannels().get(i).name;
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

        // Create the list of available configurations that the user can select from
        dvrConfigList = new String[dataStorage.getDvrConfigs().size()];
        for (int i = 0; i < dataStorage.getDvrConfigs().size(); i++) {
            dvrConfigList[i] = dataStorage.getDvrConfigs().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);

        // If the savedInstanceState is null then the fragment was created for
        // the first time. Either get the given id to edit the recording or
        // create new one. Otherwise an orientation change has occurred and the
        // saved values must be applied to the user input elements.
        if (savedInstanceState == null) {
            long recId = 0;
            Bundle bundle = getArguments();
            if (bundle != null) {
                recId = bundle.getLong("dvrId");
            }

            // Get the recording so we can show its detail
            rec = dataStorage.getRecording(recId);
            if (rec != null) {
                priorityValue = rec.priority;
                startExtraValue = rec.startExtra;
                stopExtraValue = rec.stopExtra;
                startValue.setTimeInMillis(rec.start.getTime());
                stopValue.setTimeInMillis(rec.stop.getTime());
                titleValue = rec.title;
                subtitleValue = rec.subtitle;
                descriptionValue = rec.description;
                enabledValue = rec.enabled;

                // The default value is the first one
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
                priorityValue = 2;
                startExtraValue = 0;
                stopExtraValue = 0;
                stopValue.setTimeInMillis(startValue.getTimeInMillis() + 30 * 60000); // add 30 min
                titleValue = "";
                subtitleValue = "";
                descriptionValue = "";
                channelSelectionValue = 0;
                enabledValue = true;
            }

            // Get the position of the selected profile in the dvrConfigList
            dvrConfigNameValue = 0;
            final Connection conn = databaseHelper.getSelectedConnection();
            final Profile p = databaseHelper.getProfile(conn.recording_profile_id);
            if (p != null) {
                for (int i = 0; i < dvrConfigList.length; i++) {
                    if (dvrConfigList[i].equals(p.name)) {
                        dvrConfigNameValue = i;
                        break;
                    }
                }
            }

        } else {
            // Restore the values before the orientation change
            priorityValue = savedInstanceState.getLong("priorityValue");
            startExtraValue = savedInstanceState.getLong("startExtraValue");
            stopExtraValue = savedInstanceState.getLong("stopExtraValue");
            startValue.setTimeInMillis(savedInstanceState.getLong("startTimeValue"));
            stopValue.setTimeInMillis(savedInstanceState.getLong("stopTimeValue"));
            titleValue = savedInstanceState.getString("titleValue");
            subtitleValue = savedInstanceState.getString("subtitleValue");
            descriptionValue = savedInstanceState.getString("descriptionValue");
            channelSelectionValue = savedInstanceState.getInt("channelNameValue");
            dvrConfigNameValue = savedInstanceState.getInt("configNameValue");
            enabledValue = savedInstanceState.getBoolean("enabledValue");
        }

        if (isEnabled != null) {
            isEnabled.setChecked(enabledValue);
            isEnabled.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_DVR_FIELD_ENABLED ? View.VISIBLE : View.GONE);
        }
        if (title != null && titelLabel != null) {
            title.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_TITLE ? View.VISIBLE : View.GONE);
            titelLabel.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_TITLE ? View.VISIBLE : View.GONE);
            title.setText(titleValue);
        }
        if (subtitle != null && subtitleLabel != null) {
            subtitle.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_SUBTITLE ? View.VISIBLE : View.GONE);
            subtitleLabel.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_SUBTITLE ? View.VISIBLE : View.GONE);
            subtitle.setText(subtitleValue);
        }
        if (description != null && descriptionLabel != null) {
            description.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DESCRIPTION ? View.VISIBLE : View.GONE);
            descriptionLabel.setVisibility(dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_DESCRIPTION ? View.VISIBLE : View.GONE);
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
            if (priorityValue < priorityList.length) {
                priority.setText(priorityList[(int) priorityValue]);
            } else {
                logger.log(TAG, "Priority value '"
                        + priorityValue + "' is larger then priority array size of '"
                        + priorityList.length + "'. Using default of 2");
                priority.setText(priorityList[2]);
            }
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

        if (dvrConfigName != null && dvrConfigNameLabel != null) {
            if ((rec != null && rec.id > 0) || dvrConfigList.length == 0) {
                dvrConfigName.setText("");
                dvrConfigName.setVisibility(View.GONE);
                dvrConfigNameLabel.setVisibility(View.GONE);
            } else {
                dvrConfigName.setVisibility(View.VISIBLE);
                dvrConfigNameLabel.setVisibility(View.VISIBLE);
                dvrConfigName.setText(dvrConfigList[dvrConfigNameValue]);
                dvrConfigName.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new MaterialDialog.Builder(activity)
                        .title(R.string.select_dvr_config)
                        .items(dvrConfigList)
                        .itemsCallbackSingleChoice(dvrConfigNameValue, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                dvrConfigName.setText(dvrConfigList[which]);
                                dvrConfigNameValue = which;
                                return true;
                            }
                        })
                        .show();
                    }
                });
            }
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
     * Called when the user has selected a menu item in the toolbar
     *
     * @param item Selected menu item
     * @return True if selection was handled, otherwise false
     */
    private boolean onToolbarItemSelected(MenuItem item) {
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
            if (startExtra != null && startExtra.getText() != null) {
                startExtraValue = Long.valueOf(startExtra.getText().toString());
            } else {
                startExtraValue = DEFAULT_START_EXTRA;
            }
        } catch (NumberFormatException ex) {
            startExtraValue = DEFAULT_START_EXTRA;
        }
        try {
            if (stopExtra != null && stopExtra.getText() != null) {
                stopExtraValue = Long.valueOf(stopExtra.getText().toString());
            } else {
                stopExtraValue = DEFAULT_STOP_EXTRA;
            }
        } catch (NumberFormatException ex) {
            stopExtraValue = DEFAULT_STOP_EXTRA;
        }
        if (title != null && title.getText() != null) {
            titleValue = title.getText().toString();
        }
        if (subtitle != null && subtitle.getText() != null) {
            subtitleValue = subtitle.getText().toString();
        }
        if (description != null && description.getText() != null) {
            descriptionValue = description.getText().toString();
        }
        if (isEnabled != null) {
            enabledValue = isEnabled.isChecked();
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
        if (titleValue.length() == 0 && dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_REC_FIELD_TITLE) {
            Toast.makeText(activity, getString(R.string.error_empty_title),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(activity, HTSService.class);

        // If the recording id was provided, then an existing recording was
        // edited and not a new one was added
        if (rec != null && rec.id > 0) {
            intent.setAction("updateDvrEntry");
            intent.putExtra("id", rec.id);
        } else {
            intent.setAction("addDvrEntry");

            // Add the recording profile if available and enabled
            final Connection conn = databaseHelper.getSelectedConnection();
            final Profile p = databaseHelper.getProfile(conn.recording_profile_id);
            if (p != null 
                    && p.enabled
                    && (dvrConfigName.getText().length() > 0) 
                    && dataStorage.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                    && app.isUnlocked()) {
                // Use the selected profile. If no change was done in the 
                // selection then the default one from the connection setting will be used
                intent.putExtra("configName", dvrConfigName.getText().toString());
            }
        }

        // The id must be passed on to the server, not the name. So go through
        // all available channels and get the id for the selected channel name.
        for (Channel c : dataStorage.getChannels()) {
            if (c.name.equals(channelList[channelSelectionValue])) {
                intent.putExtra("channelId", c.id);
                break;
            }
        }

        intent.putExtra("title", titleValue);
        intent.putExtra("subtitle", subtitleValue);
        intent.putExtra("start", startValue.getTimeInMillis() / 1000); // Pass on seconds not milliseconds
        intent.putExtra("stop", stopValue.getTimeInMillis() / 1000); // Pass on seconds not milliseconds
        intent.putExtra("startExtra", startExtraValue);
        intent.putExtra("stopExtra", stopExtraValue);
        intent.putExtra("description", descriptionValue);
        intent.putExtra("priority", priorityValue);
        intent.putExtra("enabled", (long) (enabledValue ? 1 : 0));

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
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (getDialog() != null) {
                            getDialog().dismiss();
                        }
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
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
     * @param v TextView that shall display the datevalue
     * @param cal Calendar
     */
    private void setDateStringFromDate(TextView v, Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        String text = ((day < 10) ? "0" + day : day) + "."
                + ((month < 10) ? "0" + month : month) + "." + year;
        v.setText(text);
    }

    /**
     * 
     * @param v TextView that shall display the time value
     * @param cal Calendar
     */
    private void setTimeStringFromDate(TextView v, Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        String text = ((hour < 10) ? "0" + hour : hour) + ":"
                + ((minute < 10) ? "0" + minute : minute);
        v.setText(text);
    }
}
