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
package org.tvheadend.tvhclient.ui.settings;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.repository.ConnectionDataRepository;
import org.tvheadend.tvhclient.data.repository.ProfileDataRepository;
import org.tvheadend.tvhclient.data.repository.ServerDataRepository;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;

import java.util.List;

public class SettingsProfilesFragment extends PreferenceFragment implements OnPreferenceClickListener, BackPressedInterface {

    private ToolbarInterface toolbarInterface;
    private ServerProfile playbackServerProfile = null;
    private ServerProfile recordingServerProfile = null;
    private CheckBoxPreference recordingProfileEnabledPreference;
    private CheckBoxPreference playbackProfileEnabledPreference;
    private ListPreference recordingProfileListPreference;
    private ListPreference playbackProfileListPreference;
    private ServerDataRepository serverRepository;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_profiles);

        Activity activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        Connection connection = new ConnectionDataRepository(activity).getActiveConnectionSync();
        toolbarInterface.setTitle(getString(R.string.pref_profiles));
        toolbarInterface.setSubtitle(connection.getName());

        recordingProfileEnabledPreference = (CheckBoxPreference) findPreference("pref_enable_recording_profiles");
        playbackProfileEnabledPreference = (CheckBoxPreference) findPreference("pref_enable_playback_profiles");
        recordingProfileEnabledPreference.setOnPreferenceClickListener(this);
        playbackProfileEnabledPreference.setOnPreferenceClickListener(this);

        recordingProfileListPreference = (ListPreference) findPreference("pref_recording_profiles");
        playbackProfileListPreference = (ListPreference) findPreference("pref_playback_profiles");

        ProfileDataRepository profileRepository = new ProfileDataRepository(activity);
        playbackServerProfile = profileRepository.getPlaybackServerProfile();
        recordingServerProfile = profileRepository.getRecordingServerProfile();

        addProfiles(playbackProfileListPreference, profileRepository.getAllPlaybackServerProfiles());
        addProfiles(recordingProfileListPreference, profileRepository.getAllRecordingServerProfiles());

        serverRepository = new ServerDataRepository(activity);

        // Restore the currently selected uuids after an orientation change
        if (savedInstanceState != null) {
            playbackServerProfile.setUuid(savedInstanceState.getString("playback_profile_uuid"));
            recordingServerProfile.setUuid(savedInstanceState.getString("recordind_profile_uuid"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("playback_profile_uuid", playbackProfileListPreference.getValue());
        outState.putString("recordind_profile_uuid", recordingProfileListPreference.getValue());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_options_menu, menu);
    }

    private void saveRecordingProfile() {
        // Save the values into the recording profile (recording)
        recordingServerProfile.setEnabled(recordingProfileEnabledPreference.isChecked());
        recordingServerProfile.setName((recordingProfileListPreference.getEntry() != null ? recordingProfileListPreference.getEntry().toString() : ""));
        recordingServerProfile.setUuid(recordingProfileListPreference.getValue());

        serverRepository.updateRecordingServerProfile(recordingServerProfile.getId());
    }

    private void savePlaybackProfile() {
        // Save the values into the program profile (play and streaming)
        playbackServerProfile.setEnabled(playbackProfileEnabledPreference.isChecked());
        playbackServerProfile.setName((playbackProfileListPreference.getEntry() != null ? playbackProfileListPreference.getEntry().toString() : ""));
        playbackServerProfile.setUuid(playbackProfileListPreference.getValue());

        serverRepository.updatePlaybackServerProfile(playbackServerProfile.getId());
    }

    @Override
    public void onBackPressed() {
        saveRecordingProfile();
        savePlaybackProfile();
        getActivity().finish();
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

    private void addProfiles(final ListPreference listPreference, final List<ServerProfile> serverProfileList) {
        // Initialize the arrays that contain the profile values
        final int size = serverProfileList.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] entryValues = new CharSequence[size];

        // Add the available profiles to list preference
        for (int i = 0; i < size; i++) {
            entries[i] = serverProfileList.get(i).getName();
            entryValues[i] = serverProfileList.get(i).getUuid();
        }
        listPreference.setEntries(entries);
        listPreference.setEntryValues(entryValues);
    }
}
