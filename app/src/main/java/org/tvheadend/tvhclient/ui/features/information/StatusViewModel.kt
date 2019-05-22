package org.tvheadend.tvhclient.ui.features.information

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.*
import timber.log.Timber
import javax.inject.Inject

class StatusViewModel : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val serverStatus: LiveData<ServerStatus>
    val connection: Connection
    val channelCount: LiveData<Int>
    val programCount: LiveData<Int>
    val timerRecordingCount: LiveData<Int>
    val seriesRecordingCount: LiveData<Int>
    val completedRecordingCount: LiveData<Int>
    val scheduledRecordingCount: LiveData<Int>
    val failedRecordingCount: LiveData<Int>
    val removedRecordingCount: LiveData<Int>

    val showRunningRecordingCount = MediatorLiveData<Boolean>()
    val showLowStorageSpace = MediatorLiveData<Boolean>()
    var runningRecordingCount = 0
    var availableStorageSpace = 0

    val subscriptions: LiveData<List<Subscription>>
    val inputs: LiveData<List<Input>>

    private lateinit var statusUpdateTask: Runnable
    private val statusUpdateHandler = Handler()

    init {
        Timber.d("Initializing")
        MainApplication.component.inject(this)

        serverStatus = appRepository.serverStatusData.liveDataActiveItem
        connection = appRepository.connectionData.activeItem
        channelCount = appRepository.channelData.getLiveDataItemCount()
        programCount = appRepository.programData.getLiveDataItemCount()
        timerRecordingCount = appRepository.timerRecordingData.getLiveDataItemCount()
        seriesRecordingCount = appRepository.seriesRecordingData.getLiveDataItemCount()
        completedRecordingCount = appRepository.recordingData.getLiveDataCountByType("completed")
        scheduledRecordingCount = appRepository.recordingData.getLiveDataCountByType("scheduled")
        failedRecordingCount = appRepository.recordingData.getLiveDataCountByType("failed")
        removedRecordingCount = appRepository.recordingData.getLiveDataCountByType("removed")

        subscriptions = appRepository.subscriptionData.getLiveDataItems()
        inputs = appRepository.inputData.getLiveDataItems()

        // Listen to changes of the recording count. If the count changes to zero or the setting
        // to show notifications is disabled, set the value to false to remove any notification
        showRunningRecordingCount.addSource(appRepository.recordingData.getLiveDataCountByType("running")) { count ->
            Timber.d("Running recording count has changed, checking if notification shall be shown")
            runningRecordingCount = count
            val enabled = sharedPreferences.getBoolean("notify_running_recording_count_enabled", appContext.resources.getBoolean(R.bool.pref_default_notify_running_recording_count_enabled))
            showRunningRecordingCount.value = enabled && count > 0
        }

        // Listen to changes of the server status especially the free storage space.
        // If the free space is above the threshold or the setting to show
        // notifications is disabled, set the value to false to remove any notification
        showLowStorageSpace.addSource(serverStatus) { serverStatus ->
            if (serverStatus != null) {
                Timber.d("Server status free space has changed, checking if notification shall be shown")
                availableStorageSpace = (serverStatus.freeDiskSpace / 1000000000).toInt()
                val enabled = sharedPreferences.getBoolean("notify_low_storage_space_enabled", appContext.resources.getBoolean(R.bool.pref_default_notify_low_storage_space_enabled))
                val threshold = Integer.valueOf(sharedPreferences.getString("low_storage_space_threshold", "1")!!)
                showLowStorageSpace.value = enabled && availableStorageSpace > threshold
            }
        }

        statusUpdateTask = Runnable {
            if (!MainApplication.isActivityVisible) {
                Timber.d("App is in the background, not starting service to get updated subscriptions, inputs and disk space ")
                return@Runnable
            }

            val intent = Intent(appContext, HtspService::class.java)
            intent.action = "getSubscriptions"
            appContext.startService(intent)
            intent.action = "getInputs"
            appContext.startService(intent)
            intent.action = "getDiskSpace"
            appContext.startService(intent)

            Timber.d("Restarting status update handler in 15s")
            statusUpdateHandler.postDelayed(statusUpdateTask, 15000)
        }

        Timber.d("Starting status update handler")
        statusUpdateHandler.post(statusUpdateTask)

        onSharedPreferenceChanged(sharedPreferences, "notify_running_recording_count_enabled")
        onSharedPreferenceChanged(sharedPreferences, "notify_low_storage_space_enabled")

        Timber.d("Registering shared preference change listener")
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        Timber.d("Unregistering shared preference change listener")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        Timber.d("Removing status update handler")
        statusUpdateHandler.removeCallbacks(statusUpdateTask)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        if (sharedPreferences == null) return
        when (key) {
            "notifications_enabled" -> {
                Timber.d("Setting has changed, checking if running recording count notification shall be shown")
                val enabled = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_notify_running_recording_count_enabled))
                showRunningRecordingCount.value = enabled && runningRecordingCount > 0
            }
            "notify_low_storage_space_enabled" -> {
                Timber.d("Setting has changed, checking if low storage space notification shall be shown")
                val enabled = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_notify_low_storage_space_enabled))
                val threshold = Integer.valueOf(sharedPreferences.getString("low_storage_space_threshold", "1")!!)
                showLowStorageSpace.value = enabled && availableStorageSpace > threshold
            }
        }
    }

    fun getChannelById(id: Int): Channel? {
        return appRepository.channelData.getItemById(id)
    }
}