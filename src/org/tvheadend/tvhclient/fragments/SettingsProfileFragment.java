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
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentActivity;

public class SettingsProfileFragment extends PreferenceFragment implements HTSListener {

    private final static String TAG = SettingsProfileFragment.class.getSimpleName();

    private Activity activity;
    private Connection conn;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_profiles);

        // Get the connection where the profiles shall be edited
        Bundle bundle = getArguments();
        if (bundle != null) {
            long id = bundle.getLong(Constants.BUNDLE_CONNECTION_ID, 0);
            conn = DatabaseHelper.getInstance().getConnection(id);
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

        ListPreference prefRecordingProfiles = (ListPreference) findPreference("pref_recording_profiles");
        ListPreference prefPlaybackProfiles = (ListPreference) findPreference("pref_playback_profiles");

        if (prefRecordingProfiles != null && prefPlaybackProfiles != null) {
            // Disable the settings until profiles have been loaded.
            // If the server does not support it then it stays disabled
            prefRecordingProfiles.setEnabled(false);
            prefPlaybackProfiles.setEnabled(false);

            prefRecordingProfiles.setSummary(R.string.loading_profile);
            prefPlaybackProfiles.setSummary(R.string.loading_profile);

            // Hide or show the available profile menu items depending on
            // the server version
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
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void save() {
        // TODO Auto-generated method stub

    }

    public void cancel() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_GET_DVR_CONFIG)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ListPreference prefProfile = (ListPreference) findPreference("pref_recording_profiles");
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefProfile != null && app.getProtocolVersion() > 15) {
                        addProfiles(prefProfile, app.getDvrConfigs());
                        prefProfile.setSummary(R.string.pref_recording_profiles_sum);
                    }
                }
            });
        } else if (action.equals(Constants.ACTION_GET_PROFILES)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ListPreference prefProfile = (ListPreference) findPreference("pref_playback_profiles");
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefProfile != null && app.getProtocolVersion() > 15) {
                        addProfiles(prefProfile, app.getProfiles());
                        prefProfile.setSummary(R.string.pref_playback_profiles_sum);

                        PreferenceScreen prefPlaybackPrograms = (PreferenceScreen) findPreference("pref_playback_programs");
                        if (prefPlaybackPrograms != null) {
                            prefPlaybackPrograms.setEnabled(false);
                        }
                        PreferenceScreen prefPlaybackRecordings = (PreferenceScreen) findPreference("pref_playback_recordings");
                        if (prefPlaybackRecordings != null) {
                            prefPlaybackRecordings.setEnabled(false);
                        }
                    }
                }
            });
        }
    }

    /**
     * 
     * @param listPref
     * @param profileList
     */
    protected void addProfiles(ListPreference listPref, final List<Profiles> profileList) {
        // Initialize the arrays that contain the profile values
        final int size = profileList.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] entryValues = new CharSequence[size];

        // Add the available profiles to list preference
        for (int i = 0; i < size; i++) {
            entries[i] = profileList.get(i).name;
            entryValues[i] = profileList.get(i).uuid;
        }
        listPref.setEntries(entries);
        listPref.setEntryValues(entryValues);

        // Enable the preference for use selection
        listPref.setEnabled(true);
    }
}
