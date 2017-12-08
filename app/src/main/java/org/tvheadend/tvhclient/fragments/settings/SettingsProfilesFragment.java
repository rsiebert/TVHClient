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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.SettingsToolbarInterface;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Profiles;

import java.util.List;

public class SettingsProfilesFragment extends PreferenceFragment implements OnPreferenceClickListener, HTSListener, BackPressedInterface {

    private Activity activity;
    private SettingsToolbarInterface toolbarInterface;

    private Connection connection = null;
    private Profile playbackProfile = null;
    private Profile recordingProfile = null;

    private CheckBoxPreference recordingProfileEnabledPreference;
    private CheckBoxPreference playbackProfileEnabledPreference;
    private ListPreference recordingProfileListPreference;
    private ListPreference playbackProfileListPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_profiles);

        activity = getActivity();
        if (activity instanceof SettingsToolbarInterface) {
            toolbarInterface = (SettingsToolbarInterface) activity;
        }

        connection = DatabaseHelper.getInstance(getActivity()).getSelectedConnection();
        toolbarInterface.setTitle(getString(R.string.pref_profiles));
        toolbarInterface.setSubtitle(connection.name);

        recordingProfileEnabledPreference = (CheckBoxPreference) findPreference("pref_enable_recording_profiles");
        playbackProfileEnabledPreference = (CheckBoxPreference) findPreference("pref_enable_playback_profiles");
        recordingProfileEnabledPreference.setOnPreferenceClickListener(this);
        playbackProfileEnabledPreference.setOnPreferenceClickListener(this);

        recordingProfileListPreference = (ListPreference) findPreference("pref_recording_profiles");
        playbackProfileListPreference = (ListPreference) findPreference("pref_playback_profiles");

        playbackProfile = DatabaseHelper.getInstance(getActivity()).getProfile(connection.playback_profile_id);
        recordingProfile = DatabaseHelper.getInstance(getActivity()).getProfile(connection.recording_profile_id);

        // Set defaults in case no profile was set for the current connection
        if (playbackProfile == null) {
            playbackProfile = new Profile();
        }
        if (recordingProfile == null) {
            recordingProfile = new Profile();
        }
        // Restore the currently selected uuids after an orientation change
        if (savedInstanceState != null) {
            playbackProfile.uuid = savedInstanceState.getString("playback_profile_uuid");
            recordingProfile.uuid = savedInstanceState.getString("recordind_profile_uuid");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("playback_profile_uuid", playbackProfileListPreference.getValue());
        outState.putString("recordind_profile_uuid", recordingProfileListPreference.getValue());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
        toolbarInterface.setSubtitle(getString(R.string.loading_profiles));
        loadRecordingProfiles();
        loadPlaybackProfiles();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_menu, menu);
    }

    private void saveRecordingProfile() {
        // Save the values into the recording profile (recording)
        recordingProfile.enabled = recordingProfileEnabledPreference.isChecked();
        recordingProfile.name = (recordingProfileListPreference.getEntry() != null ? recordingProfileListPreference.getEntry().toString() : "");
        recordingProfile.uuid = recordingProfileListPreference.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (recordingProfile.id == 0) {
            connection.recording_profile_id = (int) DatabaseHelper.getInstance(getActivity()).addProfile(recordingProfile);
            DatabaseHelper.getInstance(getActivity()).updateConnection(connection);
        } else {
            DatabaseHelper.getInstance(getActivity()).updateProfile(recordingProfile);
        }
    }

    private void savePlaybackProfile() {
        // Save the values into the program profile (play and streaming)
        playbackProfile.enabled = playbackProfileEnabledPreference.isChecked();
        playbackProfile.name = (playbackProfileListPreference.getEntry() != null ? playbackProfileListPreference.getEntry().toString() : "");
        playbackProfile.uuid = playbackProfileListPreference.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (playbackProfile.id == 0) {
            connection.playback_profile_id = (int) DatabaseHelper.getInstance(getActivity()).addProfile(playbackProfile);
            DatabaseHelper.getInstance(getActivity()).updateConnection(connection);
        } else {
            DatabaseHelper.getInstance(getActivity()).updateProfile(playbackProfile);
        }
    }

    private void loadRecordingProfiles() {
        // Disable the preferences until the profiles have been loaded.
        // If the server does not support it then it stays disabled
        recordingProfileEnabledPreference.setEnabled(false);
        recordingProfileListPreference.setEnabled(false);

        final Intent intent = new Intent(activity, HTSService.class);
        intent.setAction("getDvrConfigs");
        activity.startService(intent);

    }

    private void loadPlaybackProfiles() {
        // Disable the preferences until the profiles have been loaded.
        // If the server does not support it then it stays disabled
        playbackProfileEnabledPreference.setEnabled(false);
        playbackProfileListPreference.setEnabled(false);

        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction("getProfiles");
        activity.startService(intent);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals("getDvrConfigs")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Loading is done, remove the loading subtitle
                    toolbarInterface.setSubtitle(connection.name);

                    addProfiles(recordingProfileListPreference, DataStorage.getInstance().getDvrConfigs());
                    recordingProfileListPreference.setEnabled(recordingProfileEnabledPreference.isChecked());
                    recordingProfileEnabledPreference.setEnabled(true);

                    // If no uuid is set, no selected profile exists so preselect the default one.
                    if (recordingProfile.uuid == null || recordingProfile.uuid.length() == 0) {
                        for (Profiles p : DataStorage.getInstance().getDvrConfigs()) {
                            if (p.name.equals(Constants.REC_PROFILE_DEFAULT)) {
                                recordingProfile.uuid = p.uuid;
                                break;
                            }
                        }
                    }
                    // show the currently selected profile or the default name
                    recordingProfileListPreference.setValue(recordingProfile.uuid);

                }
            });
        } else if (action.equals("getProfiles")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Loading is done, remove the loading subtitle
                    toolbarInterface.setSubtitle(connection.name);

                    addProfiles(playbackProfileListPreference, DataStorage.getInstance().getProfiles());
                    playbackProfileListPreference.setEnabled(playbackProfileEnabledPreference.isChecked());
                    playbackProfileEnabledPreference.setEnabled(true);

                    // If no uuid is set, no selected profile exists so preselect the default one.
                    if (playbackProfile.uuid == null || playbackProfile.uuid.length() == 0) {
                        for (Profiles p : DataStorage.getInstance().getProfiles()) {
                            if (p.name.equals(Constants.PROG_PROFILE_DEFAULT)) {
                                playbackProfile.uuid = p.uuid;
                                break;
                            }
                        }
                    }
                    // show the currently selected profile or the default name
                    playbackProfileListPreference.setValue(playbackProfile.uuid);
                }
            });
        }
    }

    /**
     * Adds the names and uuids of the available profiles to the preference widget
     *
     * @param preferenceList Preference list widget that shall hold the values
     * @param profileList    Profile list with the data
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
        saveRecordingProfile();
        savePlaybackProfile();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "pref_enable_recording_profiles":
                recordingProfileListPreference.setEnabled(recordingProfileEnabledPreference.isChecked());
                return true;
            case "pref_enable_playback_profiles":
                playbackProfileListPreference.setEnabled(playbackProfileEnabledPreference.isChecked());
                return true;
        }
        return false;
    }
}
