package org.tvheadend.tvhclient.fragments;

import java.io.File;
import java.util.List;

import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.SuggestionProvider;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;
import org.tvheadend.tvhclient.model.Profiles;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsFragment.class.getSimpleName();

    private Toolbar toolbar = null;
    private Activity activity;
    private SettingsInterface settingsInterface;
    int currentPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentPreference = R.xml.preferences;
        Bundle bundle = getArguments();
        if (bundle != null) {
            currentPreference = bundle.getInt(Constants.BUNDLE_SETTINGS_PREFS);
        }

        PreferenceManager.setDefaultValues(getActivity(), currentPreference, false);
        addPreferencesFromResource(currentPreference);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (activity instanceof SettingsInterface) {
            settingsInterface = (SettingsInterface) activity;
        }

        toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.menu_settings);
            toolbar.setNavigationIcon((Utils.getThemeId(activity) == R.style.CustomTheme_Light) ? R.drawable.ic_menu_back_light
                    : R.drawable.ic_menu_back_dark);

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onBackPressed();
                }
            });
        }

        // Add the listeners to the each preference so the correct preference
        // file can be loaded for the selected fragment.
        addPreferenceListeners();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;
    }

    @Override
    public void onDetach() {
        settingsInterface = null;
        super.onDetach();
    }

    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Check if the general settings are currently shown
        if (currentPreference == R.xml.preferences) {
            ListPreference prefRecordingProfiles = (ListPreference) findPreference("pref_recording_profiles");
            ListPreference prefPlaybackProfiles = (ListPreference) findPreference("pref_playback_profiles");

            if (prefRecordingProfiles != null && prefPlaybackProfiles != null) {
                // Disable the settings until profiles have been loaded.
                // If the server does not support it then it stays disabled
                prefRecordingProfiles.setEnabled(false);
                prefPlaybackProfiles.setEnabled(false);

                prefRecordingProfiles.setSummary(R.string.loading_profile);
                prefPlaybackProfiles.setSummary(R.string.loading_profile);

                // Hide or show the available profile menu items depending on
                // the server version
                if (app.getProtocolVersion() > 15) {
                    // Get the available profiles from the server
                    final Intent intent = new Intent(activity, HTSService.class);
                    intent.setAction(Constants.ACTION_GET_DVR_CONFIG);
                    activity.startService(intent);

                    intent.setAction(Constants.ACTION_GET_PROFILES);
                    activity.startService(intent);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    /**
     * Show a notification to the user in case the theme or language preference
     * has changed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("lightThemePref") || key.equals("languagePref")) {
            if (settingsInterface != null) {
                settingsInterface.restart();
                settingsInterface.restartActivity();
            }
        } else if (key.equals("epgMaxDays") || key.equals("epgHoursVisible")) {
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

    /**
     * 
     */
    private void addPreferenceListeners() {
        Preference prefManage = findPreference("pref_manage_connections");
        if (prefManage != null) {
            prefManage.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (settingsInterface != null) {
                        settingsInterface.manageConnections();
                    }
                    return true;
                }
            });
        }

        Preference prefGenreColors = findPreference("pref_genre_colors");
        if (prefGenreColors != null) {
            prefGenreColors.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (settingsInterface != null) {
                        settingsInterface.showPreference(R.xml.preferences_genre_colors);
                    }
                    return true;
                }
            });
        }

        Preference prefProgramGuide = findPreference("pref_program_guide");
        if (prefProgramGuide != null) {
            prefProgramGuide.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (settingsInterface != null) {
                        settingsInterface.showPreference(R.xml.preferences_program_guide);
                    }
                    return true;
                }
            });
        }

        Preference prefMenuVisibility = findPreference("pref_menu_visibility");
        if (prefMenuVisibility != null) {
            prefMenuVisibility.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (settingsInterface != null) {
                        settingsInterface.showPreference(R.xml.preferences_menu_visibility);
                    }
                    return true;
                }
            });
        }

        Preference prefPlaybackPrograms = findPreference("pref_playback_programs");
        if (prefPlaybackPrograms != null) {
            prefPlaybackPrograms.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (settingsInterface != null) {
                        settingsInterface.showPreference(R.xml.preferences_playback_programs);
                    }
                    return true;
                }
            });
        }

        Preference prefPlaybackRecordings = findPreference("pref_playback_recordings");
        if (prefPlaybackRecordings != null) {
            prefPlaybackRecordings.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (settingsInterface != null) {
                        settingsInterface.showPreference(R.xml.preferences_playback_recordings);
                    }
                    return true;
                }
            });
        }
        // Add a listener to the connection preference so that the
        // ChangeLogDialog with all changes can be shown.
        Preference prefChangelog = findPreference("pref_changelog");
        if (prefChangelog != null) {
            prefChangelog.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final ChangeLogDialog cld = new ChangeLogDialog(getActivity());
                    cld.getFullLogDialog().show();
                    return true;
                }
            });
        }
        // Add a listener to the clear search history preference so that it can
        // be cleared.
        Preference prefClearSearchHistory = findPreference("pref_clear_search_history");
        if (prefClearSearchHistory != null) {
            prefClearSearchHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Show a confirmation dialog before deleting the recording
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.clear_search_history)
                            .setMessage(getString(R.string.clear_search_history_sum))
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
                                                    getActivity(), SuggestionProvider.AUTHORITY,
                                                    SuggestionProvider.MODE);
                                            suggestions.clearHistory();
                                            Toast.makeText(getActivity(),
                                                    getString(R.string.clear_search_history_done),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    })
                            .setNegativeButton(android.R.string.no,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // NOP
                                        }
                                    }).show();
                    return false;
                }
            });
        }
        // Add a listener to the clear icon cache preference so that it can be
        // cleared.
        Preference prefClearIconCache = findPreference("pref_clear_icon_cache");
        if (prefClearIconCache != null) {
            prefClearIconCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Show a confirmation dialog before deleting the recording
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.clear_icon_cache)
                            .setMessage(getString(R.string.clear_icon_cache_sum))
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            File[] files = activity.getCacheDir().listFiles();
                                            for (File file : files) {
                                                if (file.toString().endsWith(".png")) {
                                                    file.delete();
                                                    if (settingsInterface != null) {
                                                        settingsInterface.reconnect();
                                                    }
                                                }
                                            }
                                            Toast.makeText(getActivity(),
                                                    getString(R.string.clear_icon_cache_done),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    })
                            .setNegativeButton(android.R.string.no,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // NOP
                                        }
                                    }).show();
                    return false;
                }
            });
        }
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_GET_DVR_CONFIG)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ListPreference prefProfile = (ListPreference) findPreference("pref_recording_profiles");
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefProfile != null && currentPreference == R.xml.preferences && app.getProtocolVersion() > 15) {
                        addProfiles(prefProfile, app.getDvrConfigs());
                        prefProfile.setSummary(R.string.pref_recording_profiles_sum);
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_GET_PROFILES)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ListPreference prefProfile = (ListPreference) findPreference("pref_playback_profiles");
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefProfile != null && currentPreference == R.xml.preferences && app.getProtocolVersion() > 15) {
                        addProfiles(prefProfile, app.getProfiles());
                        prefProfile.setSummary(R.string.pref_playback_profiles_sum);

                        PreferenceScreen prefPlaybackPrograms = (PreferenceScreen) findPreference("pref_playback_programs");
                        if (prefPlaybackPrograms != null) {
                            prefPlaybackPrograms.setEnabled(false);
                        }
                        PreferenceScreen prefPlaybackRecordings = (PreferenceScreen) findPreference("pref_playback_recordings");
                        if (prefPlaybackRecordings != null) {
                            prefPlaybackRecordings.setEnabled(false);
                        }
                    }
                }
            });
        }
    }

    /**
     * 
     * @param listPref
     * @param profileList
     */
    protected void addProfiles(ListPreference listPref, final List<Profiles> profileList) {
        // Initialize the arrays that contain the profile values
        final int size = profileList.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] entryValues = new CharSequence[size];

        // Add the available profiles to list preference
        for (int i = 0; i < size; i++) {
            entries[i] = profileList.get(i).name;
            entryValues[i] = profileList.get(i).uuid;
        }
        listPref.setEntries(entries);
        listPref.setEntryValues(entryValues);

        // Enable the preference for use selection
        listPref.setEnabled(true);
    }
}