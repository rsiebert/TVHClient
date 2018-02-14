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

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;

import java.util.List;

public class SettingsProfilesFragment extends PreferenceFragment implements BackPressedInterface {

    private ToolbarInterface toolbarInterface;
    private ListPreference recordingProfileListPreference;
    private ListPreference playbackProfileListPreference;
    private ConfigRepository configRepository;
    private int playbackServerProfileId;
    private int recordingServerProfileId;
    private AppCompatActivity activity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_profiles);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        Connection connection = new ConnectionRepository(activity).getActiveConnectionSync();
        toolbarInterface.setTitle(getString(R.string.pref_profiles));
        toolbarInterface.setSubtitle(connection.getName());

        playbackProfileListPreference = (ListPreference) findPreference("pref_playback_profiles");
        recordingProfileListPreference = (ListPreference) findPreference("pref_recording_profiles");

        configRepository = new ConfigRepository(activity);
        ServerStatus serverStatus = configRepository.getServerStatus();
        addProfiles(playbackProfileListPreference, configRepository.getAllPlaybackServerProfiles());
        addProfiles(recordingProfileListPreference, configRepository.getAllRecordingServerProfiles());

        if (savedInstanceState != null) {
            playbackServerProfileId = savedInstanceState.getInt("playback_profile_id");
            recordingServerProfileId = savedInstanceState.getInt("recording_profile_id");
        } else {
            playbackServerProfileId = serverStatus.getPlaybackServerProfileId();
            recordingServerProfileId = serverStatus.getRecordingServerProfileId();
        }

        setPlaybackProfileListSummary();
        setRecordingProfileSummary();

        playbackProfileListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                playbackServerProfileId = Integer.valueOf((String) o);
                setPlaybackProfileListSummary();
                return true;
            }
        });
        recordingProfileListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                recordingServerProfileId = Integer.valueOf((String) o);
                setRecordingProfileSummary();
                return true;
            }
        });
    }

    private void setRecordingProfileSummary() {
        if (recordingServerProfileId == 0) {
            recordingProfileListPreference.setSummary("None");
        } else {
            ServerProfile recordingProfile = configRepository.getRecordingServerProfileById(recordingServerProfileId);
            recordingProfileListPreference.setSummary(recordingProfile != null ? recordingProfile.getName() : null);
        }
    }

    private void setPlaybackProfileListSummary() {
        if (playbackServerProfileId == 0) {
            playbackProfileListPreference.setSummary("None");
        } else {
            ServerProfile playbackProfile = configRepository.getPlaybackServerProfileById(playbackServerProfileId);
            playbackProfileListPreference.setSummary(playbackProfile != null ? playbackProfile.getName() : null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("playback_profile_id", playbackServerProfileId);
        outState.putInt("recording_profile_id", recordingServerProfileId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        configRepository.updatePlaybackServerProfile(playbackServerProfileId);
        configRepository.updateRecordingServerProfile(recordingServerProfileId);
        activity.finish();
    }

    private void addProfiles(final ListPreference listPreference, final List<ServerProfile> serverProfileList) {
        // Initialize the arrays that contain the profile values
        final int size = serverProfileList.size() + 1;
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] entryValues = new CharSequence[size];

        entries[0] = "None";
        entryValues[0] = "0";

        // Add the available profiles to list preference
        for (int i = 1; i < size; i++) {
            ServerProfile profile = serverProfileList.get(i - 1);
            entries[i] = profile.getName();
            entryValues[i] = String.valueOf(profile.getId());
        }
        listPreference.setEntries(entries);
        listPreference.setEntryValues(entryValues);
    }
}
