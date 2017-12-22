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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecordingUtils {

    private final int mHtspVersion;
    private final boolean mIsUnlocked;
    private WeakReference<Activity> activity;

    public RecordingUtils(Activity activity) {
        this.activity = new WeakReference<>(activity);
        mHtspVersion = DataStorage.getInstance().getProtocolVersion();
        mIsUnlocked = TVHClientApplication.getInstance().isUnlocked();
    }

    public void handleDateSelection(Calendar date, RecordingDateTimeCallback callback, String tag) {
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
                            callback.dateSelected(year, month, day, tag);
                        }
                    }
                }, year, month, day, false);

        datePicker.setCloseOnSingleTapDay(false);
        datePicker.show(activity.getSupportFragmentManager(), "");
    }

    public void handleTimeSelection(Calendar time, RecordingDateTimeCallback callback, String tag) {
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
                            callback.timeSelected(selectedHour, selectedMinute, tag);
                        }
                    }
                }, hour, minute, true, false);

        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(activity.getSupportFragmentManager(), "");
    }

    public void handleRecordingProfileSelection(String[] recordingProfilesList, int selectedProfile, RecordingProfileCallback callback) {
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
                            callback.profileSelected(which);
                        }
                        return true;
                    }
                })
                .show();
    }

    public void handlePrioritySelection(String[] priorityList, int selectedPriority, RecordingPriorityCallback callback) {
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
                            callback.prioritySelected(which);
                        }
                        return true;
                    }
                })
                .show();
    }

    public void handleDayOfWeekSelection(int daysOfWeek, RecordingDayOfWeekCallback callback) {
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
                            callback.dayOfWeekSelected(selectedDays);
                        }
                        return true;
                    }
                })
                .positiveText(R.string.select)
                .show();
    }

    public void handleDuplicateDetectionSelection(String[] duplicateDetectionList, int duplicateDetectionId, RecordingDuplicateCallback callback) {
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
                            callback.duplicateSelected(which);
                        }
                        return true;
                    }
                })
                .show();
    }
}
