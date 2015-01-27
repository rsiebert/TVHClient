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
package org.tvheadend.tvhclient.fragments;

import java.util.List;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Profiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class SettingsProfileFragment extends PreferenceFragment implements HTSListener {

    private final static String TAG = SettingsProfileFragment.class.getSimpleName();

    private Activity activity;
    private Connection conn = null;
    private Profile playbackConnProfile = null;
    private Profile recordingConnProfile = null;

    private ListPreference recProfiles;
    private ListPreference playbackProfiles;
    private ListPreference progContainer;
    private CheckBoxPreference progTranscode;
    private ListPreference progResolution;
    private ListPreference progAudioCodec;
    private ListPreference progVideoCodec;
    private ListPreference progSubtitleCodec;
    private ListPreference recContainer;
    private CheckBoxPreference recTranscode;
    private ListPreference recResolution;
    private ListPreference recAudioCodec;
    private ListPreference recVideoCodec;
    private ListPreference recSubtitleCodec;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_profiles);

        recProfiles = (ListPreference) findPreference("pref_recording_profiles");
        playbackProfiles = (ListPreference) findPreference("pref_playback_profiles");
        progContainer = (ListPreference) findPreference("progContainerPref");
        progTranscode = (CheckBoxPreference) findPreference("progTranscodePref");
        progResolution = (ListPreference) findPreference("progResolutionPref");
        progAudioCodec = (ListPreference) findPreference("progAcodecPref");
        progVideoCodec = (ListPreference) findPreference("progVcodecPref");
        progSubtitleCodec = (ListPreference) findPreference("progScodecPref");
        recContainer = (ListPreference) findPreference("recContainerPref");
        recTranscode = (CheckBoxPreference) findPreference("recTranscodePref");
        recResolution = (ListPreference) findPreference("recResolutionPref");
        recAudioCodec = (ListPreference) findPreference("recAcodecPref");
        recVideoCodec = (ListPreference) findPreference("recVcodecPref");
        recSubtitleCodec = (ListPreference) findPreference("recScodecPref");

        // Get the connection where the profiles shall be edited
        Bundle bundle = getArguments();
        if (bundle != null) {
            long id = bundle.getLong(Constants.BUNDLE_CONNECTION_ID, 0);
            conn = DatabaseHelper.getInstance().getConnection(id);
            if (conn != null) {
                playbackConnProfile = DatabaseHelper.getInstance().getProfile(conn.playback_profile_id);
                recordingConnProfile = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;
    }

    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this); 

        recProfiles.setEnabled(false);
        playbackProfiles.setEnabled(false);

        // Apply the saved settings from the database for the old profile stuff
        if (playbackConnProfile != null) {
            progContainer.setValue(playbackConnProfile.container);
            progTranscode.setChecked(playbackConnProfile.transcode);
            progResolution.setValue(playbackConnProfile.resolution);
            progAudioCodec.setValue(playbackConnProfile.audio_codec);
            progVideoCodec.setValue(playbackConnProfile.video_codec);
            progSubtitleCodec.setValue(playbackConnProfile.subtitle_codec);
        }
        if (recordingConnProfile != null) {
            recContainer.setValue(recordingConnProfile.container);
            recTranscode.setChecked(recordingConnProfile.transcode);
            recResolution.setValue(recordingConnProfile.resolution);
            recAudioCodec.setValue(recordingConnProfile.audio_codec);
            recVideoCodec.setValue(recordingConnProfile.video_codec);
            recSubtitleCodec.setValue(recordingConnProfile.subtitle_codec);
        }

        // Connect to the server with the selected connection and do not
        // retrieve the initial data
        if (conn != null) {
            Utils.connect(activity, conn, true, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
        
        // Connect to the server with the selected connection and do not
        // retrieve the initial data
        Utils.connect(activity, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void save() {
        Log.i(TAG, "save");
        if (conn == null) {
            activity.finish();
        }
        if (playbackConnProfile == null) {
            Log.i(TAG, "save playbackConnProfile is null");
            playbackConnProfile = new Profile();
        }
        playbackConnProfile.uuid = playbackProfiles.getValue();
        playbackConnProfile.container = progContainer.getValue();
        playbackConnProfile.transcode = progTranscode.isChecked();
        playbackConnProfile.resolution = progResolution.getValue();
        playbackConnProfile.audio_codec = progAudioCodec.getValue();
        playbackConnProfile.video_codec = progVideoCodec.getValue();
        playbackConnProfile.subtitle_codec = progSubtitleCodec.getValue();

        // This check is wrong
        if (playbackConnProfile == null) {
            conn.playback_profile_id = (int) DatabaseHelper.getInstance().addProfile(playbackConnProfile);
            DatabaseHelper.getInstance().updateConnection(conn);
            Log.i(TAG, "save added playbackConnProfile to db with id " + conn.playback_profile_id);
        } else {
            DatabaseHelper.getInstance().updateProfile(playbackConnProfile);
            Log.i(TAG, "save updated playbackConnProfile to db with id " + conn.playback_profile_id);
        }

        if (recordingConnProfile == null) {
            Log.i(TAG, "save recordingConnProfile is null");
            recordingConnProfile = new Profile();
        }
        recordingConnProfile.uuid = recProfiles.getValue();
        recordingConnProfile.container = recContainer.getValue();
        recordingConnProfile.transcode = recTranscode.isChecked();
        recordingConnProfile.resolution = recResolution.getValue();
        recordingConnProfile.audio_codec = recAudioCodec.getValue();
        recordingConnProfile.video_codec = recVideoCodec.getValue();
        recordingConnProfile.subtitle_codec = recSubtitleCodec.getValue();

        if (recordingConnProfile == null) {
            conn.recording_profile_id = (int) DatabaseHelper.getInstance().addProfile(recordingConnProfile);
            DatabaseHelper.getInstance().updateConnection(conn);
            Log.i(TAG, "save added recordingConnProfile to db with id " + conn.recording_profile_id);
        } else {
            DatabaseHelper.getInstance().updateProfile(recordingConnProfile);
            Log.i(TAG, "save updated recordingConnProfile in db with id " + conn.recording_profile_id);
        }
        activity.finish();
    }

    public void cancel() {
        // TODO Auto-generated method stub

    }

    /**
     * 
     */
    private void loadProfiles() {
        if (recProfiles != null && playbackProfiles != null) {
            Log.i(TAG, "loadProfiles");
            // Disable the settings until profiles have been loaded.
            // If the server does not support it then it stays disabled
            recProfiles.setEnabled(false);
            playbackProfiles.setEnabled(false);

            // Hide or show the available profile menu items depending on
            // the server version
            TVHClientApplication app = (TVHClientApplication) activity.getApplication();
            if (app.getProtocolVersion() > 15) {
                // Get the available profiles from the server
                final Intent intent = new Intent(activity, HTSService.class);
                intent.setAction(Constants.ACTION_GET_DVR_CONFIG);
                activity.startService(intent);
                intent.setAction(Constants.ACTION_GET_PROFILES);
                activity.startService(intent);
            }
        }
    }

    @Override
    public void onMessage(String action, final Object obj) {
        Log.i(TAG, "onMessage " + action);
        if (action.equals(Constants.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    Log.i(TAG, "Loading done? " + !loading);
                    if (!loading) {
                        // Load the profiles from the selected connection
                        loadProfiles();
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_GET_DVR_CONFIG)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (recProfiles != null && app.getProtocolVersion() > 15) {
                        addProfiles(recProfiles, app.getDvrConfigs());
                        // TODO 
                        // show the currently selected profile name
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_GET_PROFILES)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (playbackProfiles != null && app.getProtocolVersion() > 15) {
                        addProfiles(playbackProfiles, app.getProfiles());
                        playbackProfiles.setSummary(R.string.pref_playback_profiles_sum);
                        // TODO 
                        // show the currently selected profile name
                    }
                }
            });
        }
    }

    /**
     * 
     * @param preferenceList
     * @param profileList
     */
    protected void addProfiles(ListPreference preferenceList, final List<Profiles> profileList) {
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

        // Enable the preference for use selection
        preferenceList.setEnabled(true);
    }
}
