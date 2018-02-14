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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.TranscodingProfile;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;

// TODO use viewmodel to store profile

public class SettingsTranscodingFragment extends PreferenceFragment implements BackPressedInterface {

    private ToolbarInterface toolbarInterface;
    private TranscodingProfile playbackProfile = null;
    private TranscodingProfile recordingProfile = null;

    private ListPreference prefProgContainer;
    private CheckBoxPreference prefProgTranscode;
    private ListPreference prefProgResolution;
    private ListPreference prefProgAudioCodec;
    private ListPreference prefProgVideoCodec;
    private ListPreference prefProgSubtitleCodec;
    private ListPreference prefRecContainer;
    private CheckBoxPreference prefRecTranscode;
    private ListPreference prefRecResolution;
    private ListPreference prefRecAudioCodec;
    private ListPreference prefRecVideoCodec;
    private ListPreference prefRecSubtitleCodec;

    private AppCompatActivity activity;
    private ConfigRepository configRepository;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_transcoding);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        setHasOptionsMenu(true);

        Connection connection = new ConnectionRepository(activity).getActiveConnectionSync();
        toolbarInterface.setTitle(getString(R.string.pref_transcoding));
        toolbarInterface.setSubtitle(connection.getName());

        prefProgContainer = (ListPreference) findPreference("progContainerPref");
        prefProgTranscode = (CheckBoxPreference) findPreference("progTranscodePref");
        prefProgResolution = (ListPreference) findPreference("progResolutionPref");
        prefProgAudioCodec = (ListPreference) findPreference("progAcodecPref");
        prefProgVideoCodec = (ListPreference) findPreference("progVcodecPref");
        prefProgSubtitleCodec = (ListPreference) findPreference("progScodecPref");
        prefRecContainer = (ListPreference) findPreference("recContainerPref");
        prefRecTranscode = (CheckBoxPreference) findPreference("recTranscodePref");
        prefRecResolution = (ListPreference) findPreference("recResolutionPref");
        prefRecAudioCodec = (ListPreference) findPreference("recAcodecPref");
        prefRecVideoCodec = (ListPreference) findPreference("recVcodecPref");
        prefRecSubtitleCodec = (ListPreference) findPreference("recScodecPref");

        configRepository = new ConfigRepository(activity);
        playbackProfile = configRepository.getPlaybackTranscodingProfile();
        recordingProfile = configRepository.getRecordingTranscodingProfile();

        // Restore the currently selected uuids after an orientation change
        if (savedInstanceState != null) {
            playbackProfile.setContainer(savedInstanceState.getString("prog_profile_container"));
            playbackProfile.setTranscode(savedInstanceState.getBoolean("prog_profile_transcode"));
            playbackProfile.setResolution(savedInstanceState.getString("prog_profile_resolution"));
            playbackProfile.setAudioCodec(savedInstanceState.getString("prog_profile_audio_codec"));
            playbackProfile.setVideoCodec(savedInstanceState.getString("prog_profile_vodeo_codec"));
            playbackProfile.setSubtitleCodec(savedInstanceState.getString("prog_profile_subtitle_codec"));

            recordingProfile.setContainer(savedInstanceState.getString("rec_profile_container"));
            recordingProfile.setTranscode(savedInstanceState.getBoolean("rec_profile_transcode"));
            recordingProfile.setResolution(savedInstanceState.getString("rec_profile_resolution"));
            recordingProfile.setAudioCodec(savedInstanceState.getString("rec_profile_audio_codec"));
            recordingProfile.setVideoCodec(savedInstanceState.getString("rec_profile_vodeo_codec"));
            recordingProfile.setSubtitleCodec(savedInstanceState.getString("rec_profile_subtitle_codec"));
        }

        prefProgContainer.setValue(playbackProfile.getContainer());
        prefProgTranscode.setChecked(playbackProfile.isTranscode());
        prefProgResolution.setValue(playbackProfile.getResolution());
        prefProgAudioCodec.setValue(playbackProfile.getAudioCodec());
        prefProgVideoCodec.setValue(playbackProfile.getVideoCodec());
        prefProgSubtitleCodec.setValue(playbackProfile.getSubtitleCodec());

        prefRecContainer.setValue(recordingProfile.getContainer());
        prefRecTranscode.setChecked(recordingProfile.isTranscode());
        prefRecResolution.setValue(recordingProfile.getResolution());
        prefRecAudioCodec.setValue(recordingProfile.getAudioCodec());
        prefRecVideoCodec.setValue(recordingProfile.getVideoCodec());
        prefRecSubtitleCodec.setValue(recordingProfile.getSubtitleCodec());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("prog_profile_container", prefProgContainer.getValue());
        outState.putBoolean("prog_profile_transcode", prefProgTranscode.isChecked());
        outState.putString("prog_profile_resolution", prefProgResolution.getValue());
        outState.putString("prog_profile_audio_codec", prefProgAudioCodec.getValue());
        outState.putString("prog_profile_vodeo_codec", prefProgVideoCodec.getValue());
        outState.putString("prog_profile_subtitle_codec", prefProgSubtitleCodec.getValue());

        outState.putString("rec_profile_container", prefRecContainer.getValue());
        outState.putBoolean("rec_profile_transcode", prefRecTranscode.isChecked());
        outState.putString("rec_profile_resolution", prefRecResolution.getValue());
        outState.putString("rec_profile_audio_codec", prefRecAudioCodec.getValue());
        outState.putString("rec_profile_vodeo_codec", prefRecVideoCodec.getValue());
        outState.putString("rec_profile_subtitle_codec", prefRecSubtitleCodec.getValue());
        super.onSaveInstanceState(outState);
    }

    private void save() {
        // Save the values into the profile
        playbackProfile.setContainer(prefProgContainer.getValue());
        playbackProfile.setTranscode(prefProgTranscode.isChecked());
        playbackProfile.setResolution(prefProgResolution.getValue());
        playbackProfile.setAudioCodec(prefProgAudioCodec.getValue());
        playbackProfile.setVideoCodec(prefProgVideoCodec.getValue());
        playbackProfile.setSubtitleCodec(prefProgSubtitleCodec.getValue());
        configRepository.updatePlaybackTranscodingProfile(playbackProfile);

        // Save the values into the profile
        recordingProfile.setContainer(prefRecContainer.getValue());
        recordingProfile.setTranscode(prefRecTranscode.isChecked());
        recordingProfile.setResolution(prefRecResolution.getValue());
        recordingProfile.setAudioCodec(prefRecAudioCodec.getValue());
        recordingProfile.setVideoCodec(prefRecVideoCodec.getValue());
        recordingProfile.setSubtitleCodec(prefRecSubtitleCodec.getValue());
        configRepository.updateRecordingTranscodingProfile(recordingProfile);

        activity.finish();
    }

    @Override
    public void onBackPressed() {
        save();
    }
}
