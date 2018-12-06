package org.tvheadend.tvhclient.features.dvr;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.adapter.ChannelListSelectionAdapter;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelListSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.DaysOfWeekSelectionCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecordingPriorityListCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.RecordingProfileListCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

public abstract class BaseRecordingAddEditFragment extends BaseFragment {

    private String[] daysOfWeekList;
    protected String[] priorityList;
    protected String[] recordingProfilesList;
    private List<Channel> channelList;
    protected ServerProfile profile;
    protected int recordingProfileNameId;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        daysOfWeekList = activity.getResources().getStringArray(R.array.day_short_names);
        recordingProfilesList = appRepository.getServerProfileData().getRecordingProfileNames();
        Timber.d("Recording profile list size is " + recordingProfilesList.length);

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);
        Timber.d("Priority list size is " + priorityList.length);

        channelList = appRepository.getChannelData().getItems();

        profile = appRepository.getServerProfileData().getItemById(serverStatus.getRecordingServerProfileId());
        // Get the selected profile from the connection and select it from the recording config list
        if (profile != null) {
            for (int i = 0; i < recordingProfilesList.length; i++) {
                if (recordingProfilesList[i].equals(profile.getName())) {
                    recordingProfileNameId = i;
                    break;
                }
            }
        }

        setHasOptionsMenu(true);
    }

    protected String getDateStringFromTimeInMillis(long milliSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd:MM", Locale.US);
        return sdf.format(milliSeconds);
    }

    protected long getMinutesFromTimeInMillis(long milliSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return (long) (hour * 60 + minute);
    }

    protected String getTimeStringFromTimeInMillis(long milliSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        return sdf.format(milliSeconds);
    }

    protected String getSelectedDaysOfWeekText(int daysOfWeek) {
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
                .itemsCallbackMultiChoice(selectedIndices, (dialog, which, text) -> {
                    int selectedDays = 0;
                    for (Integer i : which) {
                        selectedDays += (1 << i);
                    }
                    if (callback != null) {
                        callback.onDaysOfWeekSelected(selectedDays);
                    }
                    return true;
                })
                .positiveText(R.string.select)
                .show();
    }

    protected void handleChannelListSelection(ChannelListSelectionCallback callback, boolean showAllChannelsListEntry) {
        // Fill the channel tag adapter with the available channel tags
        CopyOnWriteArrayList<Channel> channels = new CopyOnWriteArrayList<>(channelList);

        // Add the default channel (all channels)
        // to the list after it has been sorted
        if (showAllChannelsListEntry) {
            Channel channel = new Channel();
            channel.setId(0);
            channel.setName(activity.getString(R.string.all_channels));
            channels.add(0, channel);
        }

        final ChannelListSelectionAdapter channelListSelectionAdapter = new ChannelListSelectionAdapter(activity, channels);
        // Show the dialog that shows all available channel tags. When the
        // user has selected a tag, restart the loader to loadRecordingById the updated channel list
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(R.string.tags)
                .adapter(channelListSelectionAdapter, null)
                .build();
        // Set the callback to handle clicks. This needs to be done after the
        // dialog creation so that the inner method has access to the dialog variable
        channelListSelectionAdapter.setCallback(channel -> {
            if (callback != null) {
                callback.onChannelIdSelected(channel);
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
                .itemsCallbackSingleChoice(selectedPriority, (dialog, view, which, text) -> {
                    if (callback != null) {
                        callback.onPrioritySelected(which);
                    }
                    return true;
                })
                .show();
    }

    protected void handleRecordingProfileSelection(String[] recordingProfilesList, int selectedProfile, RecordingProfileListCallback callback) {
        new MaterialDialog.Builder(activity)
                .title(R.string.select_dvr_config)
                .items(recordingProfilesList)
                .itemsCallbackSingleChoice(selectedProfile, (dialog, view, which, text) -> {
                    if (callback != null) {
                        callback.onProfileSelected(which);
                    }
                    return true;
                })
                .show();
    }

    protected void handleDateSelection(long milliSeconds, Fragment callback, String tag) {
        DialogFragment newFragment = new DatePickerFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("milliSeconds", milliSeconds);
        newFragment.setArguments(bundle);
        newFragment.setTargetFragment(callback, 1);
        newFragment.show(activity.getSupportFragmentManager(), tag);
    }

    protected void handleTimeSelection(long milliSeconds, Fragment callback, String tag) {
        DialogFragment newFragment = new TimePickerFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("milliSeconds", milliSeconds);
        newFragment.setArguments(bundle);
        newFragment.setTargetFragment(callback, 1);
        newFragment.show(activity.getSupportFragmentManager(), tag);
    }
}
