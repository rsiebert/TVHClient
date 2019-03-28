@file:JvmName("RecordingUtils")

package org.tvheadend.tvhclient.ui.features.dvr

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.ServerProfile
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
    val sdf = SimpleDateFormat("dd:MM", Locale.US)
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
    MaterialDialog.Builder(context)
            .title(R.string.days_of_week)
            .items(R.array.day_long_names)
            .itemsCallbackMultiChoice(selectedIndices) { _, which, _ ->
                var selectedDays = 0
                for (i in which) {
                    selectedDays += 1 shl i!!
                }
                callback?.onDaysSelected(selectedDays)
                true
            }
            .positiveText(R.string.select)
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
    val dialog = MaterialDialog.Builder(context)
            .title(R.string.tags)
            .adapter(channelListSelectionAdapter, null)
            .build()
    // Set the callback to handle clicks. This needs to be done after the
    // dialog creation so that the inner method has access to the dialog variable
    channelListSelectionAdapter.setCallback { channel ->
        callback?.onChannelSelected(channel)
        dialog?.dismiss()
    }
    dialog!!.show()
}

fun handlePrioritySelection(context: Context, selectedPriority: Int, callback: RecordingConfigSelectedListener?) {
    Timber.d("Selected priority is ${if (selectedPriority == 6) 5 else selectedPriority}")
    val priorityNames = context.resources.getStringArray(R.array.dvr_priority_names)
    MaterialDialog.Builder(context)
            .title(R.string.select_priority)
            .items(*priorityNames)
            .itemsCallbackSingleChoice(if (selectedPriority == 6) 5 else selectedPriority) { _, _, which, _ ->
                if (callback != null) {
                    Timber.d("New selected priority is ${if (which == 5) 6 else which}")
                    callback.onPrioritySelected(if (which == 5) 6 else which)
                }
                true
            }
            .show()
}

fun handleRecordingProfileSelection(context: Context, recordingProfilesList: Array<String>, selectedProfile: Int, callback: RecordingConfigSelectedListener?) {
    MaterialDialog.Builder(context)
            .title(R.string.select_dvr_config)
            .items(*recordingProfilesList)
            .itemsCallbackSingleChoice(selectedProfile) { _, _, which, _ ->
                callback?.onProfileSelected(which)
                true
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

fun handleDateSelection(activity: AppCompatActivity, milliSeconds: Long, callback: Fragment, tag: String) {
    val newFragment = DatePickerFragment()
    val bundle = Bundle()
    bundle.putLong("milliSeconds", milliSeconds)
    newFragment.arguments = bundle
    newFragment.setTargetFragment(callback, 1)
    newFragment.show(activity.supportFragmentManager, tag)
}

fun handleTimeSelection(activity: AppCompatActivity, milliSeconds: Long, callback: Fragment, tag: String) {
    val newFragment = TimePickerFragment()
    val bundle = Bundle()
    bundle.putLong("milliSeconds", milliSeconds)
    newFragment.arguments = bundle
    newFragment.setTargetFragment(callback, 1)
    newFragment.show(activity.supportFragmentManager, tag)
}
