package org.tvheadend.tvhclient.ui.recordings;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.model.Profile;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.RecordingUtils;

import java.util.Calendar;

// TODO move day of week into here
// TODO Rename to recording utils maybe
// TODO add title and full names to day of weeks list

public class BaseRecordingAddEditFragment extends Fragment {

    protected Activity activity;
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
    protected RecordingUtils recordingUtils;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        menuUtils = new MenuUtils(getActivity());
        recordingUtils = new RecordingUtils(getActivity());
        dataStorage = DataStorage.getInstance();
        htspVersion = dataStorage.getProtocolVersion();
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
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
}
