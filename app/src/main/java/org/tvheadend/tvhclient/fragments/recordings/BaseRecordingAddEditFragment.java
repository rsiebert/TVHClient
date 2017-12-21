package org.tvheadend.tvhclient.fragments.recordings;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.utils.MenuUtils;

import java.util.Calendar;

public class BaseRecordingAddEditFragment extends Fragment {

    protected Activity activity;
    protected ToolbarInterfaceLight toolbarInterface;
    protected MenuUtils menuUtils;
    protected DataStorage dataStorage;
    protected int htspVersion;
    protected boolean isUnlocked;
    protected Profile profile;

    protected int priority;
    protected int recordingProfileName;
    protected String[] priorityList;
    protected String[] recordingProfilesList;
    protected DatabaseHelper databaseHelper;

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
        databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        Connection connection = databaseHelper.getSelectedConnection();
        profile = databaseHelper.getProfile(connection.recording_profile_id);
        setHasOptionsMenu(true);

        // Create the list of available configurations that the user can select from
        recordingProfilesList = new String[dataStorage.getDvrConfigs().size()];
        for (int i = 0; i < dataStorage.getDvrConfigs().size(); i++) {
            recordingProfilesList[i] = dataStorage.getDvrConfigs().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);
    }

    protected void handleDateSelection(Calendar time, TextView dateTextView) {
        int year = time.get(Calendar.YEAR);
        int month = time.get(Calendar.MONTH);
        int day = time.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePicker = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                        time.set(Calendar.DAY_OF_MONTH, day);
                        time.set(Calendar.MONTH, month);
                        time.set(Calendar.YEAR, year);
                        dateTextView.setText(getDateStringFromDate(time));
                    }
                }, year, month, day, false);

        datePicker.setCloseOnSingleTapDay(false);
        datePicker.show(getChildFragmentManager(), "");
    }

    protected void handleTimeSelection(Calendar time, TextView timeTextView) {
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout timePicker, int selectedHour, int selectedMinute) {
                        // Save the given value in seconds. This values will be passed to the server
                        time.set(Calendar.HOUR_OF_DAY, selectedHour);
                        time.set(Calendar.MINUTE, selectedMinute);
                        timeTextView.setText(getTimeStringFromDate(time));
                    }
                }, hour, minute, true, false);

        timePickerDialog.setCloseOnSingleTapMinute(false);
        timePickerDialog.show(getChildFragmentManager(), "");
    }

    protected void handleRecordingProfileSelection(TextView recordingProfileTextView) {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_dvr_config)
                .items(recordingProfilesList)
                .itemsCallbackSingleChoice(recordingProfileName, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        recordingProfileTextView.setText(recordingProfilesList[which]);
                        recordingProfileName = which;
                        return true;
                    }
                })
                .show();
    }

    protected void handlePrioritySelection(TextView priorityTextView) {
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

    protected String getDateStringFromDate(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        String text = ((day < 10) ? "0" + day : day) + "."
                + ((month < 10) ? "0" + month : month) + "." + year;
        return text;
    }

    protected String getTimeStringFromDate(Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        String text = ((hour < 10) ? "0" + hour : hour) + ":"
                + ((minute < 10) ? "0" + minute : minute);
        return text;
    }
}
