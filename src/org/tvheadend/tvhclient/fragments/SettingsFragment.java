package org.tvheadend.tvhclient.fragments;

import java.io.File;
import java.util.List;

import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.SettingsProfileActivity;
import org.tvheadend.tvhclient.SuggestionProvider;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;
import org.tvheadend.tvhclient.model.Connection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsFragment.class.getSimpleName();

    private Toolbar toolbar = null;
    private Activity activity;
    private SettingsInterface settingsInterface;
    int currentPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        // TODO hide the profiles preference for now 
        currentPreference = app.isUnlocked() ? R.xml.preferences : R.xml.preferences_hide_settings;

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
     * Adds a listener to the specified preferences so that the desired action
     * can be executed when the user has selected a preference.
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

        // Add a listener so that the genre colors can be enabled or not
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

        // Add a listener so that certain parameters of the program guide can be changed
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

        // Add a listener so that certain menus can be shown or hidden
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

        // Add a listener so that the profiles and play and recording options can be set
        Preference prefMenuProfiles = findPreference("pref_menu_profiles");
        if (prefMenuProfiles != null) {
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            if (!app.isUnlocked()) {
                prefMenuProfiles.setEnabled(false);
                prefMenuProfiles.setSummary(R.string.feature_not_available_in_free_version);
            } else {
                prefMenuProfiles.setSummary(R.string.pref_profiles_sum);
                prefMenuProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // Show a list of available connections
                        final List<Connection> connList = DatabaseHelper.getInstance().getConnections();
                        if (connList != null) {
                            String[] items = new String[connList.size()];
                            for (int i = 0; i < connList.size(); i++) {
                                items[i] = connList.get(i).name;
                            }
                            // Show a dialog to select a connection
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.select_connection).setItems(items,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // The 'which' argument contains the index of the selected item
                                            Intent intent = new Intent(activity, SettingsProfileActivity.class);
                                            intent.putExtra(Constants.BUNDLE_CONNECTION_ID, connList.get(which).id);
                                            startActivityForResult(intent, Constants.RESULT_CODE_PROFILES);
                                        }
                                    });
                            builder.create().show();
                        }
                        return false;
                    }
                });
            }
        }

        // Add a listener so that the dialog with all changes can be shown.
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

        // Add a listener so that the android market can be launched when the
        // user wants to buy the application.
        Preference prefUnlocker = findPreference("pref_unlocker");
        if (prefUnlocker != null) {
            prefUnlocker.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // TODO
                    return true;
                }
            });
        }

        // Add a listener so that the search history can be be cleared.
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

        // Add a listener so that the icon cache can be cleared.
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

    /**
     * This method is called when an activity has quit which was called with
     * startActivityForResult method. Depending on the given request and result
     * code certain action can be done.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_PROFILES) {
            if (resultCode == Activity.RESULT_OK) {
                if (settingsInterface != null) {
                    settingsInterface.reconnect();
                }
            }
        }
    }
}