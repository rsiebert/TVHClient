/*
 *  Copyright (C) 2013 Robert Siebert
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuInflater;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Profiles;

import java.util.List;

public class SettingsProfilesFragment extends PreferenceFragment implements HTSListener, BackPressedInterface {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsProfilesFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;

    private Connection conn = null;
    private Profile progProfile = null;
    private Profile recProfile = null;

    private CheckBoxPreference prefEnableRecProfiles;
    private CheckBoxPreference prefEnableProgProfiles;
    private ListPreference prefRecProfiles;
    private ListPreference prefProgProfiles;

    private static final String PROG_PROFILE_UUID = "prog_profile_uuid";
    private static final String REC_PROFILE_UUID = "rec_profile_uuid";

    private TVHClientApplication app;
    private DatabaseHelper databaseHelper;
    private DataStorage dataStorage;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(PROG_PROFILE_UUID, prefProgProfiles.getValue());
        outState.putString(REC_PROFILE_UUID, prefRecProfiles.getValue());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        actionBarInterface = null;
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_profiles);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        app = (TVHClientApplication) activity.getApplication();
        databaseHelper = DatabaseHelper.getInstance(getActivity().getApplicationContext());
        dataStorage = DataStorage.getInstance();

        prefEnableRecProfiles = (CheckBoxPreference) findPreference("pref_enable_recording_profiles");
        prefEnableProgProfiles = (CheckBoxPreference) findPreference("pref_enable_playback_profiles");
        prefRecProfiles = (ListPreference) findPreference("pref_recording_profiles");
        prefProgProfiles = (ListPreference) findPreference("pref_playback_profiles");

        conn = databaseHelper.getSelectedConnection();
        progProfile = databaseHelper.getProfile(conn.playback_profile_id);
        if (progProfile == null) {
            progProfile = new Profile();
        }
        recProfile = databaseHelper.getProfile(conn.recording_profile_id);
        if (recProfile == null) {
            recProfile = new Profile();
        }

        // If the state is null then this activity has been started for
        // the first time. If the state is not null then the screen has
        // been rotated and we have to reuse the values.
        if (savedInstanceState != null) {
            progProfile.uuid = savedInstanceState.getString(PROG_PROFILE_UUID);
            recProfile.uuid = savedInstanceState.getString(REC_PROFILE_UUID);
        }

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.pref_profiles));
        }

        prefEnableRecProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prefRecProfiles.setEnabled(prefEnableRecProfiles.isChecked());
                return false;
            }
        });
        prefEnableProgProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prefProgProfiles.setEnabled(prefEnableProgProfiles.isChecked());
                return false;
            }
        });
    }

    public void onResume() {
        super.onResume();
        app.addListener(this);

        if (actionBarInterface != null) {
            actionBarInterface.setActionBarSubtitle(conn.name);
        }

        loadProfiles();
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_menu, menu);
    }

    private void save() {
        // Save the values into the program profile (play and streaming)
        progProfile.enabled = prefEnableProgProfiles.isChecked();
        progProfile.name = (prefProgProfiles.getEntry() != null ? prefProgProfiles.getEntry().toString() : "");
        progProfile.uuid = prefProgProfiles.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (progProfile.id == 0) {
            conn.playback_profile_id = (int) databaseHelper.addProfile(progProfile);
            databaseHelper.updateConnection(conn);
        } else {
            databaseHelper.updateProfile(progProfile);
        }

        // Save the values into the recording profile (recording)
        recProfile.enabled = prefEnableRecProfiles.isChecked();
        recProfile.name = (prefRecProfiles.getEntry() != null ? prefRecProfiles.getEntry().toString() : "");
        recProfile.uuid = prefRecProfiles.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (recProfile.id == 0) {
            conn.recording_profile_id = (int) databaseHelper.addProfile(recProfile);
            databaseHelper.updateConnection(conn);
        } else {
            databaseHelper.updateProfile(recProfile);
        }
    }

    /**
     * 
     */
    private void loadProfiles() {
        if (prefRecProfiles == null || prefProgProfiles == null) {
            return;
        }

        // Disable the preferences until the profiles have been loaded.
        // If the server does not support it then it stays disabled
        prefEnableRecProfiles.setEnabled(false);
        prefEnableProgProfiles.setEnabled(false);
        prefRecProfiles.setEnabled(false);
        prefProgProfiles.setEnabled(false);

        // Get the connection status to check if the profile can be loaded from
        // the server. If not show a message and return
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final String connectionStatus = prefs.getString(Constants.LAST_CONNECTION_STATE, "");
        if (!connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.err_connect, Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        // Set the loading indication
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarSubtitle(getString(R.string.loading_profiles));
        }

        // Get the available profiles from the server
        final Intent intent = new Intent(activity, HTSService.class);
        intent.setAction("getDvrConfigs");
        activity.startService(intent);
        intent.setAction("getProfiles");
        activity.startService(intent);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals("getDvrConfigs")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Loading is done, remove the loading subtitle
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarSubtitle(conn.name);
                    }

                    if (prefRecProfiles != null && prefEnableRecProfiles != null) {
                        addProfiles(prefRecProfiles, dataStorage.getDvrConfigs());

                        prefRecProfiles.setEnabled(prefEnableRecProfiles.isChecked());
                        prefEnableRecProfiles.setEnabled(true);

                        // If no uuid is set, no selected profile exists.
                        // Preselect the default one.
                        if (recProfile.uuid == null || recProfile.uuid.length() == 0) {
                            for (Profiles p : dataStorage.getDvrConfigs()) {
                                if (p.name.equals(Constants.REC_PROFILE_DEFAULT)) {
                                    recProfile.uuid = p.uuid;
                                    break;
                                }
                            }
                        }
                        // show the currently selected profile name, if none is
                        // available then the default value is used
                        prefRecProfiles.setValue(recProfile.uuid);
                    }
                }
            });
        } else if (action.equals("getProfiles")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Loading is done, remove the loading subtitle
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarSubtitle(conn.name);
                    }

                    if (prefProgProfiles != null && prefEnableProgProfiles != null) {
                        addProfiles(prefProgProfiles, dataStorage.getProfiles());
                        prefProgProfiles.setEnabled(prefEnableProgProfiles.isChecked());
                        prefEnableProgProfiles.setEnabled(true);

                        // If no uuid is set, no selected profile exists.
                        // Preselect the default one.
                        if (progProfile.uuid == null || progProfile.uuid.length() == 0) {
                            for (Profiles p : dataStorage.getProfiles()) {
                                if (p.name.equals(Constants.PROG_PROFILE_DEFAULT)) {
                                    progProfile.uuid = p.uuid;
                                    break;
                                }
                            }
                        }
                        // show the currently selected profile name, if none is
                        // available then the default value is used
                        prefProgProfiles.setValue(progProfile.uuid);
                    }
                }
            });
        }
    }

    /**
     * Adds the names and uuids of the available profiles to the preference widget
     *
     * @param preferenceList Preference list widget that shall hold the values
     * @param profileList Profile list with the data
     */
    private void addProfiles(ListPreference preferenceList, final List<Profiles> profileList) {
        // Initialize the arrays that contain the profile values
        final int size = profileList.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] entryValues = new CharSequence[size];

        // Add the available profiles to list preference
        for (int i = 0; i < size; i++) {
            entries[i] = profileList.get(i).name;
            entryValues[i] = profileList.get(i).uuid;
        }
        preferenceList.setEntries(entries);
        preferenceList.setEntryValues(entryValues);
    }

    @Override
    public void onBackPressed() {
        save();
    }
}
