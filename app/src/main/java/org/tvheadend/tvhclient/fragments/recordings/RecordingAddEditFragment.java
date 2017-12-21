package org.tvheadend.tvhclient.fragments.recordings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuChannelSelectionCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

// TODO hide show layout stuff if editing scheduled or recording recordings
// TODO updated getIntentData according to the previous todo
// TODO preselect the first channel in the list
// TODO extend from BaseRecordingAddEditFragment

public class RecordingAddEditFragment extends Fragment implements MenuChannelSelectionCallback {

    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.stop_time)
    TextView stopTimeTextView;
    @BindView(R.id.start_date)
    TextView startDateTextView;
    @BindView(R.id.stop_date)
    TextView stopDateTextView;
    @BindView(R.id.is_enabled)
    CheckBox isEnabledCheckbox;
    @BindView(R.id.priority)
    TextView priorityTextView;
    @BindView(R.id.start_extra)
    EditText startExtraEditText;
    @BindView(R.id.stop_extra)
    EditText stopExtraEditText;
    @BindView(R.id.title)
    EditText titleEditText;
    @BindView(R.id.title_label)
    TextView titleLabelTextView;
    @BindView(R.id.subtitle)
    EditText subtitleEditText;
    @BindView(R.id.subtitle_label)
    TextView subtitleLabelTextView;
    @BindView(R.id.description)
    EditText descriptionEditText;
    @BindView(R.id.description_label)
    TextView descriptionLabelTextView;
    @BindView(R.id.channel)
    TextView channelNameTextView;
    @BindView(R.id.dvr_config)
    TextView recordingProfileNameTextView;
    @BindView(R.id.dvr_config_label)
    TextView recordingProfileLabelTextView;

    private int priority;
    private String title;
    private String subtitle;
    private String description;
    private Calendar startTime = Calendar.getInstance();
    private Calendar stopTime = Calendar.getInstance();
    private long startExtra;
    private long stopExtra;
    private boolean isEnabled;
    private int channelId;
    private int recordingProfileName;

    private String[] priorityList;
    private String[] recordingProfilesList;

    private Activity activity;
    private ToolbarInterfaceLight toolbarInterface;
    private DataStorage dataStorage;
    private Unbinder unbinder;
    private MenuUtils menuUtils;
    private int htspVersion;
    private boolean isUnlocked;
    private Profile profile;
    private int dvrId = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.recording_add_layout, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        if (activity instanceof ToolbarInterfaceLight) {
            toolbarInterface = (ToolbarInterfaceLight) activity;
        }

        menuUtils = new MenuUtils(getActivity());
        dataStorage = DataStorage.getInstance();
        htspVersion = dataStorage.getProtocolVersion();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        Connection connection = databaseHelper.getSelectedConnection();
        profile = databaseHelper.getProfile(connection.recording_profile_id);
        setHasOptionsMenu(true);

        // Create the list of available configurations that the user can select from
        recordingProfilesList = new String[dataStorage.getDvrConfigs().size()];
        for (int i = 0; i < dataStorage.getDvrConfigs().size(); i++) {
            recordingProfilesList[i] = dataStorage.getDvrConfigs().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);

        Bundle bundle = getArguments();
        if (bundle != null) {
            dvrId = bundle.getInt("dvrId");
        }

        // Get the values from the recording otherwise use default values
        Channel channel;
        if (dvrId > 0) {
            Recording recording = dataStorage.getRecordingFromArray(dvrId);
            priority = recording.priority;
            startExtra = recording.startExtra;
            stopExtra = recording.stopExtra;
            startTime.setTimeInMillis(recording.start);
            stopTime.setTimeInMillis(recording.stop);
            title = recording.title;
            subtitle = recording.subtitle;
            description = recording.description;
            isEnabled = recording.enabled > 0;
            channelId = recording.channel;
            channel = dataStorage.getChannelFromArray(channelId);
        } else {
            priority = 2;
            startExtra = 0;
            stopExtra = 0;
            // The start time is already set during init
            // From this time add 30 minutes to the stop time
            stopTime.setTimeInMillis(startTime.getTimeInMillis() + 30 * 60 * 1000);
            title = "";
            subtitle = "";
            description = "";
            isEnabled = true;
            channelId = 0;
            channel = null;
        }

        toolbarInterface.setTitle(dvrId > 0 ?
                getString(R.string.edit_recording) :
                getString(R.string.add_recording));

        // Get the selected profile from the connection
        // and select it from the recording config list
        recordingProfileName = 0;
        if (profile != null) {
            for (int i = 0; i < recordingProfilesList.length; i++) {
                if (recordingProfilesList[i].equals(profile.name)) {
                    recordingProfileName = i;
                    break;
                }
            }
        }

        // Restore the values before the orientation change
        if (savedInstanceState != null) {
            priority = savedInstanceState.getInt("priority");
            startExtra = savedInstanceState.getLong("startExtra");
            stopExtra = savedInstanceState.getLong("stopExtra");
            startTime.setTimeInMillis(savedInstanceState.getLong("startTime"));
            stopTime.setTimeInMillis(savedInstanceState.getLong("stopTime"));
            title = savedInstanceState.getString("title");
            subtitle = savedInstanceState.getString("subtitle");
            description = savedInstanceState.getString("description");
            channelId = savedInstanceState.getInt("channelId");
            recordingProfileName = savedInstanceState.getInt("configName");
            isEnabled = savedInstanceState.getBoolean("isEnabled");
        }

        isEnabledCheckbox.setVisibility(htspVersion >= 23 ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(isEnabled);

        titleLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        titleEditText.setText(title);
        subtitleLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        subtitleEditText.setText(subtitle);
        descriptionLabelTextView.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setVisibility(htspVersion >= 21 ? View.VISIBLE : View.GONE);
        descriptionEditText.setText(description);

        channelNameTextView.setText(channel != null ? channel.channelName : getString(R.string.no_channel));
        channelNameTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                menuUtils.handleMenuChannelSelection(channelId, RecordingAddEditFragment.this, false);
            }
        });

        priorityTextView.setText(priorityList[priority]);
        priorityTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handlePrioritySelection();
            }
        });

        recordingProfileNameTextView.setText(recordingProfilesList[recordingProfileName]);
        recordingProfileNameTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleRecordingProfileSelection();
            }
        });

        startTimeTextView.setText(getTimeStringFromDate(startTime));
        startTimeTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleStartTimeSelection();
            }
        });

        stopTimeTextView.setText(getTimeStringFromDate(stopTime));
        stopTimeTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleStopTimeSelection();
            }
        });

        startDateTextView.setText(getDateStringFromDate(startTime));
        startDateTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleStartDateSelection();
            }
        });

        stopDateTextView.setText(getDateStringFromDate(stopTime));
        stopDateTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleStopDateSelection();
            }
        });

        // Add the additional pre- and post recording values in minutes
        startExtraEditText.setText(String.valueOf(startExtra));
        stopExtraEditText.setText(String.valueOf(stopExtra));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        saveWidgetValuesIntoVariables();
        outState.putInt("priority", priority);
        outState.putLong("startTime", startTime.getTimeInMillis());
        outState.putLong("stopTime", stopTime.getTimeInMillis());
        outState.putLong("startExtra", startExtra);
        outState.putLong("stopExtra", stopExtra);
        outState.putString("title", title);
        outState.putString("subtitle", subtitle);
        outState.putString("description", description);
        outState.putInt("channelId", channelId);
        outState.putInt("configName", recordingProfileName);
        outState.putBoolean("isEnabled", isEnabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                cancel();
                return true;
            case R.id.menu_save:
                save();
                return true;
            case R.id.menu_cancel:
                cancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleStartDateSelection() {
        int year = startTime.get(Calendar.YEAR);
        int month = startTime.get(Calendar.MONTH);
        int day = startTime.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePicker = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                        startTime.set(Calendar.DAY_OF_MONTH, day);
                        startTime.set(Calendar.MONTH, month);
                        startTime.set(Calendar.YEAR, year);
                        startDateTextView.setText(getDateStringFromDate(startTime));
                    }
                }, year, month, day, false);

        datePicker.setCloseOnSingleTapDay(false);
        datePicker.show(getChildFragmentManager(), "");
    }

    private void handleStopDateSelection() {
        int year = stopTime.get(Calendar.YEAR);
        int month = stopTime.get(Calendar.MONTH);
        int day = stopTime.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePicker = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                        stopTime.set(Calendar.DAY_OF_MONTH, day);
                        stopTime.set(Calendar.MONTH, month);
                        stopTime.set(Calendar.YEAR, year);
                        stopDateTextView.setText(getDateStringFromDate(stopTime));
                    }
                }, year, month, day, false);

        datePicker.setCloseOnSingleTapDay(false);
        datePicker.show(getChildFragmentManager(), "");
    }

    private void handleStopTimeSelection() {
        int hour = stopTime.get(Calendar.HOUR_OF_DAY);
        int minute = stopTime.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                        stopTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                        stopTime.set(Calendar.MINUTE, selectedMinute);
                        stopTimeTextView.setText(getTimeStringFromDate(stopTime));
                    }
                }, hour, minute, true, false);

        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(getChildFragmentManager(), "");
    }

    private void handleStartTimeSelection() {
        int hour = startTime.get(Calendar.HOUR_OF_DAY);
        int minute = startTime.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                        // Save the given value in seconds. This values will be passed to the server
                        startTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                        startTime.set(Calendar.MINUTE, selectedMinute);
                        startTimeTextView.setText(getTimeStringFromDate(startTime));
                    }
                }, hour, minute, true, false);

        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(getChildFragmentManager(), "");
    }

    private void handleRecordingProfileSelection() {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_dvr_config)
                .items(recordingProfilesList)
                .itemsCallbackSingleChoice(recordingProfileName, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        recordingProfileNameTextView.setText(recordingProfilesList[which]);
                        recordingProfileName = which;
                        return true;
                    }
                })
                .show();
    }

    private void handlePrioritySelection() {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_priority)
                .items(priorityList)
                .itemsCallbackSingleChoice(priority, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        priorityTextView.setText(priorityList[which]);
                        priority = which;
                        return true;
                    }
                })
                .show();
    }

    /**
     * Retrieves and checks the values from the user input elements and stores
     * them in internal variables. These are used to remember the values during
     * an orientation change or when the recording shall be saved.
     */
    private void saveWidgetValuesIntoVariables() {
        title = titleEditText.getText().toString();
        subtitle = subtitleEditText.getText().toString();
        description = descriptionEditText.getText().toString();
        isEnabled = isEnabledCheckbox.isChecked();
        try {
            startExtra = Long.valueOf(startExtraEditText.getText().toString());
        } catch (NumberFormatException ex) {
            startExtra = 2;
        }
        try {
            stopExtra = Long.valueOf(stopExtraEditText.getText().toString());
        } catch (NumberFormatException ex) {
            stopExtra = 2;
        }
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        saveWidgetValuesIntoVariables();

        if (TextUtils.isEmpty(title) && htspVersion >= 21) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), getString(R.string.error_empty_title), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (dvrId > 0) {
            updateRecording();
        } else {
            addRecording();
        }
    }

    private void addRecording() {
        Intent intent = getIntentData();
        intent.setAction("addDvrEntry");
        activity.startService(intent);
    }

    private void updateRecording() {
        Intent intent = getIntentData();
        intent.setAction("updateDvrEntry");
        intent.putExtra("id", dvrId);
        activity.startService(intent);
    }

    private Intent getIntentData() {
        Intent intent = new Intent(activity, HTSService.class);

        if (dvrId == 0) {
            intent.putExtra("title", title);
            intent.putExtra("subtitle", subtitle);
            intent.putExtra("startTime", startTime); // Pass on seconds not milliseconds
            intent.putExtra("stopTime", stopTime); // Pass on seconds not milliseconds
            intent.putExtra("startExtra", startExtra);
            intent.putExtra("stopExtra", stopExtra);
            intent.putExtra("description", description);
            intent.putExtra("priority", priority);
            intent.putExtra("channelId", channelId);
            intent.putExtra("enabled", (isEnabled ? 1 : 0));

            // Add the recording profile if available and enabled
            if (profile != null && profile.enabled
                    && (recordingProfileNameTextView.getText().length() > 0)
                    && htspVersion >= 16
                    && isUnlocked) {
                // Use the selected profile. If no change was done in the
                // selection then the default one from the connection setting will be used
                intent.putExtra("configName", recordingProfileNameTextView.getText().toString());
            }
        }
        return intent;
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
                        activity.finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.cancel();
                    }
                }).show();
    }

    private String getDateStringFromDate(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        String text = ((day < 10) ? "0" + day : day) + "."
                + ((month < 10) ? "0" + month : month) + "." + year;
        return text;
    }

    private String getTimeStringFromDate(Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        String text = ((hour < 10) ? "0" + hour : hour) + ":"
                + ((minute < 10) ? "0" + minute : minute);
        return text;
    }

    @Override
    public void menuChannelSelected(int which) {
        if (which > 0) {
            channelId = which;
            Channel channel = dataStorage.getChannelFromArray(which);
            channelNameTextView.setText(channel.channelName);
        } else {
            channelNameTextView.setText(R.string.all_channels);
        }
    }
}
