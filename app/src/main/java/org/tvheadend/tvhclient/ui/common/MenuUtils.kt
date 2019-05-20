package org.tvheadend.tvhclient.ui.common


import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.*
import org.tvheadend.tvhclient.ui.features.channels.ChannelDisplayOptionListener
import org.tvheadend.tvhclient.ui.features.dvr.RecordingRemovedCallback
import org.tvheadend.tvhclient.ui.features.playback.external.CastChannelActivity
import org.tvheadend.tvhclient.ui.features.playback.external.CastRecordingActivity
import org.tvheadend.tvhclient.ui.features.playback.external.PlayChannelActivity
import org.tvheadend.tvhclient.ui.features.playback.external.PlayRecordingActivity
import org.tvheadend.tvhclient.ui.features.playback.internal.PlaybackActivity
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity
import org.tvheadend.tvhclient.util.isServerProfileEnabled
import timber.log.Timber
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MenuUtils(activity: Activity) {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val isUnlocked: Boolean
    private val activityReference: WeakReference<Activity>
    private val connection: Connection?
    private val serverStatus: ServerStatus
    private var htspVersion: Int = 13

    init {
        MainApplication.getComponent().inject(this)

        activityReference = WeakReference(activity)
        isUnlocked = MainApplication.getInstance().isUnlocked
        connection = appRepository.connectionData.activeItem
        serverStatus = appRepository.serverStatusData.activeItem
        htspVersion = serverStatus.htspVersion
    }

    fun handleMenuTimeSelection(currentSelection: Int, intervalInHours: Int, maxIntervalsToShow: Int, callback: ChannelDisplayOptionListener?): Boolean {
        val activity = activityReference.get() ?: return false

        val startDateFormat = SimpleDateFormat("dd.MM.yyyy - HH.00", Locale.US)
        val endDateFormat = SimpleDateFormat("HH.00", Locale.US)

        val times = ArrayList<String>()
        times.add(activity.getString(R.string.current_time))

        // Set the time that shall be shown next in the dialog. This is the
        // current time plus the value of the intervalInHours in milliseconds
        var timeInMillis = System.currentTimeMillis() + 1000 * 60 * 60 * intervalInHours

        // Add the date and time to the list. Remove Increase the time in
        // milliseconds for each iteration by the defined intervalInHours
        for (i in 1 until maxIntervalsToShow) {
            val startTime = startDateFormat.format(timeInMillis)
            timeInMillis += (1000 * 60 * 60 * intervalInHours).toLong()
            val endTime = endDateFormat.format(timeInMillis)
            times[i] = "$startTime - $endTime"
        }

        MaterialDialog(activity).show {
            title(R.string.select_time)
            listItemsSingleChoice(items = times, initialSelection = currentSelection) { _, index, _ -> callback?.onTimeSelected(index) }
        }
        return true
    }

    fun handleMenuChannelSortOrderSelection(callback: ChannelDisplayOptionListener): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }

        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", activity.resources.getString(R.string.pref_default_channel_sort_order))!!)
        MaterialDialog(activity).show {
            title(R.string.select_dvr_config)
            listItemsSingleChoice(R.array.pref_sort_channels_names, initialSelection = channelSortOrder) { _, index, _ ->
                Timber.d("New selected channel sort order changed from $channelSortOrder to $index")
                val editor = sharedPreferences.edit()
                editor.putString("channel_sort_order", index.toString())
                editor.apply()
                callback.onChannelSortOrderSelected(index)
            }
        }
        return false
    }

    fun handleMenuRecordSelection(eventId: Int): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        Timber.d("handleMenuRecordSelection() called with: eventId = [$eventId]")
        val intent = Intent(activity, HtspService::class.java)
        intent.action = "addDvrEntry"
        intent.putExtra("eventId", eventId)

        val profile = appRepository.serverProfileData.getItemById(serverStatus.recordingServerProfileId)
        if (profile != null && isServerProfileEnabled(profile, htspVersion)) {
            intent.putExtra("configName", profile.name)
        }
        activity.startService(intent)
        return true
    }

    fun handleMenuSeriesRecordSelection(title: String?): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        val intent = Intent(activity, HtspService::class.java)
        intent.action = "addAutorecEntry"
        intent.putExtra("title", title)

        val profile = appRepository.serverProfileData.getItemById(serverStatus.recordingServerProfileId)
        if (profile != null && isServerProfileEnabled(profile, htspVersion)) {
            intent.putExtra("configName", profile.name)
        }
        activity.startService(intent)
        return true
    }

    fun handleMenuStopRecordingSelection(recording: Recording?, callback: RecordingRemovedCallback?): Boolean {
        val activity = activityReference.get()
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        Timber.d("Stopping recording ${recording.title}")
        // Show a confirmation dialog before stopping the recording
        MaterialDialog(activity).show {
            title(R.string.record_stop)
            message(text = activity.getString(R.string.stop_recording, recording.title))
            negativeButton(R.string.cancel)
            positiveButton(R.string.stop) {
                val intent = Intent(activity, HtspService::class.java)
                intent.action = "stopDvrEntry"
                intent.putExtra("id", recording.id)
                activity.startService(intent)
                callback?.onRecordingRemoved()
            }
        }
        return true
    }

    fun handleMenuRemoveRecordingSelection(recording: Recording?, callback: RecordingRemovedCallback?): Boolean {
        val activity = activityReference.get()
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        Timber.d("Removing recording ${recording.title}")
        // Show a confirmation dialog before removing the recording
        MaterialDialog(activity).show {
            title(R.string.record_remove)
            message(text = activity.getString(R.string.remove_recording, recording.title))
            negativeButton(R.string.cancel)
            positiveButton(R.string.remove) {
                val intent = Intent(activity, HtspService::class.java)
                intent.action = "deleteDvrEntry"
                intent.putExtra("id", recording.id)
                activity.startService(intent)
                callback?.onRecordingRemoved()
            }
        }
        return true
    }

    fun handleMenuCancelRecordingSelection(recording: Recording?, callback: RecordingRemovedCallback?): Boolean {
        val activity = activityReference.get()
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        Timber.d("Cancelling recording ${recording.title}")
        // Show a confirmation dialog before cancelling the recording
        MaterialDialog(activity).show {
            title(R.string.record_remove)
            message(text = activity.getString(R.string.cancel_recording, recording.title))
            negativeButton(R.string.cancel)
            positiveButton(R.string.remove) {
                val intent = Intent(activity, HtspService::class.java)
                intent.action = "cancelDvrEntry"
                intent.putExtra("id", recording.id)
                activity.startService(intent)
                callback?.onRecordingRemoved()
            }
        }
        return true
    }

    fun handleMenuRemoveSeriesRecordingSelection(recording: SeriesRecording?, callback: RecordingRemovedCallback?): Boolean {
        val activity = activityReference.get()
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        Timber.d("Removing series recording ${recording.title}")
        // Show a confirmation dialog before removing the recording
        MaterialDialog(activity).show {
            title(R.string.record_remove)
            message(text = activity.getString(R.string.remove_series_recording, recording.title))
            negativeButton(R.string.cancel)
            positiveButton(R.string.remove) {
                val intent = Intent(activity, HtspService::class.java)
                intent.action = "deleteAutorecEntry"
                intent.putExtra("id", recording.id)
                activity.startService(intent)
                callback?.onRecordingRemoved()
            }
        }
        return true
    }

    fun handleMenuRemoveTimerRecordingSelection(recording: TimerRecording?, callback: RecordingRemovedCallback?): Boolean {
        val activity = activityReference.get()
        if (activity == null || recording == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }

        val recordingName = recording.name ?: ""
        val name = if (recordingName.isNotEmpty()) recordingName else ""
        val displayTitle = if (name.isNotEmpty()) name else recording.title ?: ""
        Timber.d("Removing timer recording $displayTitle")

        // Show a confirmation dialog before removing the recording
        MaterialDialog(activity).show {
            title(R.string.record_remove)
            message(text = activity.getString(R.string.remove_timer_recording, displayTitle))
            negativeButton(R.string.cancel)
            positiveButton(R.string.remove) {
                val intent = Intent(activity, HtspService::class.java)
                intent.action = "deleteTimerecEntry"
                intent.putExtra("id", recording.id)
                activity.startService(intent)
                callback?.onRecordingRemoved()
            }
        }
        return true
    }

    fun handleMenuRemoveAllRecordingsSelection(items: List<Recording>): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        MaterialDialog(activity).show {
            title(R.string.record_remove_all)
            message(R.string.confirm_remove_all)
            negativeButton(R.string.cancel)
            positiveButton(R.string.remove) {
                object : Thread() {
                    override fun run() {
                        for (item in items) {
                            val intent = Intent(activity, HtspService::class.java)
                            intent.putExtra("id", item.id)
                            if (item.isRecording || item.isScheduled) {
                                intent.action = "cancelDvrEntry"
                            } else {
                                intent.action = "deleteDvrEntry"
                            }
                            activity.startService(intent)
                            try {
                                sleep(500)
                            } catch (e: InterruptedException) {
                                // NOP
                            }

                        }
                    }
                }.start()
            }
        }
        return true
    }

    fun handleMenuRemoveAllSeriesRecordingSelection(items: List<SeriesRecording>): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        MaterialDialog(activity).show {
            title(R.string.record_remove_all)
            message(R.string.remove_all_recordings)
            negativeButton(R.string.cancel)
            positiveButton(R.string.remove) {
                object : Thread() {
                    override fun run() {
                        for ((id) in items) {
                            val intent = Intent(activity, HtspService::class.java)
                            intent.action = "deleteAutorecEntry"
                            intent.putExtra("id", id)
                            activity.startService(intent)
                            try {
                                sleep(500)
                            } catch (e: InterruptedException) {
                                // NOP
                            }

                        }
                    }
                }.start()
            }
        }
        return true
    }

    fun handleMenuRemoveAllTimerRecordingSelection(items: List<TimerRecording>): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }

        MaterialDialog(activity).show {
            title(R.string.record_remove_all)
            message(R.string.remove_all_recordings)
            negativeButton(R.string.cancel)
            positiveButton(R.string.remove) {
                object : Thread() {
                    override fun run() {
                        for ((id) in items) {
                            val intent = Intent(activity, HtspService::class.java)
                            intent.action = "deleteTimerecEntry"
                            intent.putExtra("id", id)
                            activity.startService(intent)
                            try {
                                sleep(500)
                            } catch (e: InterruptedException) {
                                // NOP
                            }

                        }
                    }
                }.start()
            }
        }
        return true
    }

    fun handleMenuCustomRecordSelection(eventId: Int, channelId: Int): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }

        val dvrConfigList = appRepository.serverProfileData.recordingProfileNames

        // Get the selected recording profile to highlight the
        // correct item in the list of the selection dialog
        var dvrConfigNameValue = 0

        val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.recordingServerProfileId)
        if (serverProfile != null) {
            for (i in dvrConfigList.indices) {
                if (dvrConfigList[i] == serverProfile.name) {
                    dvrConfigNameValue = i
                    break
                }
            }
        }
        // Create the dialog to show the available profiles
        MaterialDialog(activity).show {
            title(R.string.select_dvr_config)
            listItemsSingleChoice(items = dvrConfigList.toList(), initialSelection = dvrConfigNameValue) { _, index, _ ->
                val intent = Intent(activity, HtspService::class.java)
                intent.action = "addDvrEntry"
                intent.putExtra("eventId", eventId)
                intent.putExtra("channelId", channelId)
                intent.putExtra("configName", dvrConfigList[index])
                activity.startService(intent)
            }
        }
        return true
    }

    fun handleMenuReconnectSelection(): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }

        MaterialDialog(activity).show {
            title(R.string.dialog_title_reconnect_to_server)
            message(R.string.dialog_content_reconnect_to_server)
            negativeButton(R.string.cancel)
            positiveButton(R.string.reconnect) {
                Timber.d("Reconnect requested, stopping service and updating active connection to require a full sync")
                activity.stopService(Intent(activity, HtspService::class.java))

                if (connection != null) {
                    Timber.d("Updating active connection to request a full sync")
                    connection.isSyncRequired = true
                    connection.lastUpdate = 0
                    appRepository.connectionData.updateItem(connection)
                } else {
                    val msg = "Reconnect requested, trying to get active connection from database returned no entry"
                    Timber.e(msg)
                    if (Fabric.isInitialized()) {
                        Crashlytics.logException(Exception(msg))
                    }
                }
                // Finally restart the application to show the startup fragment
                val intent = Intent(activity, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.startActivity(intent)
            }
        }
        return true
    }

    fun handleMenuPlayChannel(channelId: Int): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }

        if (isUnlocked && sharedPreferences.getBoolean("internal_player_for_channels_enabled",
                        activity.resources.getBoolean(R.bool.pref_default_internal_player_enabled))) {
            val intent = Intent(activity, PlaybackActivity::class.java)
            intent.putExtra("channelId", channelId)
            activity.startActivity(intent)
        } else {
            val intent = Intent(activity, PlayChannelActivity::class.java)
            intent.putExtra("channelId", channelId)
            activity.startActivity(intent)
        }
        return true
    }

    fun handleMenuPlayRecording(dvrId: Int): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        if (isUnlocked && sharedPreferences.getBoolean("internal_player_for_recordings_enabled",
                        activity.resources.getBoolean(R.bool.pref_default_internal_player_enabled))) {
            val intent = Intent(activity, PlaybackActivity::class.java)
            intent.putExtra("dvrId", dvrId)
            activity.startActivity(intent)
        } else {
            val intent = Intent(activity, PlayRecordingActivity::class.java)
            intent.putExtra("dvrId", dvrId)
            activity.startActivity(intent)
        }
        return true
    }

    fun handleMenuCast(type: String, id: Int): Boolean {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return false
        }
        val intent: Intent
        when (type) {
            "dvrId" -> {
                intent = Intent(activity, CastRecordingActivity::class.java)
                intent.putExtra("dvrId", id)
                activity.startActivity(intent)
                return true
            }
            "channelId" -> {
                intent = Intent(activity, CastChannelActivity::class.java)
                intent.putExtra("channelId", id)
                activity.startActivity(intent)
                return true
            }
        }
        return false
    }

    fun handleMenuPlayChannelIcon(channelId: Int) {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return
        }


        val channelIconAction = Integer.valueOf(sharedPreferences.getString("channel_icon_action", activity.resources.getString(R.string.pref_default_channel_icon_action))!!)
        if (channelIconAction == 1) {
            handleMenuPlayChannel(channelId)
        } else if (channelIconAction == 2) {
            if (getCastSession(activity) != null) {
                handleMenuCast("channelId", channelId)
            } else {
                handleMenuPlayChannel(channelId)
            }
        }
    }

    fun handleMenuPlayRecordingIcon(recordingId: Int) {
        val activity = activityReference.get()
        if (activity == null) {
            Timber.d("Weak reference to activity is null")
            return
        }


        val channelIconAction = Integer.valueOf(sharedPreferences.getString("channel_icon_action", activity.resources.getString(R.string.pref_default_channel_icon_action))!!)
        if (channelIconAction == 1) {
            handleMenuPlayRecording(recordingId)
        } else if (channelIconAction == 2) {
            if (getCastSession(activity) != null) {
                handleMenuCast("dvrId", recordingId)
            } else {
                handleMenuPlayRecording(recordingId)
            }
        }
    }
}
