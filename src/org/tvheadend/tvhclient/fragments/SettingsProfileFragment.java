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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class SettingsProfileFragment extends PreferenceFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsProfileFragment.class.getSimpleName();

    private Activity activity;
    private Connection conn = null;
    private Profile progProfile = null;
    private Profile recProfile = null;

    private ListPreference prefRecProfiles;
    private ListPreference prefProgProfiles;
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

    private boolean connectToSelectedServer;

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
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_profiles);

        prefRecProfiles = (ListPreference) findPreference("pref_recording_profiles");
        prefProgProfiles = (ListPreference) findPreference("pref_playback_profiles");
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

        // Connect to the chosen server when the fragment was called for the
        // first time. Do not reconnect when orientation changes have occurred.
        connectToSelectedServer = (savedInstanceState == null);

        // If the state is null then this activity has been started for
        // the first time. If the state is not null then the screen has
        // been rotated and we have to reuse the values.
        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                // Get the connection where the profiles shall be edited
                long id = bundle.getLong(Constants.BUNDLE_CONNECTION_ID, 0);
                conn = DatabaseHelper.getInstance().getConnection(id);
                if (conn != null) {
                    progProfile = DatabaseHelper.getInstance().getProfile(conn.playback_profile_id);
                    if (progProfile == null) {
                        progProfile = new Profile();
                    }
                    recProfile = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
                    if (recProfile == null) {
                        recProfile = new Profile();
                    }
                }
            }
        } else {
            long id = savedInstanceState.getLong(CONNECTION_ID);
            conn = DatabaseHelper.getInstance().getConnection(id);
            if (conn != null) {
                progProfile = DatabaseHelper.getInstance().getProfile(conn.playback_profile_id);
                if (progProfile == null) {
                    progProfile = new Profile();
                }
                progProfile.container = savedInstanceState.getString(PROG_PROFILE_CONTAINER);
                progProfile.transcode = savedInstanceState.getBoolean(PROG_PROFILE_TRANSCODE);
                progProfile.resolution = savedInstanceState.getString(PROG_PROFILE_RESOLUTION);
                progProfile.audio_codec = savedInstanceState.getString(PROG_PROFILE_AUDIO_CODEC);
                progProfile.video_codec = savedInstanceState.getString(PROG_PROFILE_VIDEO_CODEC);
                progProfile.subtitle_codec = savedInstanceState.getString(PROG_PROFILE_SUBTITLE_CODEC);

                recProfile = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
                if (recProfile == null) {
                    recProfile = new Profile();
                }
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity = null;
    }

    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this); 

        // If no connection exists exit
        if (conn == null) {
            activity.setResult(Activity.RESULT_OK, activity.getIntent());
            activity.finish();
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

        // Connect to the server with the selected credentials and do not
        // retrieve the initial data
        if (connectToSelectedServer && conn != null) {
            // Disable these preference, the data will be loaded now
            prefRecProfiles.setEnabled(false);
            prefProgProfiles.setEnabled(false);

            if (app.isUnlocked()) {
                Utils.connect(activity, conn, true, false);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    public void save() {
        // Save the values into the profile
        progProfile.name = prefProgProfiles.getEntry().toString();
        progProfile.uuid = prefProgProfiles.getValue();
        progProfile.container = prefProgContainer.getValue();
        progProfile.transcode = prefProgTranscode.isChecked();
        progProfile.resolution = prefProgResolution.getValue();
        progProfile.audio_codec = prefProgAudioCodec.getValue();
        progProfile.video_codec = prefProgVideoCodec.getValue();
        progProfile.subtitle_codec = prefProgSubtitleCodec.getValue();

        // If the profile does not contain an id then it is a new one. Add the
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (progProfile.id == 0) {
            conn.playback_profile_id = (int) DatabaseHelper.getInstance().addProfile(progProfile);
            DatabaseHelper.getInstance().updateConnection(conn);
        } else {
            DatabaseHelper.getInstance().updateProfile(progProfile);
        }

        // Save the values into the profile
        recProfile.name = prefProgProfiles.getEntry().toString();
        recProfile.uuid = prefRecProfiles.getValue();
        recProfile.container = prefRecContainer.getValue();
        recProfile.transcode = prefRecTranscode.isChecked();
        recProfile.resolution = prefRecResolution.getValue();
        recProfile.audio_codec = prefRecAudioCodec.getValue();
        recProfile.video_codec = prefRecVideoCodec.getValue();
        recProfile.subtitle_codec = prefRecSubtitleCodec.getValue();

        // If the profile does not contain an id then it is a new one. Add the
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (recProfile.id == 0) {
            conn.recording_profile_id = (int) DatabaseHelper.getInstance().addProfile(recProfile);
            DatabaseHelper.getInstance().updateConnection(conn);
        } else {
            DatabaseHelper.getInstance().updateProfile(recProfile);
        }

        activity.setResult(Activity.RESULT_OK, activity.getIntent());
        activity.finish();
    }

    public void cancel() {
        // Show confirmation dialog to cancel
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(getString(R.string.cancel));
        builder.setTitle(getString(R.string.menu_cancel));

        // Define the action of the yes button
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Connect to the server with the selected connection and do not
                // retrieve the initial data
                activity.setResult(Activity.RESULT_OK, activity.getIntent());
                activity.finish();
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * 
     */
    private void loadProfiles() {
        if (prefRecProfiles != null && prefProgProfiles != null) {
            // Disable the settings until profiles have been loaded.
            // If the server does not support it then it stays disabled
            prefRecProfiles.setEnabled(false);
            prefProgProfiles.setEnabled(false);

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
        if (action.equals(Constants.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    if (!loading) {
                        loadProfiles();
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_GET_DVR_CONFIG)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefRecProfiles != null && app.getProtocolVersion() > 15) {
                        addProfiles(prefRecProfiles, app.getDvrConfigs());

                        // If no uuid is set, no selected profile exists.
                        // Preselect the default one.
                        if (recProfile.uuid.isEmpty()) {
                            for (Profiles p : app.getDvrConfigs()) {
                                if (p.name.equals(Constants.REC_PROFILE_DEFAULT)) {
                                    recProfile.uuid = p.uuid;
                                    break;
                                }
                            }
                        }
                        // show the currently selected profile name, if none is
                        // available then the default value is used
                        prefRecProfiles.setValue(recProfile.uuid);
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_GET_PROFILES)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefProgProfiles != null && app.getProtocolVersion() > 15) {
                        addProfiles(prefProgProfiles, app.getProfiles());
                        prefProgProfiles.setSummary(R.string.pref_playback_profiles_sum);

                        // If no uuid is set, no selected profile exists.
                        // Preselect the default one.
                        if (progProfile.uuid.isEmpty()) {
                            for (Profiles p : app.getProfiles()) {
                                if (p.name.equals(Constants.PROG_PROFILE_DEFAULT)) {
                                    progProfile.uuid = p.uuid;
                                    break;
                                }
                            }
                        }
                        // show the currently selected profile name, if none is
                        // available then the default value is used
                        prefProgProfiles.setValue(progProfile.uuid);
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_CONNECTION_STATE_SERVER_DOWN)
                || action.equals(Constants.ACTION_CONNECTION_STATE_LOST)
                || action.equals(Constants.ACTION_CONNECTION_STATE_TIMEOUT)
                || action.equals(Constants.ACTION_CONNECTION_STATE_REFUSED)
                || action.equals(Constants.ACTION_CONNECTION_STATE_AUTH)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    prefRecProfiles.setEnabled(false);
                    prefProgProfiles.setEnabled(false);
                    Toast.makeText(activity, getString(R.string.err_loading_profiles, getString(R.string.err_connect)), Toast.LENGTH_LONG).show();
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
