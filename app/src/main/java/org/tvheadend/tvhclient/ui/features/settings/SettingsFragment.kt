package org.tvheadend.tvhclient.ui.features.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.DocumentsContract
import androidx.core.app.ActivityCompat
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.ui.features.settings.SettingsActivity.Companion.REQUEST_CODE_DIRECTORY_PICKER
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    lateinit var sharedPreferences: SharedPreferences
    lateinit var settingsViewModel: SettingsViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        PreferenceManager.setDefaultValues(activity, R.xml.preferences, false)
        settingsViewModel = ViewModelProviders.of(activity as SettingsActivity).get(SettingsViewModel::class.java)

        (activity as ToolbarInterface).let {
            it.setTitle(getString(R.string.settings))
            it.setSubtitle("")
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        findPreference<Preference>("list_connections")?.onPreferenceClickListener = this
        findPreference<Preference>("user_interface")?.onPreferenceClickListener = this
        findPreference<Preference>("profiles")?.onPreferenceClickListener = this
        findPreference<Preference>("playback")?.onPreferenceClickListener = this
        findPreference<Preference>("unlocker")?.onPreferenceClickListener = this
        findPreference<Preference>("advanced")?.onPreferenceClickListener = this
        findPreference<Preference>("changelog")?.onPreferenceClickListener = this
        findPreference<Preference>("language")?.onPreferenceClickListener = this
        findPreference<Preference>("selected_theme")?.onPreferenceClickListener = this
        findPreference<Preference>("information")?.onPreferenceClickListener = this
        findPreference<Preference>("privacy_policy")?.onPreferenceClickListener = this
        findPreference<Preference>("download_directory")?.onPreferenceClickListener = this
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateDownloadDirSummary()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun updateDownloadDirSummary() {
        val path = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        Timber.d("Updating download directory summary to $path")
        findPreference<Preference>("download_directory")?.summary = getString(R.string.pref_download_directory_sum, path)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions.isNotEmpty()
                && permissions[0] == "android.permission.READ_EXTERNAL_STORAGE") {
            // The delay is needed, otherwise an illegalStateException would be thrown. This is
            // a known bug in android. Until it is fixed this workaround is required.
            Handler().postDelayed({
                activity?.let { showFolderSelectionDialog(it) }
            }, 200)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        Timber.d("Shared preference $key has changed")
        when (key) {
            "selected_theme" -> handlePreferenceThemeChanged()
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
            "profiles" -> handlePreferenceProfilesSelected()
            "playback" -> handlePreferencePlaybackSelected()
            "download_directory" -> handlePreferenceDownloadDirectorySelected()
            else -> settingsViewModel.setNavigationMenuId(preference.key)
        }
        return true
    }

    private fun handlePreferenceThemeChanged() {
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
        if (!settingsViewModel.isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
        } else {
            settingsViewModel.setNavigationMenuId("playback")
        }
    }

    private fun handlePreferenceDownloadDirectorySelected() {
        if (!settingsViewModel.isUnlocked) {
            context?.sendSnackbarMessage(R.string.feature_not_available_in_free_version)
        } else {
            activity?.let {
                if (isReadPermissionGranted(it)) {
                    showFolderSelectionDialog(it)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun showFolderSelectionDialog(context: Context) {
        Timber.d("Showing folder selection dialog")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, showing legacy folder selection dialog")
            // Show the folder chooser dialog which defaults to the external storage dir
            MaterialDialog(context).show {
                folderChooser { _, file ->
                    Timber.d("Folder ${file.absolutePath}, ${file.name} was selected")
                    val strippedPath = file.absolutePath.replace(Environment.getExternalStorageDirectory().absolutePath, "")
                    sharedPreferences.edit().putString("download_directory", strippedPath).apply()
                }
            }
        } else {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, showing required selection dialog")
            // Choose a directory using the system's file picker.
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                // Provide read access to files and sub-directories in the user-selected directory.
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                // Optionally, specify a URI for the directory that should
                // be opened in the system file picker when it loads.
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
                }
            }
            activity?.startActivityForResult(intent, REQUEST_CODE_DIRECTORY_PICKER)
        }
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