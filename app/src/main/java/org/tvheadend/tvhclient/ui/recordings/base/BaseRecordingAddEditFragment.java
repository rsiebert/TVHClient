package org.tvheadend.tvhclient.ui.recordings.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.model.Profile;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.channels.ChannelListSelectionAdapter;
import org.tvheadend.tvhclient.ui.recordings.common.DateTimePickerCallback;
import org.tvheadend.tvhclient.ui.recordings.common.DaysOfWeekSelectionCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.ui.recordings.common.RecordingProfileListCallback;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.callbacks.ChannelListSelectionCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// TODO add title and full names to day of weeks list

public class BaseRecordingAddEditFragment extends Fragment {

    protected AppCompatActivity activity;
    protected ToolbarInterface toolbarInterface;
    protected MenuUtils menuUtils;
    protected DataStorage dataStorage;
    protected int htspVersion;
    protected boolean isUnlocked;
    protected Profile profile;

    protected int priority;
    protected int daysOfWeek;
    protected int recordingProfileName;
    protected String[] daysOfWeekList;
    protected String[] priorityList;
    protected String[] recordingProfilesList;
    protected DatabaseHelper databaseHelper;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        menuUtils = new MenuUtils(getActivity());
        dataStorage = DataStorage.getInstance();
        htspVersion = dataStorage.getProtocolVersion();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        databaseHelper = DatabaseHelper.getInstance(activity.getApplicationContext());
        Connection connection = databaseHelper.getSelectedConnection();
        profile = databaseHelper.getProfile(connection.recording_profile_id);
        setHasOptionsMenu(true);

        daysOfWeekList = activity.getResources().getStringArray(R.array.day_short_names);

        // Create the list of available configurations that the user can select from
        recordingProfilesList = new String[dataStorage.getDvrConfigs().size()];
        for (int i = 0; i < dataStorage.getDvrConfigs().size(); i++) {
            recordingProfilesList[i] = dataStorage.getDvrConfigs().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);
    }

    protected String getDateStringFromTimeInMillis(long milliSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        return ((day < 10) ? "0" + day : day) + "."
                + ((month < 10) ? "0" + month : month) + "." + year;
    }

    protected String getTimeStringFromTimeInMillis(long milliSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        return ((hour < 10) ? "0" + hour : hour) + ":"
                + ((minute < 10) ? "0" + minute : minute);
    }

    protected String getSelectedDaysOfWeek() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            String s = (((daysOfWeek >> i) & 1) == 1) ? daysOfWeekList[i] : "";
            if (text.length() > 0 && s.length() > 0) {
                text.append(", ");
            }
            text.append(s);
        }
        return text.toString();
    }

    protected void handleDayOfWeekSelection(int daysOfWeek, DaysOfWeekSelectionCallback callback) {
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
                .title(R.string.days_of_week)
                .items(R.array.day_long_names)
                .itemsCallbackMultiChoice(selectedIndices, new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        int selectedDays = 0;
                        for (Integer i : which) {
                            selectedDays += (1 << i);
                        }
                        if (callback != null) {
                            callback.onDaysOfWeekSelected(selectedDays);
                        }
                        return true;
                    }
                })
                .positiveText(R.string.select)
                .show();
    }

    protected void handleChannelListSelection(long selectedChannelId, ChannelListSelectionCallback callback, boolean showAllChannelsListEntry) {
        // Fill the channel tag adapter with the available channel tags
        List<Channel> channelList = new ArrayList<>();
        Map<Integer, Channel> map = DataStorage.getInstance().getChannelsFromArray();
        channelList.addAll(map.values());

        // Sort the channel tag list before showing it
        Collections.sort(channelList, new Comparator<Channel>() {
            @Override
            public int compare(Channel o1, Channel o2) {
                return o1.getChannelName().compareTo(o2.getChannelName());
            }
        });

        // Add the default channel (all channels)
        // to the list after it has been sorted
        if (showAllChannelsListEntry) {
            Channel channel = new Channel();
            channel.setChannelId(0);
            channel.setChannelName(activity.getString(R.string.all_channels));
            channelList.add(0, channel);
        }

        final ChannelListSelectionAdapter channelListSelectionAdapter = new ChannelListSelectionAdapter(activity, channelList, selectedChannelId);
        // Show the dialog that shows all available channel tags. When the
        // user has selected a tag, restart the loader to loadRecording the updated channel list
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(R.string.tags)
                .adapter(channelListSelectionAdapter, null)
                .build();
        // Set the callback to handle clicks. This needs to be done after the
        // dialog creation so that the inner method has access to the dialog variable
        channelListSelectionAdapter.setCallback(which -> {
            if (callback != null) {
                callback.onChannelIdSelected(which);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    protected void handlePrioritySelection(String[] priorityList, int selectedPriority, RecordingPriorityListCallback callback) {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_priority)
                .items(priorityList)
                .itemsCallbackSingleChoice(selectedPriority, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (callback != null) {
                            callback.onPrioritySelected(which);
                        }
                        return true;
                    }
                })
                .show();
    }

    protected void handleRecordingProfileSelection(String[] recordingProfilesList, int selectedProfile, RecordingProfileListCallback callback) {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_dvr_config)
                .items(recordingProfilesList)
                .itemsCallbackSingleChoice(selectedProfile, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (callback != null) {
                            callback.onProfileSelected(which);
                        }
                        return true;
                    }
                })
                .show();
    }

    protected void handleDateSelection(long milliSeconds, DateTimePickerCallback callback, String tag) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);

                        if (callback != null) {
                            callback.onDateSelected(calendar.getTimeInMillis(), tag);
                        }
                    }
                }, year, month, day, false);

        datePicker.setCloseOnSingleTapDay(false);
        datePicker.show(activity.getSupportFragmentManager(), "");
    }

    protected void handleTimeSelection(long milliSeconds, DateTimePickerCallback callback, String tag) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                        calendar.set(Calendar.MINUTE, selectedMinute);
                        if (callback != null) {
                            callback.onTimeSelected(calendar.getTimeInMillis(), tag);
                        }
                    }
                }, hour, minute, true, false);

        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(activity.getSupportFragmentManager(), "");
    }
}
