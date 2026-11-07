package org.tvheadend.tvhclient.ui.features.dvr

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tvheadend.tvhclient.ui.common.buildRecyclerView
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

fun getPriorityName(context: Context, priority: Int): String {
    val priorityNames = context.resources.getStringArray(R.array.dvr_priority_names)
    return when (priority) {
        in 0..4 -> priorityNames[priority]
        6 -> priorityNames[5]
        else -> ""
    }
}

fun getDateStringFromTimeInMillis(milliSeconds: Long): String {
    val sdf = SimpleDateFormat("dd.MM", Locale.US)
    return sdf.format(milliSeconds)
}

fun getTimeStringFromTimeInMillis(milliSeconds: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    return sdf.format(milliSeconds)
}

fun getSelectedDaysOfWeekText(context: Context, daysOfWeek: Int): String {
    val daysOfWeekList = context.resources.getStringArray(R.array.day_short_names)
    val text = StringBuilder()
    for (i in 0..6) {
        val s = if (daysOfWeek shr i and 1 == 1) daysOfWeekList[i] else ""
        if (text.isNotEmpty() && s.isNotEmpty()) {
            text.append(", ")
        }
        text.append(s)
    }
    return text.toString()
}

fun handleDayOfWeekSelection(context: Context, daysOfWeek: Int, callback: RecordingConfigSelectedListener?) {
    // Get the selected indices by storing the bits with 1 positions in a list
    // This list then needs to be converted to an Integer[] because the
    // material dialog requires this
    val list = ArrayList<Int>()
    for (i in 0..6) {
        val value = daysOfWeek shr i and 1
        if (value == 1) {
            list.add(i)
        }
    }
    val dayNames = context.resources.getStringArray(R.array.day_long_names)
    val checked = BooleanArray(dayNames.size)
    for (i in list) {
        if (i in checked.indices) {
            checked[i] = true
        }
    }
    MaterialAlertDialogBuilder(context)
            .setTitle(R.string.days_of_week)
            .setMultiChoiceItems(dayNames, checked) { _, _, _ -> /* selection state is kept in checked */ }
            .setPositiveButton(R.string.select) { _, _ ->
                var selectedDays = 0
                for (i in checked.indices) {
                    if (checked[i]) {
                        selectedDays += 1 shl i
                    }
                }
                callback?.onDaysSelected(selectedDays)
            }
            .show()
}

fun handleChannelListSelection(context: Context, channelList: List<Channel>, showAllChannelsListEntry: Boolean, callback: RecordingConfigSelectedListener?) {
    // Fill the channel tag adapter with the available channel tags
    val channels = CopyOnWriteArrayList(channelList)

    // Add the default channel (all channels)
    // to the list after it has been sorted
    if (showAllChannelsListEntry) {
        val channel = Channel()
        channel.id = 0
        channel.name = context.getString(R.string.all_channels)
        channels.add(0, channel)
    }

    val channelListSelectionAdapter = ChannelListSelectionAdapter(context, channels)
    // Show the dialog that shows all available channel tags. When the
    // user has selected a tag, restart the loader to loadRecordingById the updated channel list
    val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.tags)
            .setView(buildRecyclerView(context, channelListSelectionAdapter))
            .create()

    // Set the callback to handle clicks. This needs to be done after the
    // dialog creation so that the inner method has access to the dialog variable
    channelListSelectionAdapter.setCallback(object : ChannelListSelectionAdapter.Callback {
        override fun onItemClicked(channel: Channel) {
            callback?.onChannelSelected(channel)
            dialog.dismiss()
        }
    })
    dialog.show()
}

fun handlePrioritySelection(context: Context, selectedPriority: Int, callback: RecordingConfigSelectedListener?) {
    Timber.d("Selected priority is ${if (selectedPriority == 6) 5 else selectedPriority}")
    val priorityNames = context.resources.getStringArray(R.array.dvr_priority_names)
    MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select_priority)
            .setSingleChoiceItems(priorityNames, if (selectedPriority == 6) 5 else selectedPriority) { _, index ->
                Timber.d("New selected priority is ${if (index == 5) 6 else index}")
                callback?.onPrioritySelected(if (index == 5) 6 else index)
            }
            .show()
}

fun handleRecordingProfileSelection(context: Context, recordingProfilesList: Array<String>, selectedProfile: Int, callback: RecordingConfigSelectedListener?) {
    MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select_dvr_config)
            .setSingleChoiceItems(recordingProfilesList, selectedProfile) { _, index ->
                callback?.onProfileSelected(index)
            }
            .show()
}

fun getSelectedProfileId(profile: ServerProfile?, recordingProfilesList: Array<String>): Int {
    if (profile != null) {
        for (i in recordingProfilesList.indices) {
            if (recordingProfilesList[i] == profile.name) {
                return i
            }
        }
    }
    return 0
}

fun handleDateSelection(activity: FragmentActivity?, milliSeconds: Long, callback: Fragment, tag: String) {
    activity?.let {
        val newFragment = DatePickerFragment()
        val bundle = Bundle()
        bundle.putLong("milliSeconds", milliSeconds)
        newFragment.arguments = bundle
        newFragment.setTargetFragment(callback, 1)
        newFragment.show(activity.supportFragmentManager, tag)
    }
}

fun handleTimeSelection(activity: FragmentActivity?, milliSeconds: Long, callback: Fragment, tag: String) {
    activity?.let {
        val newFragment = TimePickerFragment()
        val bundle = Bundle()
        bundle.putLong("milliSeconds", milliSeconds)
        newFragment.arguments = bundle
        newFragment.setTargetFragment(callback, 1)
        newFragment.show(activity.supportFragmentManager, tag)
    }
}
