package org.tvheadend.tvhclient.features.settings;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.changelog.ChangeLogActivity;
import org.tvheadend.tvhclient.features.navigation.NavigationActivity;
import org.tvheadend.tvhclient.features.search.SuggestionProvider;

import java.io.File;

import timber.log.Timber;

public class SettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback, FolderChooserDialogCallback {

    private Preference downloadDirectoryPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
        toolbarInterface.setTitle(getString(R.string.settings));
        toolbarInterface.setSubtitle("");

        Preference manageConnectionsPreference = findPreference("list_connections");
        Preference userInterfacePreference = findPreference("user_interface");
        Preference notificationsPreference = findPreference("notifications");
        Preference profilesPreference = findPreference("profiles");
        Preference castingPreference = findPreference("casting");
        Preference transcodingPreference = findPreference("transcoding");
        Preference unlockerPreference = findPreference("unlocker");
        Preference advancedPreference = findPreference("advanced");
        Preference changelogPreference = findPreference("changelog");
        Preference languagePreference = findPreference("language");
        Preference clearSearchHistoryPreference = findPreference("clear_search_history");
        Preference clearIconCachePreference = findPreference("clear_icon_cache");
        downloadDirectoryPreference = findPreference("download_directory");

        manageConnectionsPreference.setOnPreferenceClickListener(this);
        userInterfacePreference.setOnPreferenceClickListener(this);
        notificationsPreference.setOnPreferenceClickListener(this);
        profilesPreference.setOnPreferenceClickListener(this);
        castingPreference.setOnPreferenceClickListener(this);
        transcodingPreference.setOnPreferenceClickListener(this);
        advancedPreference.setOnPreferenceClickListener(this);
        unlockerPreference.setOnPreferenceClickListener(this);
        changelogPreference.setOnPreferenceClickListener(this);
        languagePreference.setOnPreferenceClickListener(this);
        downloadDirectoryPreference.setOnPreferenceClickListener(this);
        clearIconCachePreference.setOnPreferenceClickListener(this);
        clearSearchHistoryPreference.setOnPreferenceClickListener(this);

        updateDownloadDirSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateDownloadDirSummary() {
        final String path = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS);
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
            // a known bug in android. Until it is fixed this unpretty workaround is required.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Get the parent activity that implements the callback
                    SettingsActivity activity = (SettingsActivity) getActivity();
                    // Show the folder chooser dialog which defaults to the external storage dir
                    new FolderChooserDialog.Builder(getActivity()).show(activity);
                }
            }, 200);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "language":
                Intent intent = new Intent(getActivity(), NavigationActivity.class);
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
            case "notifications":
                handlePreferenceNotificationsSelected();
                break;
            case "profiles":
                handlePreferenceProfilesSelected();
                break;
            case "casting":
                handlePreferenceCastingSelected();
                break;
            case "transcoding":
                showSelectedSettingsFragment("transcoding");
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
            case "download_directory":
                handlePreferenceDownloadDirectorySelected();
                break;
            case "clear_search_history":
                handlePreferenceClearSearchHistorySelected();
                break;
            case "clear_icon_cache":
                handlePreferenceClearIconCacheSelected();
                break;
        }
        return true;
    }

    private void handlePreferenceNotificationsSelected() {
        if (!isUnlocked) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            showSelectedSettingsFragment("notifications");
        }
    }

    private void handlePreferenceProfilesSelected() {
        if (getView() != null) {
            if (htspVersion < 16) {
                Snackbar.make(getView(), R.string.feature_not_supported_by_server, Snackbar.LENGTH_SHORT).show();
            } else {
                showSelectedSettingsFragment("profiles");
            }
        }
    }

    private void handlePreferenceCastingSelected() {
        if (getView() != null) {
            if (htspVersion < 16) {
                Snackbar.make(getView(), R.string.feature_not_supported_by_server, Snackbar.LENGTH_SHORT).show();
            } else if (!isUnlocked) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version, Snackbar.LENGTH_SHORT).show();
            } else {
                showSelectedSettingsFragment("casting");
            }
        }
    }

    private void handlePreferenceUnlockerSelected() {
        if (isUnlocked) {
            if (getView() != null) {
                Snackbar.make(getView(), getString(R.string.unlocker_already_purchased), Snackbar.LENGTH_SHORT).show();
            }
        } else {
            showSelectedSettingsFragment("unlocker");
        }
    }

    private void handlePreferenceChangelogSelected() {
        Intent intent = new Intent(getActivity(), ChangeLogActivity.class);
        intent.putExtra("showFullChangelog", true);
        startActivity(intent);
    }

    private void showSelectedSettingsFragment(String settingType) {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        intent.putExtra("setting_type", settingType);
        startActivity(intent);
    }

    private void handlePreferenceDownloadDirectorySelected() {
        if (!isUnlocked) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            if (isReadPermissionGranted()) {
                // Get the parent activity that implements the callback
                SettingsActivity activity = (SettingsActivity) getActivity();
                new FolderChooserDialog.Builder(getActivity()).show(activity);
            }
        }
    }

    private void handlePreferenceClearSearchHistorySelected() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.clear_search_history)
                .content(R.string.clear_search_history_sum)
                .positiveText(getString(R.string.delete))
                .negativeText(getString(R.string.cancel))
                .onPositive((dialog, which) -> {
                    SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(), SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
                    suggestions.clearHistory();
                    if (getView() != null) {
                        Snackbar.make(getView(), getString(R.string.clear_search_history_done), Snackbar.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void handlePreferenceClearIconCacheSelected() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.clear_icon_cache)
                .content(R.string.clear_icon_cache_sum)
                .positiveText(getString(R.string.delete))
                .negativeText(getString(R.string.cancel))
                .onPositive((dialog, which) -> {
                    File[] files = getActivity().getCacheDir().listFiles();
                    for (File file : files) {
                        if (file.toString().endsWith(".png")) {
                            if (!file.delete()) {
                                Timber.d("Could not delete channel icon " + file.getName());
                            }
                        }
                    }
                    if (getView() != null) {
                        Snackbar.make(getView(), getString(R.string.clear_icon_cache_done), Snackbar.LENGTH_SHORT).show();
                    }
                }).show();
    }

    @Override
    public void onFolderSelected(File folder) {
        String strippedPath = folder.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("download_directory", strippedPath).apply();

        updateDownloadDirSummary();
    }
}