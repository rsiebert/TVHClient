package org.tvheadend.tvhclient.ui.common


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Menu
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import org.tvheadend.data.entity.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.interfaces.RecordingRemovedInterface
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingAddEditFragment
import org.tvheadend.tvhclient.ui.features.dvr.recordings.download.DownloadRecordingActivity
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingAddEditFragment
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingAddEditFragment
import org.tvheadend.tvhclient.ui.features.playback.external.*
import org.tvheadend.tvhclient.ui.features.playback.internal.PlaybackActivity
import org.tvheadend.tvhclient.ui.features.programs.ProgramListFragment
import org.tvheadend.tvhclient.util.extensions.getCastSession
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

fun preparePopupOrToolbarRecordingMenu(context: Context,
                                       menu: Menu,
                                       recording: Recording?,
                                       isConnectionToServerAvailable: Boolean,
                                       htspVersion: Int,
                                       isUnlocked: Boolean) {

    // Hide the menus because the ones in the toolbar are not hidden when set in the xml
    menu.children.forEach { it.isVisible = false }

    if (isConnectionToServerAvailable) {
        if (recording == null || (!recording.isRecording
                        && !recording.isScheduled
                        && !recording.isCompleted
                        && !recording.isFailed
                        && !recording.isFileMissing
                        && !recording.isMissed
                        && !recording.isAborted)) {
            Timber.d("Recording is not recording or scheduled")
            menu.findItem(R.id.menu_record_program)?.isVisible = true
            menu.findItem(R.id.menu_record_program_and_edit)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_record_program_with_custom_profile)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_record_program_as_series_recording)?.isVisible = htspVersion >= 13

        } else if (recording.isCompleted) {
            Timber.d("Recording is completed ")
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = context.getCastSession() != null
            menu.findItem(R.id.menu_remove_recording)?.isVisible = true
            menu.findItem(R.id.menu_download_recording)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_share_recording)?.isVisible = isUnlocked

        } else if (recording.isScheduled && !recording.isRecording) {
            Timber.d("Recording is scheduled")
            menu.findItem(R.id.menu_cancel_recording)?.isVisible = true
            menu.findItem(R.id.menu_edit_recording)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_disable_recording)?.isVisible = htspVersion >= 23 && isUnlocked && recording.isEnabled
            menu.findItem(R.id.menu_enable_recording)?.isVisible = htspVersion >= 23 && isUnlocked && !recording.isEnabled

        } else if (recording.isRecording) {
            Timber.d("Recording is being recorded")
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = context.getCastSession() != null
            menu.findItem(R.id.menu_stop_recording)?.isVisible = true
            menu.findItem(R.id.menu_edit_recording)?.isVisible = isUnlocked
            menu.findItem(R.id.menu_share_recording)?.isVisible = isUnlocked

        } else if (recording.isFailed || recording.isFileMissing || recording.isMissed || recording.isAborted) {
            Timber.d("Recording is failed, file is missing, has been missed or was aborted")
            menu.findItem(R.id.menu_remove_recording)?.isVisible = true
            // Allow playing a failed recording which size is not zero
            if (recording.dataSize > 0) {
                menu.findItem(R.id.menu_play)?.isVisible = true
                menu.findItem(R.id.menu_cast)?.isVisible = context.getCastSession() != null
            }
        }
    }
}

fun preparePopupOrToolbarMiscMenu(context: Context,
                                  menu: Menu,
                                  program: ProgramInterface?,
                                  isConnectionToServerAvailable: Boolean,
                                  isUnlocked: Boolean) {

    menu.findItem(R.id.menu_cast)?.isVisible = false
    menu.findItem(R.id.menu_play)?.isVisible = false
    menu.findItem(R.id.menu_add_notification)?.isVisible = false

    if (isConnectionToServerAvailable) {
        // Show the play menu item and the cast menu item (if available)
        // when the current time is between the program start and end time
        val currentTime = System.currentTimeMillis()
        if (program != null
                && program.start > 0
                && program.stop > 0
                && currentTime > program.start
                && currentTime < program.stop) {
            menu.findItem(R.id.menu_play)?.isVisible = true
            menu.findItem(R.id.menu_cast)?.isVisible = context.getCastSession() != null
        }
    }
    // Show the add reminder menu only for programs and
    // recordings where the start time is in the future.
    if (isUnlocked && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notifications_enabled", context.resources.getBoolean(R.bool.pref_default_notifications_enabled))) {
        val currentTime = System.currentTimeMillis()
        var startTime = currentTime
        if (program != null && program.start > 0) {
            startTime = program.start
        }
        menu.findItem(R.id.menu_add_notification)?.isVisible = startTime > currentTime
    }
}

fun preparePopupOrToolbarSearchMenu(menu: Menu, title: String?, isConnectionToServerAvailable: Boolean) {
    val visible = isConnectionToServerAvailable && !title.isNullOrEmpty()
    menu.findItem(R.id.menu_search)?.isVisible = visible
    menu.findItem(R.id.menu_search_imdb)?.isVisible = visible
    menu.findItem(R.id.menu_search_fileaffinity)?.isVisible = visible
    menu.findItem(R.id.menu_search_youtube)?.isVisible = visible
    menu.findItem(R.id.menu_search_google)?.isVisible = visible
    menu.findItem(R.id.menu_search_epg)?.isVisible = visible
}

fun showConfirmationToReconnectToServer(context: Context, viewModel: BaseViewModel): Boolean {
    MaterialDialog(context).show {
        title(R.string.reconnect_to_server)
        message(R.string.restart_and_sync)
        negativeButton(R.string.cancel)
        positiveButton(R.string.reconnect) {
            viewModel.updateConnectionAndRestartApplication(context)
        }
    }
    return true
}

fun recordSelectedProgram(context: Context, eventId: Int, profile: ServerProfile?, htspVersion: Int): Boolean {
    val intent = Intent(context, ConnectionService::class.java)
    intent.action = "addDvrEntry"
    intent.putExtra("eventId", eventId)

    if (profile != null && htspVersion >= 16) {
        intent.putExtra("configName", profile.name)
    }
    context.startService(intent)
    return true
}

fun recordSelectedProgramAsSeriesRecording(context: Context, title: String?, channelId: Int, profile: ServerProfile?, htspVersion: Int): Boolean {
    val intent = Intent(context, ConnectionService::class.java)
    intent.action = "addAutorecEntry"
    intent.putExtra("title", title)
    intent.putExtra("channelId", channelId)

    if (profile != null && htspVersion >= 16) {
        intent.putExtra("configName", profile.name)
    }
    context.startService(intent)
    return true
}

fun showConfirmationToStopSelectedRecording(context: Context, recording: Recording?, callback: RecordingRemovedInterface?): Boolean {
    recording ?: return false
    Timber.d("Stopping recording ${recording.title}")
    MaterialDialog(context).show {
        title(R.string.record_stop)
        message(text = context.getString(R.string.stop_recording, recording.title))
        negativeButton(R.string.cancel)
        positiveButton(R.string.stop) {
            stopSelectedRecording(context, recording, callback)
        }
    }
    return true
}

private fun stopSelectedRecording(context: Context, recording: Recording, callback: RecordingRemovedInterface?) {
    val intent = Intent(context, ConnectionService::class.java)
    intent.action = "stopDvrEntry"
    intent.putExtra("id", recording.id)
    context.startService(intent)
    callback?.onRecordingRemoved()
}

fun showConfirmationToRemoveSelectedRecording(context: Context, recording: Recording?, callback: RecordingRemovedInterface?): Boolean {
    recording ?: return false
    Timber.d("Removing recording ${recording.title}")
    MaterialDialog(context).show {
        title(R.string.record_remove)
        message(text = context.getString(R.string.remove_recording, recording.title))
        negativeButton(R.string.cancel)
        positiveButton(R.string.remove) {
            removeSelectedRecording(context, recording, callback)
        }
    }
    return true
}

private fun removeSelectedRecording(context: Context, recording: Recording, callback: RecordingRemovedInterface?) {
    val intent = Intent(context, ConnectionService::class.java)
    intent.action = "deleteDvrEntry"
    intent.putExtra("id", recording.id)
    context.startService(intent)
    callback?.onRecordingRemoved()
}

fun showConfirmationToCancelSelectedRecording(context: Context, recording: Recording?, callback: RecordingRemovedInterface?): Boolean {
    recording ?: return false
    Timber.d("Cancelling recording ${recording.title}")
    MaterialDialog(context).show {
        title(R.string.record_remove)
        message(text = context.getString(R.string.cancel_recording, recording.title))
        negativeButton(R.string.cancel)
        positiveButton(R.string.remove) {
            cancelSelectedRecording(context, recording, callback)
        }
    }
    return true
}

private fun cancelSelectedRecording(context: Context, recording: Recording, callback: RecordingRemovedInterface?) {
    val intent = Intent(context, ConnectionService::class.java)
    intent.action = "cancelDvrEntry"
    intent.putExtra("id", recording.id)
    context.startService(intent)
    callback?.onRecordingRemoved()
}

fun editSelectedRecording(activity: FragmentActivity, id: Int): Boolean {
    val fragment = RecordingAddEditFragment.newInstance(id)
    activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    return true
}

fun editSelectedSeriesRecording(activity: FragmentActivity, id: String): Boolean {
    val fragment = SeriesRecordingAddEditFragment.newInstance(id)
    activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    return true
}

fun editSelectedTimerRecording(activity: FragmentActivity, id: String): Boolean {
    val fragment = TimerRecordingAddEditFragment.newInstance(id)
    activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    return true
}

fun addNewRecording(activity: FragmentActivity): Boolean {
    val fragment = RecordingAddEditFragment.newInstance()
    activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    return true
}

fun addNewSeriesRecording(activity: FragmentActivity): Boolean {
    val fragment = SeriesRecordingAddEditFragment.newInstance()
    activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    return true
}

fun addNewTimerRecording(activity: FragmentActivity): Boolean {
    val fragment = TimerRecordingAddEditFragment.newInstance()
    activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    return true
}

fun showConfirmationToRemoveSelectedSeriesRecording(context: Context, recording: SeriesRecording, callback: RecordingRemovedInterface?): Boolean {
    Timber.d("Removing series recording ${recording.title}")
    MaterialDialog(context).show {
        title(R.string.record_remove)
        message(text = context.getString(R.string.remove_series_recording, recording.title))
        negativeButton(R.string.cancel)
        positiveButton(R.string.remove) {
            removeSelectedSeriesRecording(context, recording, callback)
        }
    }
    return true
}

private fun removeSelectedSeriesRecording(context: Context, recording: SeriesRecording, callback: RecordingRemovedInterface?) {
    val intent = Intent(context, ConnectionService::class.java)
    intent.action = "deleteAutorecEntry"
    intent.putExtra("id", recording.id)
    context.startService(intent)
    callback?.onRecordingRemoved()
}

fun showConfirmationToRemoveSelectedTimerRecording(context: Context, recording: TimerRecording, callback: RecordingRemovedInterface?): Boolean {
    val recordingName = recording.name ?: ""
    val name = if (recordingName.isNotEmpty()) recordingName else ""
    val displayTitle = if (name.isNotEmpty()) name else recording.title ?: ""
    Timber.d("Removing timer recording $displayTitle")

    MaterialDialog(context).show {
        title(R.string.record_remove)
        message(text = context.getString(R.string.remove_timer_recording, displayTitle))
        negativeButton(R.string.cancel)
        positiveButton(R.string.remove) {
            removeSelectedTimerRecording(context, recording, callback)
        }
    }
    return true
}

private fun removeSelectedTimerRecording(context: Context, recording: TimerRecording, callback: RecordingRemovedInterface?) {
    val intent = Intent(context, ConnectionService::class.java)
    intent.action = "deleteTimerecEntry"
    intent.putExtra("id", recording.id)
    context.startService(intent)
    callback?.onRecordingRemoved()
}

fun showConfirmationToRemoveAllRecordings(context: Context, items: List<Recording>): Boolean {
    MaterialDialog(context).show {
        title(R.string.record_remove_all)
        message(R.string.confirm_remove_all)
        negativeButton(R.string.cancel)
        positiveButton(R.string.remove) {
            removeAllRecordings(context, items)
        }
    }
    return true
}

private fun removeAllRecordings(context: Context, items: List<Recording>) {
    object : Thread() {
        override fun run() {
            for (item in items) {
                val intent = Intent(context, ConnectionService::class.java)
                intent.putExtra("id", item.id)
                if (item.isRecording || item.isScheduled) {
                    intent.action = "cancelDvrEntry"
                } else {
                    intent.action = "deleteDvrEntry"
                }
                context.startService(intent)
                try {
                    sleep(500)
                } catch (e: InterruptedException) {
                    // NOP
                }
            }
        }
    }.start()
}

fun showConfirmationToRemoveAllSeriesRecordings(context: Context, items: List<SeriesRecording>): Boolean {
    MaterialDialog(context).show {
        title(R.string.record_remove_all)
        message(R.string.remove_all_recordings)
        negativeButton(R.string.cancel)
        positiveButton(R.string.remove) {
            removeAllSeriesRecordings(context, items)
        }
    }
    return true
}

private fun removeAllSeriesRecordings(context: Context, items: List<SeriesRecording>) {
    object : Thread() {
        override fun run() {
            for ((id) in items) {
                val intent = Intent(context, ConnectionService::class.java)
                intent.action = "deleteAutorecEntry"
                intent.putExtra("id", id)
                context.startService(intent)
                try {
                    sleep(500)
                } catch (e: InterruptedException) {
                    // NOP
                }
            }
        }
    }.start()
}

fun showConfirmationToRemoveAllTimerRecordings(context: Context, items: List<TimerRecording>): Boolean {
    MaterialDialog(context).show {
        title(R.string.record_remove_all)
        message(R.string.remove_all_recordings)
        negativeButton(R.string.cancel)
        positiveButton(R.string.remove) {
            removeAllTimerRecordings(context, items)
        }
    }
    return true
}

private fun removeAllTimerRecordings(context: Context, items: List<TimerRecording>) {
    object : Thread() {
        override fun run() {
            for ((id) in items) {
                val intent = Intent(context, ConnectionService::class.java)
                intent.action = "deleteTimerecEntry"
                intent.putExtra("id", id)
                context.startService(intent)
                try {
                    sleep(500)
                } catch (e: InterruptedException) {
                    // NOP
                }
            }
        }
    }.start()
}

fun recordSelectedProgramWithCustomProfile(context: Context, eventId: Int, channelId: Int, serverProfileNames: Array<String>, serverProfile: ServerProfile?): Boolean {
    // Get the selected recording profile to highlight the
    // correct item in the list of the selection dialog
    var dvrConfigNameValue = 0

    if (serverProfile != null) {
        for (i in serverProfileNames.indices) {
            if (serverProfileNames[i] == serverProfile.name) {
                dvrConfigNameValue = i
                break
            }
        }
    }
    // Create the dialog to show the available profiles
    MaterialDialog(context).show {
        title(R.string.select_dvr_config)
        listItemsSingleChoice(items = serverProfileNames.toList(), initialSelection = dvrConfigNameValue) { _, index, _ ->
            val intent = Intent(context, ConnectionService::class.java)
            intent.action = "addDvrEntry"
            intent.putExtra("eventId", eventId)
            intent.putExtra("channelId", channelId)
            intent.putExtra("configName", serverProfileNames[index])
            context.startService(intent)
        }
    }
    return true
}

fun playSelectedChannel(context: Context, channelId: Int, isUnlocked: Boolean): Boolean {
    if (isUnlocked
            && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("internal_player_for_channels_enabled",
                    context.resources.getBoolean(R.bool.pref_default_internal_player_enabled))) {
        val intent = Intent(context, PlaybackActivity::class.java)
        intent.putExtra("channelId", channelId)
        context.startActivity(intent)
    } else {
        val intent = Intent(context, PlayChannelActivity::class.java)
        intent.putExtra("channelId", channelId)
        context.startActivity(intent)
    }
    return true
}

fun playSelectedRecording(context: Context, dvrId: Int, isUnlocked: Boolean): Boolean {
    if (isUnlocked
            && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("internal_player_for_recordings_enabled",
                    context.resources.getBoolean(R.bool.pref_default_internal_player_enabled))) {
        val intent = Intent(context, PlaybackActivity::class.java)
        intent.putExtra("dvrId", dvrId)
        context.startActivity(intent)
    } else {
        val intent = Intent(context, PlayRecordingActivity::class.java)
        intent.putExtra("dvrId", dvrId)
        context.startActivity(intent)
    }
    return true
}

fun downloadSelectedRecording(context: Context, dvrId: Int): Boolean {
    val intent = Intent(context, DownloadRecordingActivity::class.java)
    intent.putExtra("dvrId", dvrId)
    context.startActivity(intent)
    return true
}

fun shareSelectedRecording(context: Context, dvrId: Int, isUnlocked: Boolean): Boolean {
    if (isUnlocked) {
        val intent = Intent(context, ShareRecordingActivity::class.java)
        intent.putExtra("dvrId", dvrId)
        context.startActivity(intent)
    }
    return true
}

fun castSelectedChannel(context: Context, id: Int): Boolean {
    val intent = Intent(context, CastChannelActivity::class.java)
    intent.putExtra("channelId", id)
    context.startActivity(intent)
    return true
}

fun castSelectedRecording(context: Context, id: Int): Boolean {
    val intent = Intent(context, CastRecordingActivity::class.java)
    intent.putExtra("dvrId", id)
    context.startActivity(intent)
    return true
}

fun playOrCastChannel(context: Context, channelId: Int, isUnlocked: Boolean): Boolean {
    val channelIconAction = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_icon_action",
            context.resources.getString(R.string.pref_default_channel_icon_action))!!)

    if (channelIconAction == 1) {
        playSelectedChannel(context, channelId, isUnlocked)
    } else if (channelIconAction == 2) {
        if (context.getCastSession() != null) {
            castSelectedChannel(context, channelId)
        } else {
            playSelectedChannel(context, channelId, isUnlocked)
        }
    }
    return true
}

fun playOrCastRecording(context: Context, recordingId: Int, isUnlocked: Boolean): Boolean {
    val channelIconAction = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("channel_icon_action",
            context.resources.getString(R.string.pref_default_channel_icon_action))!!)

    if (channelIconAction == 1) {
        playSelectedRecording(context, recordingId, isUnlocked)
    } else if (channelIconAction == 2) {
        if (context.getCastSession() != null) {
            castSelectedRecording(context, recordingId)
        } else {
            playSelectedRecording(context, recordingId, isUnlocked)
        }
    }
    return true
}

fun searchTitleOnYoutube(context: Context, title: String?): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        try {
            // Search for the given title using the installed youtube application
            val intent = Intent(Intent.ACTION_SEARCH, Uri.parse("vnd.youtube:"))
            intent.putExtra("query", url)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

        } catch (ex: ActivityNotFoundException) {
            // No youtube app was found, use the browser
            val intent = Intent(Intent.ACTION_VIEW)
            intent.putExtra("query", url)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.data = Uri.parse("https://www.youtube.com/results?search_query=$url")
            context.startActivity(intent)
        }
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

fun searchTitleOnGoogle(context: Context, title: String?): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://www.google.com/search?q=$url")
        context.startActivity(intent)
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

fun searchTitleOnImdbWebsite(context: Context, title: String?): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///find?s=tt&q=$url"))
        try {
            context.startActivity(intent)
        } catch  (ex: ActivityNotFoundException) {
            intent.data = Uri.parse("https://www.imdb.com/find?s=tt&q=$url")
            context.startActivity(intent)
        }
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

fun searchTitleOnFileAffinityWebsite(context: Context, title: String?): Boolean {
    try {
        val url = URLEncoder.encode(title, "utf-8")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://www.filmaffinity.com/es/search.php?stext=$url")
        context.startActivity(intent)
    } catch (e: UnsupportedEncodingException) {
        // NOP
    }

    return true
}

fun searchTitleInTheLocalDatabase(activity: FragmentActivity, viewModel: BaseViewModel, title: String?, channelId: Int = 0): Boolean {
    if (!title.isNullOrEmpty()) {
        val newFragment: Fragment = ProgramListFragment.newInstance(channelId = channelId)
        activity.supportFragmentManager.beginTransaction().replace(R.id.main, newFragment).let {
            it.addToBackStack(null)
            it.commit()
        }
        viewModel.removeFragmentWhenSearchIsDone = true
        viewModel.startSearchQuery(title)
    }
    return true
}
