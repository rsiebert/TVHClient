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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.TranscodingProfile;
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;

public class SettingsTranscodingFragment extends BasePreferenceFragment implements BackPressedInterface {

    private TranscodingProfile playbackProfile = null;
    private TranscodingProfile recordingProfile = null;

    private ListPreference programContainerPreference;
    private CheckBoxPreference programTranscodePreference;
    private ListPreference programResolutionPreference;
    private ListPreference programAudioCodecPreference;
    private ListPreference programVideoCodecPreference;
    private ListPreference programSubtitleCodecPreference;

    private ListPreference recordingContainerPreference;
    private CheckBoxPreference recordingTranscodePreference;
    private ListPreference recordingResolutionPreference;
    private ListPreference recordingAudioCodecPreference;
    private ListPreference recordingVideoCodecPreference;
    private ListPreference recordingSubtitleCodecPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_transcoding);

        setHasOptionsMenu(true);

        Connection connection = connectionRepository.getActiveConnectionSync();
        toolbarInterface.setTitle(getString(R.string.pref_transcoding));
        toolbarInterface.setSubtitle(connection.getName());

        programContainerPreference = (ListPreference) findPreference("program_container");
        programTranscodePreference = (CheckBoxPreference) findPreference("program_transcoding_enabled");
        programResolutionPreference = (ListPreference) findPreference("program_resolution");
        programAudioCodecPreference = (ListPreference) findPreference("program_audio_codec");
        programVideoCodecPreference = (ListPreference) findPreference("program_video_codec");
        programSubtitleCodecPreference = (ListPreference) findPreference("program_subtitle_codec");

        recordingContainerPreference = (ListPreference) findPreference("recording_container");
        recordingTranscodePreference = (CheckBoxPreference) findPreference("recording_transcoding_enabled");
        recordingResolutionPreference = (ListPreference) findPreference("recording_resolution");
        recordingAudioCodecPreference = (ListPreference) findPreference("recording_audio_codec");
        recordingVideoCodecPreference = (ListPreference) findPreference("recording_video_codec");
        recordingSubtitleCodecPreference = (ListPreference) findPreference("recording_subtitle_codec");

        playbackProfile = configRepository.getPlaybackTranscodingProfile();
        recordingProfile = configRepository.getRecordingTranscodingProfile();

        // Restore the currently selected uuids after an orientation change
        if (savedInstanceState != null) {
            playbackProfile.setContainer(savedInstanceState.getString("program_container"));
            playbackProfile.setTranscode(savedInstanceState.getBoolean("program_transcoding_enabled"));
            playbackProfile.setResolution(savedInstanceState.getString("program_resolution"));
            playbackProfile.setAudioCodec(savedInstanceState.getString("program_audio_codec"));
            playbackProfile.setVideoCodec(savedInstanceState.getString("program_video_codec"));
            playbackProfile.setSubtitleCodec(savedInstanceState.getString("recording_subtitle_codec"));

            recordingProfile.setContainer(savedInstanceState.getString("recording_container"));
            recordingProfile.setTranscode(savedInstanceState.getBoolean("recording_transcoding_enabled"));
            recordingProfile.setResolution(savedInstanceState.getString("recording_resolution"));
            recordingProfile.setAudioCodec(savedInstanceState.getString("recording_audio_codec"));
            recordingProfile.setVideoCodec(savedInstanceState.getString("recording_video_codec"));
            recordingProfile.setSubtitleCodec(savedInstanceState.getString("recording_subtitle_codec"));
        }

        programContainerPreference.setValue(playbackProfile.getContainer());
        programTranscodePreference.setChecked(playbackProfile.isTranscode());
        programResolutionPreference.setValue(playbackProfile.getResolution());
        programAudioCodecPreference.setValue(playbackProfile.getAudioCodec());
        programVideoCodecPreference.setValue(playbackProfile.getVideoCodec());
        programSubtitleCodecPreference.setValue(playbackProfile.getSubtitleCodec());

        recordingContainerPreference.setValue(recordingProfile.getContainer());
        recordingTranscodePreference.setChecked(recordingProfile.isTranscode());
        recordingResolutionPreference.setValue(recordingProfile.getResolution());
        recordingAudioCodecPreference.setValue(recordingProfile.getAudioCodec());
        recordingVideoCodecPreference.setValue(recordingProfile.getVideoCodec());
        recordingSubtitleCodecPreference.setValue(recordingProfile.getSubtitleCodec());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("program_container", programContainerPreference.getValue());
        outState.putBoolean("program_transcoding_enabled", programTranscodePreference.isChecked());
        outState.putString("program_resolution", programResolutionPreference.getValue());
        outState.putString("program_audio_codec", programAudioCodecPreference.getValue());
        outState.putString("program_video_codec", programVideoCodecPreference.getValue());
        outState.putString("recording_subtitle_codec", programSubtitleCodecPreference.getValue());

        outState.putString("recording_container", recordingContainerPreference.getValue());
        outState.putBoolean("recording_transcoding_enabled", recordingTranscodePreference.isChecked());
        outState.putString("recording_resolution", recordingResolutionPreference.getValue());
        outState.putString("recording_audio_codec", recordingAudioCodecPreference.getValue());
        outState.putString("recording_video_codec", recordingVideoCodecPreference.getValue());
        outState.putString("recording_subtitle_codec", recordingSubtitleCodecPreference.getValue());
        super.onSaveInstanceState(outState);
    }

    private void save() {
        // Save the values into the profile
        playbackProfile.setContainer(programContainerPreference.getValue());
        playbackProfile.setTranscode(programTranscodePreference.isChecked());
        playbackProfile.setResolution(programResolutionPreference.getValue());
        playbackProfile.setAudioCodec(programAudioCodecPreference.getValue());
        playbackProfile.setVideoCodec(programVideoCodecPreference.getValue());
        playbackProfile.setSubtitleCodec(programSubtitleCodecPreference.getValue());
        configRepository.updatePlaybackTranscodingProfile(playbackProfile);

        // Save the values into the profile
        recordingProfile.setContainer(recordingContainerPreference.getValue());
        recordingProfile.setTranscode(recordingTranscodePreference.isChecked());
        recordingProfile.setResolution(recordingResolutionPreference.getValue());
        recordingProfile.setAudioCodec(recordingAudioCodecPreference.getValue());
        recordingProfile.setVideoCodec(recordingVideoCodecPreference.getValue());
        recordingProfile.setSubtitleCodec(recordingSubtitleCodecPreference.getValue());
        configRepository.updateRecordingTranscodingProfile(recordingProfile);

        activity.finish();
    }

    @Override
    public void onBackPressed() {
        save();
    }
}
