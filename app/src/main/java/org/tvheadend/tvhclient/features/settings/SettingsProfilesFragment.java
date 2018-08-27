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
package org.tvheadend.tvhclient.features.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;

import java.util.List;

public class SettingsProfilesFragment extends BasePreferenceFragment implements BackPressedInterface {

    private ListPreference recordingProfilesPreference;
    private ListPreference playbackProfilesPreference;
    private ListPreference castingProfilesPreference;
    private int playbackServerProfileId;
    private int recordingServerProfileId;
    private int castingServerProfileId;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_profiles);

        Connection connection = appRepository.getConnectionData().getActiveItem();
        toolbarInterface.setTitle(getString(R.string.pref_profiles));
        toolbarInterface.setSubtitle(connection.getName());

        playbackProfilesPreference = (ListPreference) findPreference("playback_profiles");
        recordingProfilesPreference = (ListPreference) findPreference("recording_profiles");
        castingProfilesPreference = (ListPreference) findPreference("casting_profiles");

        if (savedInstanceState != null) {
            playbackServerProfileId = savedInstanceState.getInt("playback_profile_id");
            recordingServerProfileId = savedInstanceState.getInt("recording_profile_id");
            castingServerProfileId = savedInstanceState.getInt("casting_profile_id");
        } else {
            playbackServerProfileId = serverStatus.getPlaybackServerProfileId();
            recordingServerProfileId = serverStatus.getRecordingServerProfileId();
            castingServerProfileId = serverStatus.getCastingServerProfileId();
        }

        addProfiles(playbackProfilesPreference,
                appRepository.getServerProfileData().getPlaybackProfiles(),
                playbackServerProfileId);
        addProfiles(recordingProfilesPreference,
                appRepository.getServerProfileData().getRecordingProfiles(),
                recordingServerProfileId);
        addProfiles(castingProfilesPreference,
                appRepository.getServerProfileData().getPlaybackProfiles(),
                castingServerProfileId);

        setPlaybackProfileListSummary();
        setRecordingProfileListSummary();
        setCastingProfileListSummary();

        playbackProfilesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                playbackServerProfileId = Integer.valueOf((String) o);
                setPlaybackProfileListSummary();
                return true;
            }
        });
        recordingProfilesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                recordingServerProfileId = Integer.valueOf((String) o);
                setRecordingProfileListSummary();
                return true;
            }
        });
        castingProfilesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                castingServerProfileId = Integer.valueOf((String) o);
                setCastingProfileListSummary();
                return true;
            }
        });

        if (!isUnlocked) {
            castingProfilesPreference.setEnabled(false);
        }
    }

    private void setPlaybackProfileListSummary() {
        if (playbackServerProfileId == 0) {
            playbackProfilesPreference.setSummary("None");
        } else {
            ServerProfile playbackProfile = appRepository.getServerProfileData().getItemById(playbackServerProfileId);
            playbackProfilesPreference.setSummary(playbackProfile != null ? playbackProfile.getName() : null);
        }
    }

    private void setRecordingProfileListSummary() {
        if (recordingServerProfileId == 0) {
            recordingProfilesPreference.setSummary("None");
        } else {
            ServerProfile recordingProfile = appRepository.getServerProfileData().getItemById(recordingServerProfileId);
            recordingProfilesPreference.setSummary(recordingProfile != null ? recordingProfile.getName() : null);
        }
    }

    private void setCastingProfileListSummary() {
        if (castingServerProfileId == 0) {
            castingProfilesPreference.setSummary("None");
        } else {
            ServerProfile castingProfile = appRepository.getServerProfileData().getItemById(castingServerProfileId);
            castingProfilesPreference.setSummary(castingProfile != null ? castingProfile.getName() : null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("playback_profile_id", playbackServerProfileId);
        outState.putInt("recording_profile_id", recordingServerProfileId);
        outState.putInt("casting_profile_id", castingServerProfileId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        serverStatus.setPlaybackServerProfileId(playbackServerProfileId);
        serverStatus.setRecordingServerProfileId(recordingServerProfileId);
        if (isUnlocked) {
            serverStatus.setCastingServerProfileId(castingServerProfileId);
        }
        appRepository.getServerStatusData().updateItem(serverStatus);
        activity.finish();
    }

    private void addProfiles(final ListPreference listPreference, final List<ServerProfile> serverProfileList, int selectedIndex) {
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
        listPreference.setValueIndex(selectedIndex);
    }
}
