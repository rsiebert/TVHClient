package org.tvheadend.tvhclient.ui.features.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.core.content.FileProvider
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.squareup.picasso.Picasso
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.data.worker.LoadChannelIconWorker
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import org.tvheadend.tvhclient.ui.features.search.SuggestionProvider
import org.tvheadend.tvhclient.util.getIconUrl
import org.tvheadend.tvhclient.util.logging.FileLoggingTree
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsAdvancedFragment : BasePreferenceFragment(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, DatabaseClearedCallback {

    private var notificationsEnabledPreference: CheckBoxPreference? = null
    private var notifyRunningRecordingCountEnabledPreference: CheckBoxPreference? = null
    private var notifyLowStorageSpaceEnabledPreference: CheckBoxPreference? = null
    private var connectionTimeoutPreference: EditTextPreference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarInterface.setTitle(getString(R.string.pref_advanced_settings))

        findPreference<Preference>("debug_mode_enabled")?.onPreferenceClickListener = this
        findPreference<Preference>("send_debug_logfile_enabled")?.onPreferenceClickListener = this
        findPreference<Preference>("clear_database")?.onPreferenceClickListener = this
        findPreference<Preference>("clear_search_history")?.onPreferenceClickListener = this
        findPreference<Preference>("clear_icon_cache")?.onPreferenceClickListener = this

        notificationsEnabledPreference = findPreference("notifications_enabled")
        notificationsEnabledPreference?.also {
            it.onPreferenceClickListener = this
            it.isEnabled = isUnlocked
        }

        notifyRunningRecordingCountEnabledPreference = findPreference("notify_running_recording_count_enabled")
        notifyRunningRecordingCountEnabledPreference?.also {
            it.onPreferenceClickListener = this
            it.isEnabled = isUnlocked
        }

        notifyLowStorageSpaceEnabledPreference = findPreference("notify_low_storage_space_enabled")
        notifyLowStorageSpaceEnabledPreference?.also {
            it.onPreferenceClickListener = this
            it.isEnabled = isUnlocked
        }

        connectionTimeoutPreference = findPreference("connection_timeout")
        connectionTimeoutPreference?.onPreferenceChangeListener = this
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "debug_mode_enabled" -> handlePreferenceDebugModeSelected()
            "send_debug_logfile_enabled" -> handlePreferenceSendLogFileSelected()
            "clear_database" -> handlePreferenceClearDatabaseSelected()
            "clear_search_history" -> handlePreferenceClearSearchHistorySelected()
            "clear_icon_cache" -> handlePreferenceClearIconCacheSelected()
            "notifications_enabled" -> handlePreferenceNotificationsSelected()
            "notify_running_recording_count_enabled" -> handlePreferenceNotifyRunningRecordingEnabledSelected()
            "notify_low_storage_space_enabled" -> handlePreferenceNotifyLowStorageSpaceSelected()
        }
        return true
    }

    private fun handlePreferenceNotificationsSelected() {
        if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
            notificationsEnabledPreference?.isChecked = false
        }
    }

    private fun handlePreferenceNotifyRunningRecordingEnabledSelected() {
        if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
            notifyRunningRecordingCountEnabledPreference?.isChecked = false
        }
    }

    private fun handlePreferenceNotifyLowStorageSpaceSelected() {
        if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
            notifyRunningRecordingCountEnabledPreference?.isChecked = false
        }
    }

    private fun handlePreferenceClearDatabaseSelected() {
        context?.let {
            MaterialDialog(it).show {
                title(R.string.dialog_title_clear_database)
                message(R.string.dialog_content_reconnect_to_server)
                positiveButton(R.string.clear) {
                    Timber.d("Clear database requested")
                    settingsViewModel.setSyncRequiredForActiveConnection()
                    settingsViewModel.clearDatabase(this@SettingsAdvancedFragment)
                    dismiss()
                }
                negativeButton(R.string.cancel) { dismiss() }
            }
        }
    }

    private fun handlePreferenceDebugModeSelected() {
        if (sharedPreferences.getBoolean("debug_mode_enabled", resources.getBoolean(R.bool.pref_default_debug_mode_enabled))) {
            Timber.d("Debug mode is enabled")
            for (tree in Timber.forest()) {
                if (tree.javaClass.name == FileLoggingTree::class.java.name) {
                    Timber.d("FileLoggingTree already planted")
                    return
                }
            }
            Timber.d("Replanting FileLoggingTree")
            activity?.applicationContext?.let {
                Timber.plant(FileLoggingTree(it))
            }
        } else {
            Timber.d("Debug mode is disabled")
        }
    }

    private fun handlePreferenceSendLogFileSelected() {
        // Get the list of available files in the log path
        context?.let {
            val logPath = File(it.cacheDir, "logs")
            val files = logPath.listFiles()
            if (files == null) {
                MaterialDialog(it).show {
                    title(R.string.select_log_file)
                    positiveButton(android.R.string.ok) { dismiss() }
                }
            } else {
                // Fill the items for the dialog
                val logfileList = ArrayList<String>()
                for (i in files.indices) {
                    logfileList.add(files[i].name)
                }
                // Show the dialog with the list of log files
                MaterialDialog(it).show {
                    title(R.string.select_log_file)
                    listItemsSingleChoice(items = logfileList, initialSelection = -1) { _, index, _ ->
                        mailLogfile(logfileList[index])
                    }
                }
            }
        }
    }

    private fun mailLogfile(filename: String?) {
        val date = Date()
        val sdf = SimpleDateFormat("dd.MM.yyyy HH.mm", Locale.US)
        val dateText = sdf.format(date.time)

        var fileUri: Uri? = null
        try {
            context?.let {
                val logFile = File(it.cacheDir, "logs/$filename")
                fileUri = FileProvider.getUriForFile(it, "org.tvheadend.tvhclient.fileprovider", logFile)
            }
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Could not load logfile")
        }

        if (fileUri != null) {
            // Create the intent with the email, some text and the log
            // file attached. The user can select from a list of
            // applications which he wants to use to send the mail
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.DEVELOPER_EMAIL))
            intent.putExtra(Intent.EXTRA_SUBJECT, "TVHClient Logfile")
            intent.putExtra(Intent.EXTRA_TEXT, "Logfile was sent on $dateText")
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.type = "text/plain"

            startActivity(Intent.createChooser(intent, "Send Log File to developer"))
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (preference == null) return false

        Timber.d("Preference ${preference.key} changed, checking if it is valid")
        when (preference.key) {
            "connection_timeout" ->
                try {
                    val value = Integer.valueOf(newValue as String)
                    if (value < 1 || value > 60) {
                        context?.sendSnackbarMessage("The value must be an integer between 1 and 60")
                        return false
                    }
                    return true
                } catch (ex: NumberFormatException) {
                    context?.sendSnackbarMessage("The value must be an integer between 1 and 24")
                    return false
                }
            else -> return true
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        Timber.d("Preference $key has changed")
        when (key) {
            "notify_running_recording_count_enabled" -> {
                if (!prefs.getBoolean(key, resources.getBoolean(R.bool.pref_default_notify_running_recording_count_enabled))) {
                    (activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
                }
            }
            "notify_low_storage_space_enabled" -> {
                if (!prefs.getBoolean(key, resources.getBoolean(R.bool.pref_default_notify_low_storage_space_enabled))) {
                    (activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(2)
                }
            }
        }
    }

    override fun onDatabaseCleared() {
        Timber.d("Database has been cleared, stopping service and restarting application")
        activity?.let {
            it.sendSnackbarMessage("Database contents cleared, reconnecting to server")
            it.stopService(Intent(activity, HtspService::class.java))
            settingsViewModel.setSyncRequiredForActiveConnection()
            val intent = Intent(activity, HtspService::class.java)
            intent.action = "connect"
            it.startService(intent)
        }
    }

    private fun handlePreferenceClearSearchHistorySelected() {
        context?.let {
            MaterialDialog(it).show {
                title(R.string.clear_search_history)
                message(R.string.clear_search_history_sum)
                positiveButton(R.string.delete) {
                    val suggestions = SearchRecentSuggestions(activity, SuggestionProvider.AUTHORITY, SuggestionProvider.MODE)
                    suggestions.clearHistory()
                    context.sendSnackbarMessage(R.string.clear_search_history_done)
                }
                negativeButton(R.string.cancel)

            }
        }
    }

    private fun handlePreferenceClearIconCacheSelected() {
        context?.let {
            MaterialDialog(it).show {
                title(R.string.clear_icon_cache)
                        .message(R.string.clear_icon_cache_sum)
                positiveButton(R.string.delete) { _ ->
                    // Delete all channel icon files that were downloaded for the active
                    // connection. Additionally remove the icons from the Glide cache
                    Timber.d("Deleting channel icons and invalidating cache")
                    for (channel in settingsViewModel.getChannelList()) {
                        if (channel.icon.isNullOrEmpty()) {
                            continue
                        }
                        val url = getIconUrl(it, channel.icon)
                        val file = File(url)
                        if (file.exists()) {
                            if (!file.delete()) {
                                Timber.d("Could not delete channel icon ${file.name}")
                            }
                        }
                        Picasso.get().invalidate(file)
                    }
                    context.sendSnackbarMessage(R.string.clear_icon_cache_done)

                    Timber.d("Starting background worker to reload channel icons")
                    val loadChannelIcons = OneTimeWorkRequest.Builder(LoadChannelIconWorker::class.java).build()
                    WorkManager.getInstance().enqueueUniqueWork("LoadChannelIcons", ExistingWorkPolicy.REPLACE, loadChannelIcons)

                }
                negativeButton(R.string.cancel)
            }
        }
    }
}
