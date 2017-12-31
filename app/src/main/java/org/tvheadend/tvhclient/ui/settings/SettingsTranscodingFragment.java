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

import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.model.Profile;

public class SettingsTranscodingFragment extends PreferenceFragment implements BackPressedInterface {

    private ToolbarInterface toolbarInterface;
    private Connection connection = null;
    private Profile playbackProfile = null;
    private Profile recordingProfile = null;

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

    private static final String CONNECTION_ID = "conn_id";
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_transcoding);

        if (getActivity() instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) getActivity();
        }
        connection = DatabaseHelper.getInstance(getActivity()).getSelectedConnection();
        toolbarInterface.setTitle(getString(R.string.pref_transcoding));
        toolbarInterface.setSubtitle(connection.name);

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
            connection = DatabaseHelper.getInstance(getActivity()).getSelectedConnection();

            playbackProfile.container = savedInstanceState.getString(PROG_PROFILE_CONTAINER);
            playbackProfile.transcode = savedInstanceState.getBoolean(PROG_PROFILE_TRANSCODE);
            playbackProfile.resolution = savedInstanceState.getString(PROG_PROFILE_RESOLUTION);
            playbackProfile.audio_codec = savedInstanceState.getString(PROG_PROFILE_AUDIO_CODEC);
            playbackProfile.video_codec = savedInstanceState.getString(PROG_PROFILE_VIDEO_CODEC);
            playbackProfile.subtitle_codec = savedInstanceState.getString(PROG_PROFILE_SUBTITLE_CODEC);

            recordingProfile.container = savedInstanceState.getString(REC_PROFILE_CONTAINER);
            recordingProfile.transcode = savedInstanceState.getBoolean(REC_PROFILE_TRANSCODE);
            recordingProfile.resolution = savedInstanceState.getString(REC_PROFILE_RESOLUTION);
            recordingProfile.audio_codec = savedInstanceState.getString(REC_PROFILE_AUDIO_CODEC);
            recordingProfile.video_codec = savedInstanceState.getString(REC_PROFILE_VIDEO_CODEC);
            recordingProfile.subtitle_codec = savedInstanceState.getString(REC_PROFILE_SUBTITLE_CODEC);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(CONNECTION_ID, connection.id);

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

        prefProgContainer.setValue(playbackProfile.container);
        prefProgTranscode.setChecked(playbackProfile.transcode);
        prefProgResolution.setValue(playbackProfile.resolution);
        prefProgAudioCodec.setValue(playbackProfile.audio_codec);
        prefProgVideoCodec.setValue(playbackProfile.video_codec);
        prefProgSubtitleCodec.setValue(playbackProfile.subtitle_codec);

        prefRecContainer.setValue(recordingProfile.container);
        prefRecTranscode.setChecked(recordingProfile.transcode);
        prefRecResolution.setValue(recordingProfile.resolution);
        prefRecAudioCodec.setValue(recordingProfile.audio_codec);
        prefRecVideoCodec.setValue(recordingProfile.video_codec);
        prefRecSubtitleCodec.setValue(recordingProfile.subtitle_codec);
    }

    private void savePlaybackProfile() {
        // Save the values into the profile
        playbackProfile.container = prefProgContainer.getValue();
        playbackProfile.transcode = prefProgTranscode.isChecked();
        playbackProfile.resolution = prefProgResolution.getValue();
        playbackProfile.audio_codec = prefProgAudioCodec.getValue();
        playbackProfile.video_codec = prefProgVideoCodec.getValue();
        playbackProfile.subtitle_codec = prefProgSubtitleCodec.getValue();

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

    private void saveRecordingProfile() {
        // Save the values into the profile
        recordingProfile.container = prefRecContainer.getValue();
        recordingProfile.transcode = prefRecTranscode.isChecked();
        recordingProfile.resolution = prefRecResolution.getValue();
        recordingProfile.audio_codec = prefRecAudioCodec.getValue();
        recordingProfile.video_codec = prefRecVideoCodec.getValue();
        recordingProfile.subtitle_codec = prefRecSubtitleCodec.getValue();

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

    @Override
    public void onBackPressed() {
        savePlaybackProfile();
        saveRecordingProfile();
    }
}
