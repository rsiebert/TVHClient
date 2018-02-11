package org.tvheadend.tvhclient.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.local.Logger;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsAdvancedFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity activity;
    private ToolbarInterface toolbarInterface;
    private CheckBoxPreference prefDebugMode;
    private SharedPreferences sharedPreferences;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_advanced);

        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        if (toolbarInterface != null) {
            toolbarInterface.setTitle(getString(R.string.pref_advanced_settings));
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        prefDebugMode = (CheckBoxPreference) findPreference("pref_debug_mode");
        Preference prefSendLogfile = findPreference("pref_send_logfile");
        prefDebugMode.setOnPreferenceClickListener(this);
        prefSendLogfile.setOnPreferenceClickListener(this);
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
            case "pref_debug_mode":
                handlePreferenceDebugModeEnabledSelected();
                break;
            case "pref_send_logfile":
                handlePreferenceSendLogFileSelected();
                break;
        }
        return true;
    }

    private void handlePreferenceDebugModeEnabledSelected() {
        if (prefDebugMode.isChecked()) {
            Logger.getInstance().enableLogToFile();
        } else {
            Logger.getInstance().disableLogToFile();
        }
    }

    private void handlePreferenceSendLogFileSelected() {
        Logger.getInstance().saveLog();
        // Get the list of available files in the log path
        File logPath = new File(getActivity().getCacheDir(), "logs");
        File[] files = logPath.listFiles();
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
}
