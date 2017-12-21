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
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.TimerRecording;
import org.tvheadend.tvhclient.utils.MenuChannelSelectionCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class TimerRecordingAddFragment extends Fragment implements HTSListener, MenuChannelSelectionCallback {

    @BindView(R.id.is_enabled)
    CheckBox isEnabledCheckbox;
    @BindView(R.id.priority)
    TextView priorityTextView;
    @BindView(R.id.days_of_week)
    TextView daysOfWeekTextView;
    @BindView(R.id.start_time)
    TextView startTimeTextView;
    @BindView(R.id.stop_time)
    TextView stopTimeTextView;
    @BindView(R.id.directory)
    EditText directoryEditText;
    @BindView(R.id.directory_label)
    TextView directoryLabelTextView;
    @BindView(R.id.title)
    EditText titleEditText;
    @BindView(R.id.name)
    EditText nameEditText;
    @BindView(R.id.channel)
    TextView channelNameTextView;
    @BindView(R.id.dvr_config)
    TextView recordingProfileNameTextView;
    @BindView(R.id.dvr_config_label)
    TextView recordingProfileLabelTextView;

    private int priority;
    private int startTime;
    private int stopTime;
    private int daysOfWeek;
    private String directory;
    private String title;
    private String name;
    private boolean isEnabled;
    private int channelId;
    private int recordingProfileName;

    private String[] priorityList;
    private String[] recordingProfilesList;
    private String[] daysOfWeekList;

    private Activity activity;
    private ToolbarInterfaceLight toolbarInterface;
    private DataStorage dataStorage;
    private Unbinder unbinder;
    private MenuUtils menuUtils;
    private int htspVersion;
    private String id;
    private Profile profile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.timer_recording_add_layout, container, false);
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
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        Connection connection = databaseHelper.getSelectedConnection();
        profile = databaseHelper.getProfile(connection.recording_profile_id);
        setHasOptionsMenu(true);

        // Determine if the server supports recording on all channels
        boolean allowRecordingOnAllChannels = htspVersion >= 21;

        // Create the list of available configurations that the user can select from
        recordingProfilesList = new String[dataStorage.getDvrConfigs().size()];
        for (int i = 0; i < dataStorage.getDvrConfigs().size(); i++) {
            recordingProfilesList[i] = dataStorage.getDvrConfigs().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);
        daysOfWeekList = activity.getResources().getStringArray(R.array.day_short_names);

        Bundle bundle = getArguments();
        if (bundle != null) {
            id = bundle.getString("id");
        }

        // Get the values from the recording otherwise use default values
        Channel channel;
        if (!TextUtils.isEmpty(id)) {
            TimerRecording recording = dataStorage.getTimerRecordingFromArray(id);
            priority = recording.priority;
            startTime = recording.start;
            stopTime = recording.stop;
            daysOfWeek = recording.daysOfWeek;
            directory = recording.directory;
            title = recording.title;
            name = recording.name;
            isEnabled = (recording.enabled > 0);
            channelId = recording.channel;
            channel = dataStorage.getChannelFromArray(channelId);
        } else {
            Calendar cal = Calendar.getInstance();
            int currentTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            priority = 2;
            startTime = currentTime;
            stopTime = currentTime + 30;
            daysOfWeek = 127;
            directory = "";
            title = "";
            name = "";
            isEnabled = true;
            channelId = 0;
            channel = null;
        }

        toolbarInterface.setTitle(!TextUtils.isEmpty(id) ?
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
            isEnabled = savedInstanceState.getBoolean("isEnabled");
            title = savedInstanceState.getString("title");
            name = savedInstanceState.getString("name");
            channelId = savedInstanceState.getInt("channelId");
            startTime = savedInstanceState.getInt("startTime");
            stopTime = savedInstanceState.getInt("stopTime");
            daysOfWeek = savedInstanceState.getInt("daysOfWeek");
            priority = savedInstanceState.getInt("priority");
            directory = savedInstanceState.getString("directory");
            recordingProfileName = savedInstanceState.getInt("configName");
        }

        isEnabledCheckbox.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        isEnabledCheckbox.setChecked(isEnabled);
        titleEditText.setText(title);
        nameEditText.setText(name);
        directoryLabelTextView.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setVisibility(htspVersion >= 19 ? View.VISIBLE : View.GONE);
        directoryEditText.setText(directory);

        channelNameTextView.setText(channel != null ? channel.channelName : getString(R.string.all_channels));
        channelNameTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                menuUtils.handleMenuChannelSelection(channelId, TimerRecordingAddFragment.this, allowRecordingOnAllChannels);
            }
        });

        priorityTextView.setText(priorityList[priority]);
        priorityTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handlePrioritySelection();
            }
        });

        if (TextUtils.isEmpty(id) || recordingProfilesList.length == 0) {
            recordingProfileNameTextView.setVisibility(View.GONE);
            recordingProfileLabelTextView.setVisibility(View.GONE);
        } else {
            recordingProfileNameTextView.setVisibility(View.VISIBLE);
            recordingProfileLabelTextView.setVisibility(View.VISIBLE);
        }

        recordingProfileNameTextView.setText(recordingProfilesList[recordingProfileName]);
        recordingProfileNameTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleRecordingProfileSelection();
            }
        });

        startTimeTextView.setText(Utils.getTimeStringFromValue(activity, startTime));
        startTimeTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleStartTimeSelection();
            }
        });

        stopTimeTextView.setText(Utils.getTimeStringFromValue(activity, stopTime));
        stopTimeTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleStopTimeSelection();
            }
        });

        showSelectedDaysOfWeek();
        daysOfWeekTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                handleDayOfWeekSelection();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        saveWidgetValuesIntoVariables();
        outState.putBoolean("isEnabled", isEnabled);
        outState.putString("title", title);
        outState.putString("name", name);
        outState.putInt("channelId", channelId);
        outState.putInt("startTime", startTime);
        outState.putInt("stopTime", stopTime);
        outState.putInt("daysOfWeek", daysOfWeek);
        outState.putInt("priority", priority);
        outState.putString("directory", directory);
        outState.putInt("configName", recordingProfileName);
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

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
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

    private void handleDayOfWeekSelection() {
        // Get the selected indices by storing the bits with 1 positions in a list
        // This list then needs to be converted to an Integer[] because the
        // material dialog requires this
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Integer value = (daysOfWeek >> i) & 1;
            if (value == 1) {
                list.add(i);
            }
        }
        Integer[] selectedIndices = new Integer[list.size()];
        for (int i = 0; i < selectedIndices.length; i++) {
            selectedIndices[i] = list.get(i);
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.title)
                // TODO long names
                .items(R.array.day_short_names)
                .itemsCallbackMultiChoice(selectedIndices, new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        daysOfWeek = 0;
                        for (Integer i : which) {
                            daysOfWeek += (1 << i);
                        }
                        showSelectedDaysOfWeek();
                        return true;
                    }
                })
                .positiveText(R.string.select)
                .show();
    }

    private void handleStopTimeSelection() {
        int hour = stopTime / 60;
        int minute = stopTime % 60;
        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                // Save the given value in seconds. This values will be passed to the server
                stopTime = (selectedHour * 60 + selectedMinute);
                stopTimeTextView.setText(Utils.getTimeStringFromValue(activity, stopTime));
            }
        }, hour, minute, true, false);

        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(getChildFragmentManager(), "");
    }

    private void handleStartTimeSelection() {
        int hour = startTime / 60;
        int minute = startTime % 60;
        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                // Save the given value in seconds. This values will be passed to the server
                startTime = (selectedHour * 60 + selectedMinute);
                startTimeTextView.setText(Utils.getTimeStringFromValue(activity, startTime));
            }
        }, hour, minute, true, false);
        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(getChildFragmentManager(), "");
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

    private void showSelectedDaysOfWeek() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            String s = (((daysOfWeek >> i) & 1) == 1) ? daysOfWeekList[i] : "";
            if (text.length() > 0 && s.length() > 0) {
                text.append(", ");
            }
            text.append(s);
        }
        daysOfWeekTextView.setText(text.toString());
    }

    private void saveWidgetValuesIntoVariables() {
        directory = directoryEditText.getText().toString();
        title = titleEditText.getText().toString();
        name = nameEditText.getText().toString();
        isEnabled = isEnabledCheckbox.isChecked();
    }

    private void save() {
        saveWidgetValuesIntoVariables();

        if (TextUtils.isEmpty(title)) {
            if (activity.getCurrentFocus() != null) {
                Snackbar.make(activity.getCurrentFocus(), getString(R.string.error_empty_title), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (!TextUtils.isEmpty(id)) {
            updateTimerRecording();
        } else {
            addTimerRecording();
        }
    }

    private void cancel() {
        // Show confirmation dialog to cancel
        new MaterialDialog.Builder(activity)
                .content(R.string.cancel_add_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        getActivity().finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    @Override
    public void onMessage(String action, Object obj) {
        if (action.equals("timerecEntryDelete")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    TimerRecording timerRecording = (TimerRecording) obj;
                    if (timerRecording.id.equals(id)) {
                        addTimerRecording();
                    }
                    activity.finish();
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
        Intent intent = getIntentData();
        intent.setAction("addTimerecEntry");
        activity.startService(intent);
    }

    /**
     * Updates the timer recording with the given values.
     * If the API version supports it, use the native service call method
     * otherwise the old recording is removed and a new one with the
     * edited values is added afterwards
     */
    private void updateTimerRecording() {
        if (htspVersion >= 25) {
            // If the API version supports it, use the native service call method
            Intent intent = getIntentData();
            intent.setAction("updateTimerecEntry");
            intent.putExtra("id", id);
            activity.startService(intent);
            activity.finish();
        } else {
            Intent intent = new Intent(activity, HTSService.class);
            intent.setAction("deleteTimerecEntry");
            intent.putExtra("id", id);
            activity.startService(intent);
        }
    }

    /**
     * Returns an intent with the recording data
     */
    private Intent getIntentData() {
        Intent intent = new Intent(activity, HTSService.class);
        intent.putExtra("directory", directory);
        intent.putExtra("title", title);
        intent.putExtra("name", name);
        intent.putExtra("start", startTime);
        intent.putExtra("stop", stopTime);
        intent.putExtra("daysOfWeek", daysOfWeek);
        intent.putExtra("priority", priority);
        intent.putExtra("enabled", (isEnabled ? 1 : 0));

        if (channelId > 0) {
            intent.putExtra("channelId", channelId);
        }

        // Add the recording profile if available and enabled
        if (profile != null && profile.enabled
                && (recordingProfileNameTextView.getText().length() > 0)
                && htspVersion >= 16) {
            // Use the selected profile. If no change was done in the 
            // selection then the default one from the connection setting will be used
            intent.putExtra("configName", recordingProfileNameTextView.getText().toString());
        }
        return intent;
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
