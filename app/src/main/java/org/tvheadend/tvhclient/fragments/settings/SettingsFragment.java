package org.tvheadend.tvhclient.fragments.settings;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.SuggestionProvider;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.UnlockerActivity;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private AppCompatActivity activity;
    private ActionBarInterface actionBarInterface;
    private SettingsInterface settingsInterface;

    private Preference prefClearIconCache;
    private Preference prefPurchaseUnlocker;
    private Preference prefClearSearchHistory;
    private Preference prefManageConnections;
    private Preference prefMenuProfiles;
    private Preference prefMenuCasting;
    private Preference prefMenuUserInterface;
    private Preference prefMenuTranscoding;
    private Preference prefShowChangelog;
    private CheckBoxPreference prefDebugMode;
    private Preference prefMenuNotifications;
    private Preference prefDownloadDir;
    private Preference prefSendLogfile;
    private ListPreference prefDefaultMenu;

    private String[] logfileList;

    private TVHClientApplication app;
    private DatabaseHelper dbh;
    private Logger logger;
    private DataStorage ds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        app = (TVHClientApplication) activity.getApplication();
        dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        logger = Logger.getInstance();
        ds = DataStorage.getInstance();

        prefManageConnections = findPreference("pref_manage_connections");
        prefMenuProfiles = findPreference("pref_menu_profiles");
        prefMenuCasting = findPreference("pref_menu_casting");
        prefMenuUserInterface = findPreference("pref_menu_user_interface");
        prefMenuTranscoding = findPreference("pref_menu_transcoding");
        prefShowChangelog = findPreference("pref_changelog");
        prefClearSearchHistory = findPreference("pref_clear_search_history");
        prefClearIconCache = findPreference("pref_clear_icon_cache");
        prefDebugMode = (CheckBoxPreference) findPreference("pref_debug_mode");
        prefSendLogfile = findPreference("pref_send_logfile");
        prefPurchaseUnlocker = findPreference("pref_unlocker");
        prefDefaultMenu = (ListPreference) findPreference("defaultMenuPositionPref");
        prefMenuNotifications  = findPreference("pref_menu_notifications");
        prefDownloadDir = findPreference("pref_download_directory");

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof SettingsInterface) {
            settingsInterface = (SettingsInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.settings));
            actionBarInterface.setActionBarSubtitle("");
        }

        // Get the available menu names and id values and add only those entries
        // that are above the status menu and add the series and timer recording
        // menus only if these are supported by the server.
        final String[] e = getResources().getStringArray(R.array.pref_menu_names);
        final String[] ev = getResources().getStringArray(R.array.pref_menu_ids);
        List<String> menuEntries = new ArrayList<>();
        List<String> menuEntryValues = new ArrayList<>();

        for (int i = 0; i < e.length; i++) {
            if (i < 8 || (i == 3 && ds.getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS)
                    || (i == 4 && (ds.getProtocolVersion() >= Constants.MIN_API_VERSION_TIMER_RECORDINGS && app.isUnlocked()))) {
                menuEntries.add(e[i]);
                menuEntryValues.add(ev[i]);
            }
        }

        prefDefaultMenu.setEntries(menuEntries.toArray(new CharSequence[menuEntries.size()]));
        prefDefaultMenu.setEntryValues(menuEntryValues.toArray(new CharSequence[menuEntryValues.size()]));

        // Add a listener to the connection preference so that the 
        // SettingsManageConnectionsActivity can be shown.
        prefManageConnections.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (settingsInterface != null) {
                    settingsInterface.showConnections();
                }
                return false;
            }
        });

        // Add a listener so that the streaming profiles can be selected.
        prefMenuProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (getView() != null) {
                    if (dbh.getConnections().isEmpty()) {
                        Snackbar.make(getView(), R.string.no_connection_available_advice,
                                Snackbar.LENGTH_SHORT).show();
                    } else if (dbh.getSelectedConnection() == null) {
                        Snackbar.make(getView(), R.string.no_connection_active_advice,
                                Snackbar.LENGTH_SHORT).show();
                    } else if (ds.getProtocolVersion() < Constants.MIN_API_VERSION_PROFILES) {
                        Snackbar.make(getView(), R.string.feature_not_supported_by_server,
                                Snackbar.LENGTH_SHORT).show();
                    } else if (!app.isUnlocked()) {
                        Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                                Snackbar.LENGTH_SHORT).show();
                    } else {
                        if (settingsInterface != null) {
                            settingsInterface.showProfiles();
                        }
                    }
                }
                return false;
            }
        });

        // Add a listener so that the casting can be selected.
        prefMenuCasting.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (getView() != null) {
                    if (dbh.getConnections().isEmpty()) {
                        Snackbar.make(getView(), R.string.no_connection_available_advice,
                                Snackbar.LENGTH_SHORT).show();
                    } else if (dbh.getSelectedConnection() == null) {
                        Snackbar.make(getView(), R.string.no_connection_active_advice,
                                Snackbar.LENGTH_SHORT).show();
                    } else if (ds.getProtocolVersion() < Constants.MIN_API_VERSION_PROFILES) {
                        Snackbar.make(getView(), R.string.feature_not_supported_by_server,
                                Snackbar.LENGTH_SHORT).show();
                    } else if (!app.isUnlocked()) {
                        Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                                Snackbar.LENGTH_SHORT).show();
                    } else {
                        if (settingsInterface != null) {
                            settingsInterface.showCasting();
                        }
                    }
                }
                return false;
            }
        });

        // Add a listener so that the transcoding parameters for the programs
        // and recordings can be set
        prefMenuTranscoding.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (getView() != null) {
                    if (dbh.getConnections().isEmpty()) {
                        Snackbar.make(getView(), getString(R.string.no_connection_available_advice),
                                Snackbar.LENGTH_SHORT).show();
                    } else if (dbh.getSelectedConnection() == null) {
                        Snackbar.make(getView(), getString(R.string.no_connection_active_advice),
                                Snackbar.LENGTH_SHORT).show();
                    } else {
                        if (settingsInterface != null) {
                            settingsInterface.showTranscodingSettings();
                        }
                    }
                }
                return false;
            }
        });

        // Add a listener to the preference so that the user can clear the search history
        prefClearSearchHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Show a confirmation dialog before clearing the search history
                new MaterialDialog.Builder(activity)
                        .title(R.string.clear_search_history)
                        .content(R.string.clear_search_history_sum)
                        .positiveText(getString(R.string.delete))
                        .negativeText(getString(R.string.cancel))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(), SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
                                suggestions.clearHistory();
                                if (getView() != null) {
                                    Snackbar.make(getView(), getString(R.string.clear_search_history_done),
                                            Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        }).show();
                return false;
            }
        });

        // Add a listener to the preference so that the channel icon cache can be cleared.
        prefClearIconCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                logger.log(TAG, "onPreferenceClick: Deleting channel icons");
                // Show a confirmation dialog before clearing the icon cache
                new MaterialDialog.Builder(activity)
                        .title(R.string.clear_icon_cache)
                        .content(R.string.clear_icon_cache_sum)
                        .positiveText(getString(R.string.delete))
                        .negativeText(getString(R.string.cancel))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                File[] files = activity.getCacheDir().listFiles();
                                for (File file : files) {
                                    if (file.toString().endsWith(".png")) {
                                        if (!file.delete()) {
                                            logger.log(TAG, "onClick: Could not delete channel icon " + file.getName());
                                        }
                                        if (settingsInterface != null) {
                                            settingsInterface.reconnect();
                                        }
                                    }
                                }
                                if (getView() != null) {
                                    Snackbar.make(getView(), getString(R.string.clear_icon_cache_done),
                                            Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        }).show();
                return false;
            }
        });

        // Add a listener to the preference to show the activity with the
        // information about the extra features that can be unlocked
        prefPurchaseUnlocker.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (app.isUnlocked()) {
                    if (getView() != null) {
                        Snackbar.make(getView(), getString(R.string.unlocker_already_purchased),
                                Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Intent unlockerIntent = new Intent(activity, UnlockerActivity.class);
                    startActivity(unlockerIntent);
                }
                return false;
            }
        });

        // Add a listener to the connection preference so that the 
        // ChangeLogDialog with all changes can be shown.
        prefShowChangelog.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final ChangeLogDialog cld = new ChangeLogDialog(getActivity());
                cld.getFullLogDialog().show();
                return false;
            }
        });

        // Add a listener to the logger will be enabled or disabled depending on the setting
        prefDebugMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
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
        prefSendLogfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
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

        // Add a listener so that the notifications can be selected.
        prefMenuNotifications.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!app.isUnlocked()) {
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                                Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    if (settingsInterface != null) {
                        settingsInterface.showNotifications();
                    }
                }
                return false;
            }
        });

        // Add a listener so that the notifications can be selected.
        prefMenuUserInterface.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (settingsInterface != null) {
                    settingsInterface.showUserInterface();
                }
                return false;
            }
        });

        // Add a listener to the user can choose the directory
        // from the device where the recordings will be downloaded to.
        // In case the permissions for reading the external storage are not
        // given, a dilaog will be shown to the user to allow access
        prefDownloadDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!app.isUnlocked()) {
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                                Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    if (isReadPermissionGranted()) {
                        // Show the folder chooser dialog which defaults to the external storage dir
                        new FolderChooserDialog.Builder(activity).show(activity);
                    }
                }
                return false;
            }
        });
        updateDownloadDirSummary();
    }

    /**
     * Sets the current download folder in the preference summary text
     */
    public void updateDownloadDirSummary() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String path = prefs.getString("pref_download_directory", Environment.DIRECTORY_DOWNLOADS);
        prefDownloadDir.setSummary(getString(R.string.pref_download_directory_sum, path));
    }

    private boolean isReadPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions[0].equals("android.permission.READ_EXTERNAL_STORAGE")) {
            // The delay is needed, otherwise an illegalStateException would be thrown. This is
            // a known bug in android. Until it is fixed this unpretty workaround is required.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Show the folder chooser dialog which defaults to the external storage dir
                    new FolderChooserDialog.Builder(activity).show(activity);
                }
            }, 200);
        }
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

    @Override
    public void onDestroy() {
        settingsInterface = null;
        actionBarInterface = null;
        super.onDestroy();
    }

    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        // Close the menu dialog if it is visible to avoid crashing or showing
        // wrong values after an orientation. 
        if (prefDefaultMenu.getDialog() != null) {
            Dialog dlg = prefDefaultMenu.getDialog();
            if (dlg.isShowing()) {
                dlg.cancel();
            }
        }
    }

    /**
     * Show a notification to the user in case the theme or language
     * preference has changed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "languagePref":
                if (settingsInterface != null) {
                    settingsInterface.restart();
                    settingsInterface.restartNow();
                }
                break;
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
            case "pref_download_directory":
                updateDownloadDirSummary();
                break;
        }
        // Reload the data to fetch the channel icons. They are not loaded
        // (to save bandwidth) when not required.  
        if (key.equals("showIconPref")) {
            if (settingsInterface != null) {
                settingsInterface.reconnect();
            }
        }
    }
}