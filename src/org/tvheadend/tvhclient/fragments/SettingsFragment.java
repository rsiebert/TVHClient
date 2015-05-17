package org.tvheadend.tvhclient.fragments;

import java.io.File;

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private final static String TAG = SettingsFragment.class.getSimpleName();
    
    private ActionBarActivity activity;
    private ActionBarInterface actionBarInterface;
    private SettingsInterface settingsInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the default values and then load the preferences from the XML resource
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);

        // Add a listener to the connection preference so that the 
        // SettingsManageConnectionsActivity can be shown.
        Preference prefManage = findPreference("pref_manage_connections");
        prefManage.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (settingsInterface != null) {
                    settingsInterface.showConnections();
                }
                return false;
            }
        });

        // Add a listener so that the streaming profiles can be selected.
        final Preference prefMenuProfiles = findPreference("pref_menu_profiles");
        if (prefMenuProfiles != null) {
            prefMenuProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (DatabaseHelper.getInstance().getConnections().isEmpty()) {
                        SnackbarManager.show(Snackbar.with(activity.getApplicationContext())
                                .type(SnackbarType.MULTI_LINE)
                                .text(R.string.no_connection_available_advice), activity);
                    } else if (DatabaseHelper.getInstance().getSelectedConnection() == null) {
                        SnackbarManager.show(Snackbar.with(activity.getApplicationContext())
                                .type(SnackbarType.MULTI_LINE)
                                .text(R.string.no_connection_active_advice), activity);
                    } else if (app.getProtocolVersion() < Constants.MIN_API_VERSION_PROFILES) {
                        SnackbarManager.show(Snackbar.with(activity.getApplicationContext())
                                .type(SnackbarType.MULTI_LINE)
                                .text(R.string.feature_not_supported_by_server), activity);
                    } else if (!app.isUnlocked()) {
                        SnackbarManager.show(Snackbar.with(activity.getApplicationContext())
                            .type(SnackbarType.MULTI_LINE)
                            .text(R.string.feature_not_available_in_free_version), activity);
                    } else {
                        if (settingsInterface != null) {
                            settingsInterface.showProfiles();
                        }
                    }
                    return false;
                }
            });
        }

        // Add a listener so that the transcoding parameters for the programs
        // and recordings can be set
        Preference prefMenuTranscoding = findPreference("pref_menu_transcoding");
        if (prefMenuTranscoding != null) {
            prefMenuTranscoding.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (DatabaseHelper.getInstance().getConnections().isEmpty()) {
                        Toast.makeText(activity, getString(R.string.no_connection_available_advice), Toast.LENGTH_SHORT).show();
                    } else if (DatabaseHelper.getInstance().getSelectedConnection() == null) {
                        Toast.makeText(activity, getString(R.string.no_connection_active_advice), Toast.LENGTH_SHORT).show();
                    } else {
                        if (settingsInterface != null) {
                            settingsInterface.showTranscodingSettings();
                        }
                    }
                    return false;
                }
            });
        }

        // Add a listener to the connection preference so that the 
        // ChangeLogDialog with all changes can be shown.
        Preference prefChangelog = findPreference("pref_changelog");
        prefChangelog.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final ChangeLogDialog cld = new ChangeLogDialog(getActivity());
                cld.getFullLogDialog().show();
                return false;
            }
        });
        
        // Add a listener to the preference so that the user can clear the search history
        Preference prefClearSearchHistory = findPreference("pref_clear_search_history");
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
                                SnackbarManager.show(Snackbar.with(activity.getApplicationContext())
                                        .type(SnackbarType.MULTI_LINE)
                                        .text(getString(R.string.clear_search_history_done)), activity);
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
        Preference prefClearIconCache = findPreference("pref_clear_icon_cache");
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
                                SnackbarManager.show(Snackbar.with(activity.getApplicationContext())
                                        .type(SnackbarType.MULTI_LINE)
                                        .text(getString(R.string.clear_icon_cache_done)), activity);
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
        Preference prefPurchaseUnlocker = findPreference("pref_unlocker");
        if (prefPurchaseUnlocker != null) {
            prefPurchaseUnlocker.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (app.isUnlocked()) {
                        Toast.makeText(getActivity(), getString(R.string.unlocker_already_purchased), Toast.LENGTH_SHORT).show();
                    } else {
                        Intent unlockerIntent = new Intent(activity, UnlockerActivity.class);
                        startActivity(unlockerIntent);
                    }
                    return false;
                }
            });
        }
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
            actionBarInterface.setActionBarTitle(getString(R.string.settings), TAG);
            actionBarInterface.setActionBarSubtitle("", TAG);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
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