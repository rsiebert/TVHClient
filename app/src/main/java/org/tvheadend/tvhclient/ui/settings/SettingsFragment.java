package org.tvheadend.tvhclient.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.local.SuggestionProvider;
import org.tvheadend.tvhclient.data.repository.ServerStatusRepository;
import org.tvheadend.tvhclient.ui.misc.ChangeLogActivity;
import org.tvheadend.tvhclient.ui.navigation.NavigationActivity;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;

import java.io.File;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback, FolderChooserDialogCallback {
    private final static String TAG = SettingsFragment.class.getSimpleName();

    private ToolbarInterface toolbarInterface;
    private Preference prefDownloadDir;
    private boolean isUnlocked;
    private int htspVersion;
    private SharedPreferences sharedPreferences;

    // TODO remove selection of start menu entries timer and series rec

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        toolbarInterface.setTitle(getString(R.string.settings));
        toolbarInterface.setSubtitle("");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();
        htspVersion = new ServerStatusRepository(activity).loadServerStatusSync().getHtspVersion();

        Preference prefManageConnections = findPreference("pref_manage_connections");
        Preference prefMenuUserInterface = findPreference("pref_menu_user_interface");
        Preference prefMenuNotifications = findPreference("pref_menu_notifications");
        Preference prefMenuProfiles = findPreference("pref_menu_profiles");
        Preference prefMenuCasting = findPreference("pref_menu_casting");
        Preference prefMenuTranscoding = findPreference("pref_menu_transcoding");
        Preference prefPurchaseUnlocker = findPreference("pref_unlocker");
        Preference prefMenuAdvanced = findPreference("pref_advanced");
        Preference prefShowChangelog = findPreference("pref_changelog");
        Preference prefLanguage = findPreference("languagePref");
        Preference prefClearSearchHistory = findPreference("pref_clear_search_history");
        Preference prefClearIconCache = findPreference("pref_clear_icon_cache");
        prefDownloadDir = findPreference("pref_download_directory");

        prefManageConnections.setOnPreferenceClickListener(this);
        prefMenuUserInterface.setOnPreferenceClickListener(this);
        prefMenuNotifications.setOnPreferenceClickListener(this);
        prefMenuProfiles.setOnPreferenceClickListener(this);
        prefMenuCasting.setOnPreferenceClickListener(this);
        prefMenuTranscoding.setOnPreferenceClickListener(this);
        prefMenuAdvanced.setOnPreferenceClickListener(this);
        prefPurchaseUnlocker.setOnPreferenceClickListener(this);
        prefShowChangelog.setOnPreferenceClickListener(this);
        prefLanguage.setOnPreferenceClickListener(this);
        prefDownloadDir.setOnPreferenceClickListener(this);
        prefClearIconCache.setOnPreferenceClickListener(this);
        prefClearSearchHistory.setOnPreferenceClickListener(this);

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
        final String path = sharedPreferences.getString("pref_download_directory", Environment.DIRECTORY_DOWNLOADS);
        prefDownloadDir.setSummary(getString(R.string.pref_download_directory_sum, path));
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
            case "languagePref":
                Intent intent = new Intent(getActivity(), NavigationActivity.class);
                getActivity().startActivity(intent);
                break;
            case "defaultMenuPositionPref":
                // TODO show message and revert to default if not supported
                break;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "pref_manage_connections":
                showSelectedSettingsFragment("list_connections");
                break;
            case "pref_menu_user_interface":
                showSelectedSettingsFragment("user_interface");
                break;
            case "pref_menu_notifications":
                handlePreferenceNotificationsSelected();
                break;
            case "pref_menu_profiles":
                handlePreferenceProfilesSelected();
                break;
            case "pref_menu_casting":
                handlePreferenceCastingSelected();
                break;
            case "pref_menu_transcoding":
                showSelectedSettingsFragment("transcoding");
                break;
            case "pref_advanced":
                showSelectedSettingsFragment("advanced");
                break;
            case "pref_unlocker":
                handlePreferenceUnlockerSelected();
                break;
            case "pref_changelog":
                handlePreferenceChangelogSelected();
                break;
            case "pref_download_directory":
                handlePreferenceDownloadDirectorySelected();
                break;
            case "pref_clear_search_history":
                handlePreferenceClearSearchHistorySelected();
                break;
            case "pref_clear_icon_cache":
                handlePreferenceClearIconCacheSelected();
                break;
        }
        return true;
    }

    private void handlePreferenceNotificationsSelected() {
        if (!isUnlocked) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            showSelectedSettingsFragment("notifications");
        }
    }

    private void handlePreferenceProfilesSelected() {
        if (getView() != null) {
            if (htspVersion < 16) {
                Snackbar.make(getView(), R.string.feature_not_supported_by_server,
                        Snackbar.LENGTH_SHORT).show();
            } else if (!isUnlocked) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                showSelectedSettingsFragment("profiles");
            }
        }
    }

    private void handlePreferenceCastingSelected() {
        if (getView() != null) {
            if (htspVersion < 16) {
                Snackbar.make(getView(), R.string.feature_not_supported_by_server,
                        Snackbar.LENGTH_SHORT).show();
            } else if (!isUnlocked) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                        Snackbar.LENGTH_SHORT).show();
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
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                        Snackbar.LENGTH_SHORT).show();
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
                        Snackbar.make(getView(), getString(R.string.clear_search_history_done),
                                Snackbar.LENGTH_SHORT).show();
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
                                Log.d(TAG, "onClick: Could not delete channel icon " + file.getName());
                            }
                        }
                    }
                    if (getView() != null) {
                        // TODO show dialog that the user needs to reconnect manually if he wants to see icons again
                        Snackbar.make(getView(), getString(R.string.clear_icon_cache_done),
                                Snackbar.LENGTH_SHORT).show();
                    }
                }).show();
    }

    @Override
    public void onFolderSelected(File folder) {
        String strippedPath = folder.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("pref_download_directory", strippedPath).apply();

        updateDownloadDirSummary();
    }
}