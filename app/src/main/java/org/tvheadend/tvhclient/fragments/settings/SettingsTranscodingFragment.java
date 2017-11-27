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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;

public class SettingsTranscodingFragment extends PreferenceFragment implements BackPressedInterface {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsTranscodingFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;

    private Connection conn = null;
    private Profile progProfile = null;
    private Profile recProfile = null;

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

    private DatabaseHelper dbh;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = getActivity();
        dbh = DatabaseHelper.getInstance(getActivity().getApplicationContext());

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_transcoding);

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

        conn = dbh.getSelectedConnection();
        if (conn != null) {
            progProfile = dbh.getProfile(conn.playback_profile_id);
            if (progProfile == null) {
                progProfile = new Profile();
            }
            recProfile = dbh.getProfile(conn.recording_profile_id);
            if (recProfile == null) {
                recProfile = new Profile();
            }
        }

        // If the state is null then this activity has been started for
        // the first time. If the state is not null then the screen has
        // been rotated and we have to reuse the values.
        if (savedInstanceState != null) {
            conn = dbh.getSelectedConnection();
            if (conn != null) {
                progProfile.container = savedInstanceState.getString(PROG_PROFILE_CONTAINER);
                progProfile.transcode = savedInstanceState.getBoolean(PROG_PROFILE_TRANSCODE);
                progProfile.resolution = savedInstanceState.getString(PROG_PROFILE_RESOLUTION);
                progProfile.audio_codec = savedInstanceState.getString(PROG_PROFILE_AUDIO_CODEC);
                progProfile.video_codec = savedInstanceState.getString(PROG_PROFILE_VIDEO_CODEC);
                progProfile.subtitle_codec = savedInstanceState.getString(PROG_PROFILE_SUBTITLE_CODEC);

                recProfile.container = savedInstanceState.getString(REC_PROFILE_CONTAINER);
                recProfile.transcode = savedInstanceState.getBoolean(REC_PROFILE_TRANSCODE);
                recProfile.resolution = savedInstanceState.getString(REC_PROFILE_RESOLUTION);
                recProfile.audio_codec = savedInstanceState.getString(REC_PROFILE_AUDIO_CODEC);
                recProfile.video_codec = savedInstanceState.getString(REC_PROFILE_VIDEO_CODEC);
                recProfile.subtitle_codec = savedInstanceState.getString(REC_PROFILE_SUBTITLE_CODEC);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(CONNECTION_ID, conn.id);

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

    @Override
    public void onDestroy() {
        actionBarInterface = null;
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.pref_transcoding));
        }
    }

    public void onResume() {
        super.onResume();

        if (actionBarInterface != null) {
            actionBarInterface.setActionBarSubtitle(conn.name);
        }

        prefProgContainer.setValue(progProfile.container);
        prefProgTranscode.setChecked(progProfile.transcode);
        prefProgResolution.setValue(progProfile.resolution);
        prefProgAudioCodec.setValue(progProfile.audio_codec);
        prefProgVideoCodec.setValue(progProfile.video_codec);
        prefProgSubtitleCodec.setValue(progProfile.subtitle_codec);

        prefRecContainer.setValue(recProfile.container);
        prefRecTranscode.setChecked(recProfile.transcode);
        prefRecResolution.setValue(recProfile.resolution);
        prefRecAudioCodec.setValue(recProfile.audio_codec);
        prefRecVideoCodec.setValue(recProfile.video_codec);
        prefRecSubtitleCodec.setValue(recProfile.subtitle_codec);
    }

    private void save() {
        // Save the values into the profile
        progProfile.container = prefProgContainer.getValue();
        progProfile.transcode = prefProgTranscode.isChecked();
        progProfile.resolution = prefProgResolution.getValue();
        progProfile.audio_codec = prefProgAudioCodec.getValue();
        progProfile.video_codec = prefProgVideoCodec.getValue();
        progProfile.subtitle_codec = prefProgSubtitleCodec.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (progProfile.id == 0) {
            conn.playback_profile_id = (int) dbh.addProfile(progProfile);
            dbh.updateConnection(conn);
        } else {
            dbh.updateProfile(progProfile);
        }

        // Save the values into the profile
        recProfile.container = prefRecContainer.getValue();
        recProfile.transcode = prefRecTranscode.isChecked();
        recProfile.resolution = prefRecResolution.getValue();
        recProfile.audio_codec = prefRecAudioCodec.getValue();
        recProfile.video_codec = prefRecVideoCodec.getValue();
        recProfile.subtitle_codec = prefRecSubtitleCodec.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (recProfile.id == 0) {
            conn.recording_profile_id = (int) dbh.addProfile(recProfile);
            dbh.updateConnection(conn);
        } else {
            dbh.updateProfile(recProfile);
        }
    }

    @Override
    public void onBackPressed() {
        save();
    }
}
