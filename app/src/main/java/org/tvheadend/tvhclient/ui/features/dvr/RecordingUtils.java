package org.tvheadend.tvhclient.ui.features.dvr;

import android.content.Context;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.ServerProfile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import timber.log.Timber;

public class RecordingUtils {

    public static String getPriorityName(Context context,  int priority) {
        String[] priorityNames = context.getResources().getStringArray(R.array.dvr_priority_names);
        if (priority >= 0 && priority <= 4) {
            return priorityNames[priority];
        } else if (priority == 6) {
            return priorityNames[5];
        } else {
            return "";
        }
    }

    public static String getDateStringFromTimeInMillis(long milliSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd:MM", Locale.US);
        return sdf.format(milliSeconds);
    }

    public static String getTimeStringFromTimeInMillis(long milliSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        return sdf.format(milliSeconds);
    }

    public static String getSelectedDaysOfWeekText(Context context, int daysOfWeek) {
        String[] daysOfWeekList = context.getResources().getStringArray(R.array.day_short_names);
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

    public static void handleDayOfWeekSelection(Context context, int daysOfWeek, RecordingConfigSelectedListener callback) {
        // Get the selected indices by storing the bits with 1 positions in a list
        // This list then needs to be converted to an Integer[] because the
        // material dialog requires this
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            int value = (daysOfWeek >> i) & 1;
            if (value == 1) {
                list.add(i);
            }
        }
        Integer[] selectedIndices = new Integer[list.size()];
        for (int i = 0; i < selectedIndices.length; i++) {
            selectedIndices[i] = list.get(i);
        }
        new MaterialDialog.Builder(context)
                .title(R.string.days_of_week)
                .items(R.array.day_long_names)
                .itemsCallbackMultiChoice(selectedIndices, (dialog, which, text) -> {
                    int selectedDays = 0;
                    for (Integer i : which) {
                        selectedDays += (1 << i);
                    }
                    if (callback != null) {
                        callback.onDaysSelected(selectedDays);
                    }
                    return true;
                })
                .positiveText(R.string.select)
                .show();
    }

    public static void handleChannelListSelection(Context context, List<Channel> channelList, boolean showAllChannelsListEntry, RecordingConfigSelectedListener callback) {
        // Fill the channel tag adapter with the available channel tags
        CopyOnWriteArrayList<Channel> channels = new CopyOnWriteArrayList<>(channelList);

        // Add the default channel (all channels)
        // to the list after it has been sorted
        if (showAllChannelsListEntry) {
            Channel channel = new Channel();
            channel.setId(0);
            channel.setName(context.getString(R.string.all_channels));
            channels.add(0, channel);
        }

        final ChannelListSelectionAdapter channelListSelectionAdapter = new ChannelListSelectionAdapter(context, channels);
        // Show the dialog that shows all available channel tags. When the
        // user has selected a tag, restart the loader to loadRecordingById the updated channel list
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.tags)
                .adapter(channelListSelectionAdapter, null)
                .build();
        // Set the callback to handle clicks. This needs to be done after the
        // dialog creation so that the inner method has access to the dialog variable
        channelListSelectionAdapter.setCallback(channel -> {
            if (callback != null) {
                callback.onChannelSelected(channel);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public static void handlePrioritySelection(Context context, int selectedPriority, RecordingConfigSelectedListener callback) {
        Timber.d("Selected priority is " + (selectedPriority == 6 ? 5 : selectedPriority));
        String[] priorityNames = context.getResources().getStringArray(R.array.dvr_priority_names);
        new MaterialDialog.Builder(context)
                .title(R.string.select_priority)
                .items(priorityNames)
                .itemsCallbackSingleChoice(selectedPriority == 6 ? 5 : selectedPriority, (dialog, view, which, text) -> {
                    if (callback != null) {
                        Timber.d("New selected priority is " + (which == 5 ? 6 : which));
                        callback.onPrioritySelected(which == 5 ? 6 : which);
                    }
                    return true;
                })
                .show();
    }

    public static void handleRecordingProfileSelection(Context context, String[] recordingProfilesList, int selectedProfile, RecordingConfigSelectedListener callback) {
        new MaterialDialog.Builder(context)
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

    public static int getSelectedProfileId(ServerProfile profile, String[] recordingProfilesList) {
        if (profile != null) {
            for (int i = 0; i < recordingProfilesList.length; i++) {
                if (recordingProfilesList[i].equals(profile.getName())) {
                    return i;
                }
            }
        }
        return 0;
    }

    public static void handleDateSelection(AppCompatActivity activity, long milliSeconds, Fragment callback, String tag) {
        DialogFragment newFragment = new DatePickerFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("milliSeconds", milliSeconds);
        newFragment.setArguments(bundle);
        newFragment.setTargetFragment(callback, 1);
        newFragment.show(activity.getSupportFragmentManager(), tag);
    }

    public static void handleTimeSelection(AppCompatActivity activity, long milliSeconds, Fragment callback, String tag) {
        DialogFragment newFragment = new TimePickerFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("milliSeconds", milliSeconds);
        newFragment.setArguments(bundle);
        newFragment.setTargetFragment(callback, 1);
        newFragment.show(activity.getSupportFragmentManager(), tag);
    }
}
