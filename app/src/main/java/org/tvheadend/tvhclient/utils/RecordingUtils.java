package org.tvheadend.tvhclient.utils;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.ChannelListSelectionAdapter;
import org.tvheadend.tvhclient.callbacks.ChannelListSelectionCallback;
import org.tvheadend.tvhclient.callbacks.DateTimePickerCallback;
import org.tvheadend.tvhclient.callbacks.DaysOfWeekSelectionCallback;
import org.tvheadend.tvhclient.callbacks.DuplicateDetectionListCallback;
import org.tvheadend.tvhclient.callbacks.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.callbacks.RecordingProfileListCallback;
import org.tvheadend.tvhclient.model.Channel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RecordingUtils {

    private final int mHtspVersion;
    private final boolean mIsUnlocked;
    private WeakReference<Activity> activity;

    public RecordingUtils(Activity activity) {
        this.activity = new WeakReference<>(activity);
        mHtspVersion = DataStorage.getInstance().getProtocolVersion();
        mIsUnlocked = TVHClientApplication.getInstance().isUnlocked();
    }

    public void handleDateSelection(Calendar date, DateTimePickerCallback callback, String tag) {
        AppCompatActivity activity = (AppCompatActivity) this.activity.get();
        if (activity == null) {
            return;
        }
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePicker = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                        if (callback != null) {
                            callback.onDateSelected(year, month, day, tag);
                        }
                    }
                }, year, month, day, false);

        datePicker.setCloseOnSingleTapDay(false);
        datePicker.show(activity.getSupportFragmentManager(), "");
    }

    public void handleTimeSelection(Calendar time, DateTimePickerCallback callback, String tag) {
        AppCompatActivity activity = (AppCompatActivity) this.activity.get();
        if (activity == null) {
            return;
        }
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                        if (callback != null) {
                            callback.onTimeSelected(selectedHour, selectedMinute, tag);
                        }
                    }
                }, hour, minute, true, false);

        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(activity.getSupportFragmentManager(), "");
    }

    public void handleRecordingProfileSelection(String[] recordingProfilesList, int selectedProfile, RecordingProfileListCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null) {
            return;
        }
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

    public void handlePrioritySelection(String[] priorityList, int selectedPriority, RecordingPriorityListCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null) {
            return;
        }
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

    public void handleDayOfWeekSelection(int daysOfWeek, DaysOfWeekSelectionCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null) {
            return;
        }
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

    public void handleDuplicateDetectionSelection(String[] duplicateDetectionList, int duplicateDetectionId, DuplicateDetectionListCallback callback) {
        Activity activity = this.activity.get();
        if (activity == null) {
            return;
        }
        new MaterialDialog.Builder(activity)
                .title(R.string.select_duplicate_detection)
                .items(duplicateDetectionList)
                .itemsCallbackSingleChoice(duplicateDetectionId, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (callback != null) {
                            callback.onDuplicateDetectionValueSelected(which);
                        }
                        return true;
                    }
                })
                .show();
    }

    public void handleChannelListSelection(long selectedChannelId, ChannelListSelectionCallback callback, boolean showAllChannelsListEntry) {
        Activity activity = this.activity.get();
        if (activity == null) {
            return;
        }

        // Fill the channel tag adapter with the available channel tags
        List<Channel> channelList = new ArrayList<>();
        Map<Integer, Channel> map = DataStorage.getInstance().getChannelsFromArray();
        channelList.addAll(map.values());

        // Sort the channel tag list before showing it
        Collections.sort(channelList, new Comparator<Channel>() {
            @Override
            public int compare(Channel o1, Channel o2) {
                return o1.channelName.compareTo(o2.channelName);
            }
        });

        // Add the default channel (all channels)
        // to the list after it has been sorted
        if (showAllChannelsListEntry) {
            Channel channel = new Channel();
            channel.channelId = 0;
            channel.channelName = activity.getString(R.string.all_channels);
            channelList.add(0, channel);
        }

        final ChannelListSelectionAdapter channelListSelectionAdapter = new ChannelListSelectionAdapter(activity, channelList, selectedChannelId);
        // Show the dialog that shows all available channel tags. When the
        // user has selected a tag, restart the loader to get the updated channel list
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
}
