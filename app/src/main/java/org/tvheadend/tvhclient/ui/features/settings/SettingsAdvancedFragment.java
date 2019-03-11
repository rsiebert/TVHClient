package org.tvheadend.tvhclient.ui.features.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.data.worker.LoadChannelIconWorker;
import org.tvheadend.tvhclient.domain.entity.Channel;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils;
import org.tvheadend.tvhclient.ui.features.search.SuggestionProvider;
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity;
import org.tvheadend.tvhclient.util.MiscUtils;
import org.tvheadend.tvhclient.util.logging.FileLoggingTree;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.core.content.FileProvider;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

public class SettingsAdvancedFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, DatabaseClearedCallback {

    private CheckBoxPreference notificationsEnabledPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getToolbarInterface().setTitle(getString(R.string.pref_advanced_settings));

        findPreference("debug_mode_enabled").setOnPreferenceClickListener(this);
        findPreference("send_debug_logfile_enabled").setOnPreferenceClickListener(this);
        findPreference("clear_database").setOnPreferenceClickListener(this);
        findPreference("clear_search_history").setOnPreferenceClickListener(this);
        findPreference("clear_icon_cache").setOnPreferenceClickListener(this);

        notificationsEnabledPreference = (CheckBoxPreference) findPreference("notifications_enabled");
        notificationsEnabledPreference.setOnPreferenceClickListener(this);
        notificationsEnabledPreference.setEnabled(isUnlocked());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey);
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "debug_mode_enabled":
                handlePreferenceDebugModeSelected();
                break;
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
        if (!isUnlocked()) {
            SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_available_in_free_version);
            notificationsEnabledPreference.setChecked(false);
        }
    }

    private void handlePreferenceClearDatabaseSelected() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.dialog_title_clear_database)
                .content(R.string.dialog_content_reconnect_to_server)
                .positiveText(R.string.clear)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    Timber.d("Clear database requested");

                    // Update the connection with the information that a new sync is required.
                    Connection connection = getAppRepository().getConnectionData().getActiveItem();
                    connection.setSyncRequired(true);
                    connection.setLastUpdate(0);
                    getAppRepository().getConnectionData().updateItem(connection);

                    // Clear the database contents, when done the callback
                    // is triggered which will restart the application
                    getAppRepository().getMiscData().clearDatabase(getActivity(), SettingsAdvancedFragment.this);
                    dialog.dismiss();
                })
                .onNegative((dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handlePreferenceDebugModeSelected() {
        if (getSharedPreferences().getBoolean("debug_mode_enabled", getResources().getBoolean(R.bool.pref_default_debug_mode_enabled))) {
            Timber.d("Debug mode is enabled");
            for (Timber.Tree tree : Timber.forest()) {
                if (tree.getClass().getName().equals(FileLoggingTree.class.getName())) {
                    Timber.d("FileLoggingTree already planted");
                    return;
                }
            }
            Timber.d("Replanting FileLoggingTree");
            Timber.plant(new FileLoggingTree(getActivity().getApplicationContext()));
        } else {
            Timber.d("Debug mode is disabled");
        }
    }

    private void handlePreferenceSendLogFileSelected() {
        // Get the list of available files in the log path
        File logPath = new File(getActivity().getCacheDir(), "logs");
        File[] files = logPath.listFiles();
        if (files == null) {
            new MaterialDialog.Builder(getActivity())
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
            new MaterialDialog.Builder(getActivity())
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
            File logFile = new File(getActivity().getCacheDir(), "logs/" + filename);
            fileUri = FileProvider.getUriForFile(getActivity(), "org.tvheadend.tvhclient.fileprovider", logFile);
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
        Timber.d("Preference " + key + " has changed");
        switch (key) {
            case "connection_timeout":
                try {
                    //noinspection ConstantConditions
                    int value = Integer.parseInt(prefs.getString(key, getResources().getString(R.string.pref_default_connection_timeout)));
                    if (value < 1) {
                        ((EditTextPreference) findPreference(key)).setText("1");
                        prefs.edit().putString(key, "1").apply();
                    }
                    if (value > 60) {
                        ((EditTextPreference) findPreference(key)).setText("60");
                        prefs.edit().putString(key, "60").apply();
                    }
                } catch (NumberFormatException ex) {
                    prefs.edit().putString(key, getResources().getString(R.string.pref_default_connection_timeout)).apply();
                }
                break;
        }
    }

    @Override
    public void onDatabaseCleared() {
        Timber.d("Database has been cleared, stopping service and restarting application");
        getActivity().stopService(new Intent(getActivity(), HtspService.class));
        Intent intent = new Intent(getActivity(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivity(intent);
        getActivity().finish();
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
                    SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.clear_search_history_done);
                }).show();
    }

    private void handlePreferenceClearIconCacheSelected() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.clear_icon_cache)
                .content(R.string.clear_icon_cache_sum)
                .positiveText(getString(R.string.delete))
                .negativeText(getString(R.string.cancel))
                .onPositive((dialog, which) -> {
                    // Delete all channel icon files that were downloaded for the active
                    // connection. Additionally remove the icons from the Picasso cache
                    Timber.d("Deleting channel icons and invalidating cache");
                    for (Channel channel : getAppRepository().getChannelData().getItems()) {
                        if (TextUtils.isEmpty(channel.getIcon())) {
                            continue;
                        }
                        String url = MiscUtils.getIconUrl(getActivity(), channel.getIcon());
                        File file = new File(url);
                        if (file.exists()) {
                            if (!file.delete()) {
                                Timber.d("Could not delete channel icon " + file.getName());
                            }
                        }
                        Picasso.get().invalidate(file);
                    }
                    SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.clear_icon_cache_done);

                    Timber.d("Starting background worker to reload channel icons");
                    OneTimeWorkRequest loadChannelIcons = new OneTimeWorkRequest.Builder(LoadChannelIconWorker.class).build();
                    WorkManager.getInstance().enqueueUniqueWork("LoadChannelIcons", ExistingWorkPolicy.REPLACE, loadChannelIcons);

                }).show();
    }
}
