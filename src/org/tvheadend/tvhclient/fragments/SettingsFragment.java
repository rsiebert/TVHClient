package org.tvheadend.tvhclient.fragments;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.PreferenceFragment;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.SuggestionProvider;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.UnlockerActivity;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private final static String TAG = SettingsFragment.class.getSimpleName();

    private ActionBarActivity activity;
    private ActionBarInterface actionBarInterface;
    private SettingsInterface settingsInterface;

    private Preference prefClearIconCache;
    private Preference prefPurchaseUnlocker;
    private Preference prefClearSearchHistory;
    private Preference prefManageConnections;
    private Preference prefMenuProfiles;
    private Preference prefMenuTranscoding;
    private Preference prefShowChangelog;
    private CheckBoxPreference prefDebugMode;
    private CheckBoxPreference prefShowNotifications;
    private ListPreference prefShowNotificationOffset;
    private Preference prefSendLogfile;
    private ListPreference prefDefaultMenu;

    private String[] logfileList;

    private TVHClientApplication app;
    private DatabaseHelper dbh;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the default values and then load the preferences from the XML resource
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);

        prefManageConnections = findPreference("pref_manage_connections");
        prefMenuProfiles = findPreference("pref_menu_profiles");
        prefMenuTranscoding = findPreference("pref_menu_transcoding");
        prefShowChangelog = findPreference("pref_changelog");
        prefClearSearchHistory = findPreference("pref_clear_search_history");
        prefClearIconCache = findPreference("pref_clear_icon_cache");
        prefDebugMode = (CheckBoxPreference) findPreference("pref_debug_mode");
        prefSendLogfile = findPreference("pref_send_logfile");
        prefPurchaseUnlocker = findPreference("pref_unlocker");
        prefDefaultMenu = (ListPreference) findPreference("defaultMenuPositionPref");
        prefShowNotifications = (CheckBoxPreference) findPreference("pref_show_notifications");
        prefShowNotificationOffset = (ListPreference) findPreference("pref_show_notification_offset");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
        List<String> menuEntries = new ArrayList<String>();
        List<String> menuEntryValues = new ArrayList<String>();

        for (int i = 0; i < e.length; i++) {
            if (i < 8 || (i == 3 && app.getProtocolVersion() >= Constants.MIN_API_VERSION_SERIES_RECORDINGS)
                    || (i == 4 && (app.getProtocolVersion() >= Constants.MIN_API_VERSION_TIMER_RECORDINGS && app.isUnlocked()))) {
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
                if (dbh.getConnections().isEmpty()) {
                    Snackbar.make(getView(), R.string.no_connection_available_advice, 
                            Snackbar.LENGTH_SHORT).show();
                } else if (dbh.getSelectedConnection() == null) {
                    Snackbar.make(getView(), R.string.no_connection_active_advice, 
                            Snackbar.LENGTH_SHORT).show();
                } else if (app.getProtocolVersion() < Constants.MIN_API_VERSION_PROFILES) {
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
                return false;
            }
        });

        // Add a listener so that the transcoding parameters for the programs
        // and recordings can be set
        prefMenuTranscoding.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
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
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(), SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
                                suggestions.clearHistory();
                                Snackbar.make(getView(), getString(R.string.clear_search_history_done), 
                                        Snackbar.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                // NOP
                            }
                        }).show();
                return false;
            }
        });

        // Add a listener to the preference so that the channel icon cache can be cleared.
        prefClearIconCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Show a confirmation dialog before clearing the icon cache
                new MaterialDialog.Builder(activity)
                        .title(R.string.clear_icon_cache)
                        .content(R.string.clear_icon_cache_sum)
                        .positiveText(getString(R.string.delete))
                        .negativeText(getString(R.string.cancel))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                File[] files = activity.getCacheDir().listFiles();
                                for (File file : files) {
                                    if (file.toString().endsWith(".png")) {
                                        file.delete();
                                        if (settingsInterface != null) {
                                            settingsInterface.reconnect();
                                        }
                                    }
                                }
                                Snackbar.make(getView(), getString(R.string.clear_icon_cache_done), 
                                        Snackbar.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                // NOP
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
                    Snackbar.make(getView(), getString(R.string.unlocker_already_purchased), 
                            Snackbar.LENGTH_SHORT).show();
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
                    app.enableLogToFile();
                } else {
                    app.disableLogToFile();
                }
                return false;
            }
        });

        // Add a listener to the user can send the internal log file to the
        // developer. He can then use the data for debugging purposes.
        prefSendLogfile.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                app.saveLog();

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
        prefShowNotifications.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!app.isUnlocked()) {
                    Snackbar.make(getView(), R.string.feature_not_available_in_free_version, 
                            Snackbar.LENGTH_SHORT).show();
                    prefShowNotifications.setChecked(false);
                }

                // If the checkbox is checked then add all 
                // required notifications, otherwise remove them
                if (prefShowNotifications.isChecked()) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                    final long offset = Integer.valueOf(prefs.getString("pref_show_notification_offset", "0"));
                    app.addNotifications(offset);
                } else {
                    app.cancelNotifications();
                }

                return true;
            }
        });

        prefShowNotificationOffset.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object offset) {
                // Refresh all notifications by removing adding them again
                app.cancelNotifications();
                app.addNotifications(Long.valueOf((String) offset));
                return true;
            }
        });
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
            Log.e(TAG, "The file can't be shared, " + e.getLocalizedMessage());
        }

        if (fileUri != null) {
            // Create the intent with the email, some text and the log 
            // file attached. The user can select from a list of 
            // applications which he wants to use to send the mail
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rsiebert80@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "TVHClient Logfile");
            intent.putExtra(Intent.EXTRA_TEXT, "Logfile was sent on " + dateText);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setType("text/plain");

            startActivity(Intent.createChooser(intent, "Send Log File to developer"));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
        app = (TVHClientApplication) activity.getApplication();
        dbh = DatabaseHelper.getInstance(activity);
    }

    @Override
    public void onDetach() {
        settingsInterface = null;
        actionBarInterface = null;
        super.onDetach();
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
        if (key.equals("lightThemePref") 
                || key.equals("languagePref")) {
            if (settingsInterface != null) {
                settingsInterface.restart();
                settingsInterface.restartNow();
            }
        } else if (key.equals("epgMaxDays")) {
            try {
                Integer.parseInt(prefs.getString(key, "7"));
            } catch (NumberFormatException ex) {
                prefs.edit().putString(key, "7").commit();
            }
            if (settingsInterface != null) {
                settingsInterface.restart();
            }
        } else if (key.equals("epgHoursVisible")) {
            try {
                Integer.parseInt(prefs.getString(key, "4"));
            } catch (NumberFormatException ex) {
                prefs.edit().putString(key, "4").commit();
            }
            if (settingsInterface != null) {
                settingsInterface.restart();
            }
        } else if (key.equals("connectionTimeout")) {
            try {
                int value = Integer.parseInt(prefs.getString(key, "5"));
                if (value < 1) {
                    prefs.edit().putString(key, "1").commit();
                }
                if (value > 60) {
                    prefs.edit().putString(key, "60").commit();
                }
            } catch (NumberFormatException ex) {
                prefs.edit().putString(key, "5").commit();
            }
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