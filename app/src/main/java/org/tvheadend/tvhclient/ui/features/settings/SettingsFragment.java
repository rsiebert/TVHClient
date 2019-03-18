package org.tvheadend.tvhclient.ui.features.settings;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.common.SnackbarUtils;
import org.tvheadend.tvhclient.ui.features.MainActivity;
import org.tvheadend.tvhclient.ui.features.changelog.ChangeLogActivity;
import org.tvheadend.tvhclient.ui.features.information.WebViewActivity;
import org.tvheadend.tvhclient.ui.features.unlocker.UnlockerActivity;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback, FolderChooserDialogCallback {

    private Preference downloadDirectoryPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        getToolbarInterface().setTitle(getString(R.string.settings));
        getToolbarInterface().setSubtitle("");

        findPreference("list_connections").setOnPreferenceClickListener(this);
        findPreference("user_interface").setOnPreferenceClickListener(this);
        findPreference("profiles").setOnPreferenceClickListener(this);
        findPreference("playback").setOnPreferenceClickListener(this);
        findPreference("unlocker").setOnPreferenceClickListener(this);
        findPreference("advanced").setOnPreferenceClickListener(this);
        findPreference("changelog").setOnPreferenceClickListener(this);
        findPreference("language").setOnPreferenceClickListener(this);
        findPreference("light_theme_enabled").setOnPreferenceClickListener(this);
        findPreference("information").setOnPreferenceClickListener(this);
        findPreference("privacy_policy").setOnPreferenceClickListener(this);

        downloadDirectoryPreference = findPreference("download_directory");
        downloadDirectoryPreference.setOnPreferenceClickListener(this);

        updateDownloadDirSummary();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateDownloadDirSummary() {
        final String path = getSharedPreferences().getString("download_directory", Environment.DIRECTORY_DOWNLOADS);
        downloadDirectoryPreference.setSummary(getString(R.string.pref_download_directory_sum, path));
    }

    private boolean isReadPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals("android.permission.READ_EXTERNAL_STORAGE")) {
            // The delay is needed, otherwise an illegalStateException would be thrown. This is
            // a known bug in android. Until it is fixed this workaround is required.
            new Handler().postDelayed(() -> {
                // Get the parent activity that implements the callback
                SettingsActivity settingsActivity = (SettingsActivity) getActivity();
                // Show the folder chooser dialog which defaults to the external storage dir
                new FolderChooserDialog.Builder(settingsActivity).show(settingsActivity);
            }, 200);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "language":
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getActivity().startActivity(intent);
                break;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "list_connections":
                showSelectedSettingsFragment("list_connections");
                break;
            case "user_interface":
                showSelectedSettingsFragment("user_interface");
                break;
            case "light_theme_enabled":
                handlePreferenceThemeSelected();
                break;
            case "profiles":
                handlePreferenceProfilesSelected();
                break;
            case "playback":
                handlePreferencePlaybackSelected();
                break;
            case "advanced":
                showSelectedSettingsFragment("advanced");
                break;
            case "unlocker":
                handlePreferenceUnlockerSelected();
                break;
            case "changelog":
                handlePreferenceChangelogSelected();
                break;
            case "information":
                handlePreferenceInformationSelected();
                break;
            case "privacy_policy":
                handlePreferencePrivacySelected();
                break;
            case "download_directory":
                handlePreferenceDownloadDirectorySelected();
                break;
        }
        return true;
    }

    private void handlePreferenceThemeSelected() {
        TaskStackBuilder.create(getActivity())
                .addNextIntent(new Intent(getActivity(), MainActivity.class))
                .addNextIntent(getActivity().getIntent())
                .startActivities();
    }

    private void handlePreferenceProfilesSelected() {
        if (getView() != null) {
            if (getHtspVersion() < 16) {
                SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_supported_by_server);
            } else {
                showSelectedSettingsFragment("profiles");
            }
        }
    }

    private void handlePreferencePlaybackSelected() {
        if (!isUnlocked()) {
            if (getView() != null) {
                SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_available_in_free_version);
            }
        } else {
            showSelectedSettingsFragment("playback");
        }
    }

    private void handlePreferenceUnlockerSelected() {
        Intent intent = new Intent(getActivity(), UnlockerActivity.class);
        startActivity(intent);
    }

    private void handlePreferenceChangelogSelected() {
        Intent intent = new Intent(getActivity(), ChangeLogActivity.class);
        intent.putExtra("showFullChangelog", true);
        startActivity(intent);
    }

    private void handlePreferenceInformationSelected() {
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.putExtra("website", "information");
        startActivity(intent);
    }

    private void handlePreferencePrivacySelected() {
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.putExtra("website", "privacy_policy");
        startActivity(intent);
    }

    private void showSelectedSettingsFragment(String settingType) {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        intent.putExtra("setting_type", settingType);
        startActivity(intent);
    }

    private void handlePreferenceDownloadDirectorySelected() {
        if (!isUnlocked()) {
            if (getView() != null) {
                SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_available_in_free_version);
            }
        } else {
            if (isReadPermissionGranted()) {
                // Get the parent activity that implements the callback
                SettingsActivity settingsActivity = (SettingsActivity) getActivity();
                new FolderChooserDialog.Builder(settingsActivity).show(settingsActivity);
            }
        }
    }

    @Override
    public void onFolderSelected(File folder) {
        String strippedPath = folder.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("download_directory", strippedPath).apply();

        updateDownloadDirSummary();
    }
}