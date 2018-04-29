package org.tvheadend.tvhclient.features.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

public class SettingsAdvancedFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_advanced);
        toolbarInterface.setTitle(getString(R.string.pref_advanced_settings));

        Preference prefSendLogfile = findPreference("send_debug_logfile_enabled");
        Preference prefClearDatabase = findPreference("clear_database");
        prefSendLogfile.setOnPreferenceClickListener(this);
        prefClearDatabase.setOnPreferenceClickListener(this);
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
        }
        return true;
    }

    private void handlePreferenceClearDatabaseSelected() {
        new MaterialDialog.Builder(activity)
                .title("Clear database")
                .content("Do you really want to clear the database contents? You need to reconnect to the server.")
                .positiveText("Clear")
                .negativeText(activity.getString(R.string.cancel))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        AppRoomDatabase appDatabase = AppRoomDatabase.getInstance(activity.getApplicationContext());
                        try {
                            if (new ClearTablesAsyncTask(appDatabase).execute().get()
                                    && activity.getCurrentFocus() != null) {
                                Snackbar.make(activity.getCurrentFocus(), "Database cleared, please reconnect", Snackbar.LENGTH_SHORT).show();
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void handlePreferenceSendLogFileSelected() {
        // Get the list of available files in the log path
        File logPath = new File(activity.getCacheDir(), "logs");
        File[] files = logPath.listFiles();
        if (files == null) {
            new MaterialDialog.Builder(activity)
                    .title(R.string.select_log_file)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
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
        }
    }

    private static class ClearTablesAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private AppRoomDatabase db;

        ClearTablesAsyncTask(AppRoomDatabase db) {
            this.db = db;
            Timber.d("Removing all data from the database");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            db.getChannelDao().deleteAll();
            db.getChannelTagDao().deleteAll();
            db.getTagAndChannelDao().deleteAll();
            db.getProgramDao().deleteAll();
            db.getRecordingDao().deleteAll();
            db.getSeriesRecordingDao().deleteAll();
            db.getTimerRecordingDao().deleteAll();
            db.getServerStatusDao().deleteAll();
            Timber.d("Removed all data from the database");
            return true;
        }
    }
}
