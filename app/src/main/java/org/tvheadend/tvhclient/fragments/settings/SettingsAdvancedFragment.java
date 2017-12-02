package org.tvheadend.tvhclient.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.FileProvider;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsAdvancedFragment extends PreferenceFragment {
    private final static String TAG = SettingsAdvancedFragment.class.getSimpleName();

    private Activity activity;
    private ToolbarInterface toolbarInterface;
    private CheckBoxPreference prefDebugMode;
    private Preference prefSendLogfile;
    private Logger logger;
    private String[] logfileList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_advanced);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        if (toolbarInterface != null) {
            toolbarInterface.setActionBarTitle(getString(R.string.pref_advanced_settings));
        }

        logger = Logger.getInstance();

        prefDebugMode = (CheckBoxPreference) findPreference("pref_debug_mode");
        prefSendLogfile = findPreference("pref_send_logfile");

        // Add a listener to the logger will be enabled or disabled depending on the setting
        prefDebugMode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (prefDebugMode.isChecked()) {
                    logger.enableLogToFile();
                } else {
                    logger.disableLogToFile();
                }
                return false;
            }
        });

        // Add a listener to the user can send the internal log file to the
        // developer. He can then use the data for debugging purposes.
        prefSendLogfile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                logger.saveLog();

                // Get the list of available files in the log path
                File logPath = new File(activity.getCacheDir(), "logs");
                File[] files = logPath.listFiles();

                // Fill the items for the dialog
                logfileList = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    logfileList[i] = files[i].getName();
                }

                // Show the dialog with the list of log files
                new MaterialDialog.Builder(activity)
                        .title(R.string.select_log_file)
                        .items(logfileList)
                        .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                mailLogfile(logfileList[which]);
                                return true;
                            }
                        })
                        .show();
                return false;
            }
        });
    }

    private void mailLogfile(String filename) {
        logger.log(TAG, "mailLogfile() called with: filename = [" + filename + "]");

        // TODO sync logfile before sending?

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

    public void onDestroy() {
        toolbarInterface = null;
        super.onDestroy();
    }
}
