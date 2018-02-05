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
import org.tvheadend.tvhclient.data.repository.ConnectionDataRepository;
import org.tvheadend.tvhclient.data.repository.ProfileDataRepository;
import org.tvheadend.tvhclient.data.repository.ServerDataRepository;
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

    private static final String PROG_PROFILE_CONTAINER = "prog_profile_container";
    private static final String PROG_PROFILE_TRANSCODE = "prog_profile_transcode";
    private static final String PROG_PROFILE_RESOLUTION = "prog_profile_resolution";
    private static final String PROG_PROFILE_AUDIO_CODEC = "prog_profile_audio_codec";
    private static final String PROG_PROFILE_VIDEO_CODEC = "prog_profile_vodeo_codec";
    private static final String PROG_PROFILE_SUBTITLE_CODEC = "prog_profile_subtitle_codec";
    private static final String REC_PROFILE_CONTAINER = "rec_profile_container";
    private static final String REC_PROFILE_TRANSCODE = "rec_profile_transcode";
    private static final String REC_PROFILE_RESOLUTION = "rec_profile_resolution";
    private static final String REC_PROFILE_AUDIO_CODEC = "rec_profile_audio_codec";
    private static final String REC_PROFILE_VIDEO_CODEC = "rec_profile_vodeo_codec";
    private static final String REC_PROFILE_SUBTITLE_CODEC = "rec_profile_subtitle_codec";
    private ProfileDataRepository profileRepository;
    private ServerDataRepository serverRepository;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_transcoding);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        Connection connection = new ConnectionDataRepository(activity).getActiveConnectionSync();
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

        serverRepository = new ServerDataRepository(activity);
        profileRepository = new ProfileDataRepository(activity);
        playbackProfile = profileRepository.getPlaybackTranscodingProfile();
        recordingProfile = profileRepository.getRecordingTranscodingProfile();

        // Restore the currently selected uuids after an orientation change
        if (savedInstanceState != null) {
            playbackProfile.setContainer(savedInstanceState.getString(PROG_PROFILE_CONTAINER));
            playbackProfile.setTranscode(savedInstanceState.getBoolean(PROG_PROFILE_TRANSCODE));
            playbackProfile.setResolution(savedInstanceState.getString(PROG_PROFILE_RESOLUTION));
            playbackProfile.setAudioCodec(savedInstanceState.getString(PROG_PROFILE_AUDIO_CODEC));
            playbackProfile.setVideoCodec(savedInstanceState.getString(PROG_PROFILE_VIDEO_CODEC));
            playbackProfile.setSubtitleCodec(savedInstanceState.getString(PROG_PROFILE_SUBTITLE_CODEC));

            recordingProfile.setContainer(savedInstanceState.getString(REC_PROFILE_CONTAINER));
            recordingProfile.setTranscode(savedInstanceState.getBoolean(REC_PROFILE_TRANSCODE));
            recordingProfile.setResolution(savedInstanceState.getString(REC_PROFILE_RESOLUTION));
            recordingProfile.setAudioCodec(savedInstanceState.getString(REC_PROFILE_AUDIO_CODEC));
            recordingProfile.setVideoCodec(savedInstanceState.getString(REC_PROFILE_VIDEO_CODEC));
            recordingProfile.setSubtitleCodec(savedInstanceState.getString(REC_PROFILE_SUBTITLE_CODEC));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(PROG_PROFILE_CONTAINER, prefProgContainer.getValue());
        outState.putBoolean(PROG_PROFILE_TRANSCODE, prefProgTranscode.isChecked());
        outState.putString(PROG_PROFILE_RESOLUTION, prefProgResolution.getValue());
        outState.putString(PROG_PROFILE_AUDIO_CODEC, prefProgAudioCodec.getValue());
        outState.putString(PROG_PROFILE_VIDEO_CODEC, prefProgVideoCodec.getValue());
        outState.putString(PROG_PROFILE_SUBTITLE_CODEC, prefProgSubtitleCodec.getValue());

        outState.putString(REC_PROFILE_CONTAINER, prefRecContainer.getValue());
        outState.putBoolean(REC_PROFILE_TRANSCODE, prefRecTranscode.isChecked());
        outState.putString(REC_PROFILE_RESOLUTION, prefRecResolution.getValue());
        outState.putString(REC_PROFILE_AUDIO_CODEC, prefRecAudioCodec.getValue());
        outState.putString(REC_PROFILE_VIDEO_CODEC, prefRecVideoCodec.getValue());
        outState.putString(REC_PROFILE_SUBTITLE_CODEC, prefRecSubtitleCodec.getValue());
        super.onSaveInstanceState(outState);
    }

    public void onResume() {
        super.onResume();

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

    private void savePlaybackProfile() {
        // Save the values into the profile
        playbackProfile.setContainer(prefProgContainer.getValue());
        playbackProfile.setTranscode(prefProgTranscode.isChecked());
        playbackProfile.setResolution(prefProgResolution.getValue());
        playbackProfile.setAudioCodec(prefProgAudioCodec.getValue());
        playbackProfile.setVideoCodec(prefProgVideoCodec.getValue());
        playbackProfile.setSubtitleCodec(prefProgSubtitleCodec.getValue());

        profileRepository.updatePlaybackTranscodingProfile(playbackProfile);
        serverRepository.updatePlaybackTranscodingProfile(playbackProfile.getId());
    }

    private void saveRecordingProfile() {
        // Save the values into the profile
        recordingProfile.setContainer(prefRecContainer.getValue());
        recordingProfile.setTranscode(prefRecTranscode.isChecked());
        recordingProfile.setResolution(prefRecResolution.getValue());
        recordingProfile.setAudioCodec(prefRecAudioCodec.getValue());
        recordingProfile.setVideoCodec(prefRecVideoCodec.getValue());
        recordingProfile.setSubtitleCodec(prefRecSubtitleCodec.getValue());

        profileRepository.updateRecordingTranscodingProfile(recordingProfile);
        serverRepository.updateRecordingTranscodingProfile(recordingProfile.getId());
    }

    @Override
    public void onBackPressed() {
        savePlaybackProfile();
        saveRecordingProfile();
    }
}
