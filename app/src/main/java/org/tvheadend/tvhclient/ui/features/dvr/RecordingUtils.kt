package org.tvheadend.tvhclient.ui.features.dvr

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.listItemsSingleChoice
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
    val selectedIndices = arrayOfNulls<Int>(list.size)
    for (i in selectedIndices.indices) {
        selectedIndices[i] = list[i]
    }
    MaterialDialog(context).show {
        title(R.string.days_of_week)
        positiveButton(R.string.select)
        listItemsMultiChoice(R.array.day_long_names) { _, index, _ ->
            var selectedDays = 0
            for (i in index) {
                selectedDays += 1 shl i
            }
            callback?.onDaysSelected(selectedDays)
        }
    }
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
    val dialog: MaterialDialog = MaterialDialog(context).show {
        title(R.string.tags)
        customListAdapter(channelListSelectionAdapter)
    }

    // Set the callback to handle clicks. This needs to be done after the
    // dialog creation so that the inner method has access to the dialog variable
    channelListSelectionAdapter.setCallback(object : ChannelListSelectionAdapter.Callback {
        override fun onItemClicked(channel: Channel) {
            callback?.onChannelSelected(channel)
            dialog.dismiss()
        }
    })
}

fun handlePrioritySelection(context: Context, selectedPriority: Int, callback: RecordingConfigSelectedListener?) {
    Timber.d("Selected priority is ${if (selectedPriority == 6) 5 else selectedPriority}")
    MaterialDialog(context).show {
        title(R.string.select_priority)
        listItemsSingleChoice(R.array.dvr_priority_names, initialSelection = if (selectedPriority == 6) 5 else selectedPriority) { _, index, _ ->
            Timber.d("New selected priority is ${if (index == 5) 6 else index}")
            callback?.onPrioritySelected(if (index == 5) 6 else index)
        }
    }
}

fun handleRecordingProfileSelection(context: Context, recordingProfilesList: Array<String>, selectedProfile: Int, callback: RecordingConfigSelectedListener?) {
    MaterialDialog(context).show {
        title(R.string.select_dvr_config)
        listItemsSingleChoice(items = recordingProfilesList.toList(), initialSelection = selectedProfile) { _, index, _ ->
            callback?.onProfileSelected(index)
        }
    }
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
