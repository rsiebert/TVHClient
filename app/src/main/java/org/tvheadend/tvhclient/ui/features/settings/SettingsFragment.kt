package org.tvheadend.tvhclient.ui.features.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.MainActivity
import org.tvheadend.tvhclient.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    lateinit var sharedPreferences: SharedPreferences
    lateinit var settingsViewModel: SettingsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences, false)
        settingsViewModel = ViewModelProvider(activity as SettingsActivity)[SettingsViewModel::class.java]

        (activity as ToolbarInterface).let {
            it.setTitle(getString(R.string.settings))
            it.setSubtitle("")
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

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
        Timber.d("Updating download directory summary")
        val path = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, loading download folder from preference")
            sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        } else {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, using default folder")
            Environment.DIRECTORY_DOWNLOADS
        }
        Timber.d("Setting download directory summary to $path")
        findPreference<Preference>("download_directory")?.summary = getString(R.string.pref_download_directory_sum, path)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions.isNotEmpty()
                && permissions[0] == "android.permission.READ_EXTERNAL_STORAGE") {
            // The delay is needed, otherwise an illegalStateException would be thrown. This is
            // a known bug in android. Until it is fixed this workaround is required.
            Handler(Looper.getMainLooper()).postDelayed({
                activity?.let { showFolderSelectionDialog(it) }
            }, 200)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        Timber.v("Shared preference $key has changed")
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

    private fun showFolderSelectionDialog(context: Context) {
        Timber.d("Showing folder selection dialog")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, showing folder selection dialog")
            // Show a simple folder chooser dialog which defaults to the external storage dir
            showFolderChooser(context, Environment.getExternalStorageDirectory())
        } else {
            Timber.d("Android API version is ${Build.VERSION.SDK_INT}, showing information about default folder")
            context.sendSnackbarMessage("On Android 10 and higher devices the download folder is always ${Environment.DIRECTORY_DOWNLOADS}")
        }
    }

    /**
     * Simple folder picker. Lists the sub folders of [directory] and navigates into them on
     * click. The positive button selects the current folder as the download directory.
     */
    private fun showFolderChooser(context: Context, directory: File) {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        val subFolders = (directory.listFiles { file -> file.isDirectory } ?: emptyArray())
                .sortedBy { it.name }

        val displayItems = ArrayList<String>()
        val targetFolders = ArrayList<File>()
        // Allow navigating to the parent folder, but not above the external storage root
        if (directory.absolutePath != rootPath) {
            directory.parentFile?.let {
                displayItems.add("..")
                targetFolders.add(it)
            }
        }
        for (folder in subFolders) {
            displayItems.add(folder.name)
            targetFolders.add(folder)
        }

        val builder = MaterialAlertDialogBuilder(context)
                .setTitle(directory.name.ifEmpty { directory.absolutePath })
                .setPositiveButton(R.string.select) { _, _ ->
                    Timber.d("Folder ${directory.absolutePath} was selected")
                    val strippedPath = directory.absolutePath.replace(Environment.getExternalStorageDirectory().absolutePath, "")
                    sharedPreferences.edit().putString("download_directory", strippedPath).apply()
                    updateDownloadDirSummary()
                }
                .setNegativeButton(R.string.cancel, null)
        if (displayItems.isNotEmpty()) {
            builder.setItems(displayItems.toTypedArray()) { _, which ->
                showFolderChooser(context, targetFolders[which])
            }
        }
        builder.show()
    }

    private fun isReadPermissionGranted(activity: FragmentActivity): Boolean {
        // The folder chooser is only shown on Android 9 (API 28) and lower; from Android 10 there
        // is no directory to pick. Reading external storage therefore only needs a runtime grant on
        // those legacy releases (READ_EXTERNAL_STORAGE is capped at API 32 in the manifest).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        return false
    }
}