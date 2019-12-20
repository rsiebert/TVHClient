package org.tvheadend.tvhclient.ui.features.settings

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import androidx.core.app.ActivityCompat
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber
import java.io.File

class SettingsFragment : BasePreferenceFragment(), Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private var downloadDirectoryPreference: Preference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        PreferenceManager.setDefaultValues(activity, R.xml.preferences, false)

        toolbarInterface.setTitle(getString(R.string.settings))
        toolbarInterface.setSubtitle("")

        findPreference<Preference>("list_connections")?.onPreferenceClickListener = this
        findPreference<Preference>("user_interface")?.onPreferenceClickListener = this
        findPreference<Preference>("profiles")?.onPreferenceClickListener = this
        findPreference<Preference>("playback")?.onPreferenceClickListener = this
        findPreference<Preference>("unlocker")?.onPreferenceClickListener = this
        findPreference<Preference>("advanced")?.onPreferenceClickListener = this
        findPreference<Preference>("changelog")?.onPreferenceClickListener = this
        findPreference<Preference>("language")?.onPreferenceClickListener = this
        findPreference<Preference>("light_theme_enabled")?.onPreferenceClickListener = this
        findPreference<Preference>("information")?.onPreferenceClickListener = this
        findPreference<Preference>("privacy_policy")?.onPreferenceClickListener = this

        downloadDirectoryPreference = findPreference("download_directory")
        downloadDirectoryPreference?.onPreferenceClickListener = this

        updateDownloadDirSummary()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun updateDownloadDirSummary() {
        val path = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        downloadDirectoryPreference?.summary = getString(R.string.pref_download_directory_sum, path)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions.isNotEmpty()
                && permissions[0] == "android.permission.READ_EXTERNAL_STORAGE") {
            // The delay is needed, otherwise an illegalStateException would be thrown. This is
            // a known bug in android. Until it is fixed this workaround is required.
            Handler().postDelayed({
                // Get the parent activity that implements the callback
                activity?.let {
                    // Show the folder chooser dialog which defaults to the external storage dir
                    MaterialDialog(it).show {
                        folderChooser { _, file ->
                            onFolderSelected(file)
                        }
                    }
                }
            }, 200)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        when (key) {
            "language" -> {
                activity?.let {
                    val intent = Intent(it, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    it.startActivity(intent)
                }
            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "light_theme_enabled" -> handlePreferenceThemeSelected()
            "profiles" -> handlePreferenceProfilesSelected()
            "playback" -> handlePreferencePlaybackSelected()
            "download_directory" -> handlePreferenceDownloadDirectorySelected()
            else -> settingsViewModel.setNavigationMenuId(preference.key)
        }
        return true
    }

    private fun handlePreferenceThemeSelected() {
        activity?.let {
            TaskStackBuilder.create(it)
                    .addNextIntent(Intent(it, MainActivity::class.java))
                    .addNextIntent(it.intent)
                    .startActivities()
        }
    }

    private fun handlePreferenceProfilesSelected() {
        if (view != null) {
            if (settingsViewModel.currentServerStatus.htspVersion < 16) {
                context?.sendSnackbarMessage(R.string.feature_not_supported_by_server)
            } else {
                settingsViewModel.setNavigationMenuId("profiles")
            }
        }
    }

    private fun handlePreferencePlaybackSelected() {
        if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
        } else {
            settingsViewModel.setNavigationMenuId("playback")
        }
    }

    private fun handlePreferenceDownloadDirectorySelected() {
        if (!isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
        } else {
            activity?.let {
                if (isReadPermissionGranted(it)) {
                    // Get the parent activity that implements the callback
                    MaterialDialog(it).show {
                        folderChooser { _, file ->
                            onFolderSelected(file)
                        }
                    }
                }
            }
        }
    }

    private fun onFolderSelected(file: File) {
        Timber.d("Folder ${file.absolutePath}, ${file.name} was selected")
        val strippedPath = file.absolutePath.replace(Environment.getExternalStorageDirectory().absolutePath, "")
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.edit().putString("download_directory", strippedPath).apply()

        updateDownloadDirSummary()
    }

    private fun isReadPermissionGranted(activity: FragmentActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                false
            }
        } else {
            true
        }
    }
}