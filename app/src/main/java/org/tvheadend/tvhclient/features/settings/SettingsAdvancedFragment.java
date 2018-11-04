package org.tvheadend.tvhclient.features.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.SearchRecentSuggestions;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.worker.EpgWorkerHandler;
import org.tvheadend.tvhclient.features.search.SuggestionProvider;
import org.tvheadend.tvhclient.features.startup.SplashActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.work.WorkManager;
import timber.log.Timber;

public class SettingsAdvancedFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, DatabaseClearedCallback {

    private CheckBoxPreference notificationsEnabledPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_advanced);
        toolbarInterface.setTitle(getString(R.string.pref_advanced_settings));

        findPreference("send_debug_logfile_enabled").setOnPreferenceClickListener(this);
        findPreference("clear_database").setOnPreferenceClickListener(this);
        findPreference("clear_search_history").setOnPreferenceClickListener(this);
        findPreference("clear_icon_cache").setOnPreferenceClickListener(this);

        notificationsEnabledPreference = (CheckBoxPreference) findPreference("notifications_enabled");
        notificationsEnabledPreference.setOnPreferenceClickListener(this);
        notificationsEnabledPreference.setEnabled(isUnlocked);
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "send_debug_logfile_enabled":
                handlePreferenceSendLogFileSelected();
                break;
            case "clear_database":
                handlePreferenceClearDatabaseSelected();
                break;
            case "clear_search_history":
                handlePreferenceClearSearchHistorySelected();
                break;
            case "clear_icon_cache":
                handlePreferenceClearIconCacheSelected();
                break;
            case "notifications":
                handlePreferenceNotificationsSelected();
                break;
        }
        return true;
    }

    private void handlePreferenceNotificationsSelected() {
        if (!isUnlocked) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version, Snackbar.LENGTH_SHORT).show();
                notificationsEnabledPreference.setChecked(false);
            }
        }
    }

    private void handlePreferenceClearDatabaseSelected() {
        new MaterialDialog.Builder(activity)
                .title(R.string.dialog_title_clear_database)
                .content(R.string.dialog_content_reconnect_to_server)
                .positiveText(R.string.clear)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    Timber.d("Clear database requested, stopping service and clearing database");

                    activity.stopService(new Intent(activity, EpgSyncService.class));
                    // Update the connection with the information that a new sync is required.
                    Connection connection = appRepository.getConnectionData().getActiveItem();
                    connection.setSyncRequired(true);
                    connection.setLastUpdate(0);
                    appRepository.getConnectionData().updateItem(connection);
                    // Clear the database contents, when done the callback
                    // is triggered which will restart the application
                    appRepository.getMiscData().clearDatabase(activity, SettingsAdvancedFragment.this);
                    dialog.dismiss();
                })
                .onNegative((dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handlePreferenceSendLogFileSelected() {
        // Get the list of available files in the log path
        File logPath = new File(activity.getCacheDir(), "logs");
        File[] files = logPath.listFiles();
        if (files == null) {
            new MaterialDialog.Builder(activity)
                    .title(R.string.select_log_file)
                    .onPositive((dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            // Fill the items for the dialog
            String[] logfileList = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                logfileList[i] = files[i].getName();
            }
            // Show the dialog with the list of log files
            new MaterialDialog.Builder(activity)
                    .title(R.string.select_log_file)
                    .items(logfileList)
                    .itemsCallbackSingleChoice(-1, (dialog, view, which, text) -> {
                        mailLogfile(logfileList[which]);
                        return true;
                    })
                    .show();
        }
    }

    private void mailLogfile(String filename) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm", Locale.US);
        String dateText = sdf.format(date.getTime());

        Uri fileUri = null;
        try {
            File logFile = new File(activity.getCacheDir(), "logs/" + filename);
            fileUri = FileProvider.getUriForFile(activity, "org.tvheadend.tvhclient.fileprovider", logFile);
        } catch (IllegalArgumentException e) {
            // NOP
        }

        if (fileUri != null) {
            // Create the intent with the email, some text and the log
            // file attached. The user can select from a list of
            // applications which he wants to use to send the mail
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{BuildConfig.DEVELOPER_EMAIL});
            intent.putExtra(Intent.EXTRA_SUBJECT, "TVHClient Logfile");
            intent.putExtra(Intent.EXTRA_TEXT, "Logfile was sent on " + dateText);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setType("text/plain");

            startActivity(Intent.createChooser(intent, "Send Log File to developer"));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "connectionTimeout":
                try {
                    int value = Integer.parseInt(prefs.getString(key, "5"));
                    if (value < 1) {
                        prefs.edit().putString(key, "1").apply();
                    }
                    if (value > 60) {
                        prefs.edit().putString(key, "60").apply();
                    }
                } catch (NumberFormatException ex) {
                    prefs.edit().putString(key, "5").apply();
                }
                break;
            case "epg_max_time":
                Timber.d("Preference " + key + " has changed, restarting background workers");
                WorkManager.getInstance().cancelAllWorkByTag(EpgWorkerHandler.WORKER_TAG);
                EpgWorkerHandler.startBackgroundWorkers(activity.getApplicationContext());
                break;
        }
    }

    @Override
    public void onDatabaseCleared() {
        Timber.d("Database has been cleared, restarting application");
        Intent intent = new Intent(activity, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
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
}
