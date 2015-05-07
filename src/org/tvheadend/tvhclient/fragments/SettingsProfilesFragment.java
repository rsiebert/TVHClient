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
import org.tvheadend.tvhclient.PreferenceFragment;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Profiles;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsProfilesFragment extends PreferenceFragment implements HTSListener, OnPreferenceChangeListener, BackPressedInterface {

    private final static String TAG = SettingsProfilesFragment.class.getSimpleName();

    private Activity activity;
    private ActionBarInterface actionBarInterface;
    private SettingsInterface settingsInterface;

    private Connection conn = null;
    private Profile progProfile = null;
    private Profile recProfile = null;

    private CheckBoxPreference prefEnableRecProfiles;
    private CheckBoxPreference prefEnableProgProfiles;
    private ListPreference prefRecProfiles;
    private ListPreference prefProgProfiles;

    private boolean settingsHaveChanged = false;

    private static final String ENABLE_PROG_PROFILE = "enable_prog_profile";
    private static final String ENABLE_REC_PROFILE = "enable_rec_profile";
    private static final String PROG_PROFILE_UUID = "prog_profile_uuid";
    private static final String REC_PROFILE_UUID = "rec_profile_uuid";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_profiles);

        prefEnableRecProfiles = (CheckBoxPreference) findPreference("pref_enable_recording_profiles");
        prefEnableProgProfiles = (CheckBoxPreference) findPreference("pref_enable_playback_profiles");
        prefRecProfiles = (ListPreference) findPreference("pref_recording_profiles");
        prefProgProfiles = (ListPreference) findPreference("pref_playback_profiles");

        conn = DatabaseHelper.getInstance().getSelectedConnection();
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

        // If the state is null then this activity has been started for
        // the first time. If the state is not null then the screen has
        // been rotated and we have to reuse the values.
        if (savedInstanceState != null) {
            prefEnableProgProfiles.setChecked(savedInstanceState.getBoolean(ENABLE_PROG_PROFILE));
            prefEnableRecProfiles.setChecked(savedInstanceState.getBoolean(ENABLE_REC_PROFILE));
            progProfile.uuid = savedInstanceState.getString(PROG_PROFILE_UUID);
            recProfile.uuid = savedInstanceState.getString(REC_PROFILE_UUID);
        }

        prefEnableRecProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prefRecProfiles.setEnabled(prefEnableRecProfiles.isChecked());
                return false;
            }
        });
        prefEnableProgProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prefProgProfiles.setEnabled(prefEnableProgProfiles.isChecked());
                return false;
            }
        });
        prefRecProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return false;
            }
        });
        prefProgProfiles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return false;
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ENABLE_PROG_PROFILE, prefEnableProgProfiles.isChecked());
        outState.putBoolean(ENABLE_REC_PROFILE, prefEnableRecProfiles.isChecked());
        outState.putString(PROG_PROFILE_UUID, prefProgProfiles.getValue());
        outState.putString(REC_PROFILE_UUID, prefRecProfiles.getValue());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;
    }

    @Override
    public void onDetach() {
        actionBarInterface = null;
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.pref_profiles), TAG);
        }
        if (activity instanceof SettingsInterface) {
            settingsInterface = (SettingsInterface) activity;
        }
        setHasOptionsMenu(true);
    }

    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);

        // If no connection exists exit
        if (conn == null) {
            if (settingsInterface != null) {
                settingsInterface.done(Activity.RESULT_CANCELED);
            }
        }

        prefEnableProgProfiles.setOnPreferenceChangeListener((OnPreferenceChangeListener) this);
        prefEnableRecProfiles.setOnPreferenceChangeListener((OnPreferenceChangeListener) this);
        prefProgProfiles.setOnPreferenceChangeListener((OnPreferenceChangeListener) this);
        prefRecProfiles.setOnPreferenceChangeListener((OnPreferenceChangeListener) this);

        settingsHaveChanged = false;
        loadProfiles();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.save_cancel_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            cancel();
            return true;

        case R.id.menu_save:
            save();
            return true;

        case R.id.menu_cancel:
            cancel();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void save() {
        // Save the values into the profile
        progProfile.enabled = prefEnableProgProfiles.isChecked();
        progProfile.name = prefProgProfiles.getEntry().toString();
        progProfile.uuid = prefProgProfiles.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (progProfile.id == 0) {
            conn.playback_profile_id = (int) DatabaseHelper.getInstance().addProfile(progProfile);
            DatabaseHelper.getInstance().updateConnection(conn);
        } else {
            DatabaseHelper.getInstance().updateProfile(progProfile);
        }

        // Save the values into the profile
        recProfile.enabled = prefEnableRecProfiles.isChecked();
        recProfile.name = prefProgProfiles.getEntry().toString();
        recProfile.uuid = prefRecProfiles.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (recProfile.id == 0) {
            conn.recording_profile_id = (int) DatabaseHelper.getInstance().addProfile(recProfile);
            DatabaseHelper.getInstance().updateConnection(conn);
        } else {
            DatabaseHelper.getInstance().updateProfile(recProfile);
        }

        if (settingsInterface != null) {
            settingsInterface.done(Activity.RESULT_OK);
        }
    }

    public void cancel() {
        // Quit immediately if nothing has changed 
        if (!settingsHaveChanged) {
            if (settingsInterface != null) {
                settingsInterface.done(Activity.RESULT_CANCELED);
                return;
            }
        }
        // Show confirmation dialog to cancel
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(getString(R.string.confirm_discard_profile));

        // Define the action of the yes button
        builder.setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                activity.finish();
            }
        });
        // Define the action of the no button
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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
        if (prefRecProfiles == null || prefProgProfiles == null) {
            return;
        }

        // Disable the preferences until the profiles have been loaded.
        // If the server does not support it then it stays disabled
        prefEnableRecProfiles.setEnabled(false);
        prefEnableProgProfiles.setEnabled(false);
        prefRecProfiles.setEnabled(false);
        prefProgProfiles.setEnabled(false);

        // Get the connection status to check if the profile can be loaded from
        // the server. If not show a message and return
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final String connectionStatus = prefs.getString(Constants.LAST_CONNECTION_STATE, "");
        if (!connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            Toast.makeText(activity, getString(R.string.err_connect), Toast.LENGTH_SHORT).show();
            return;
        }

        // Set the loading indication
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.loading_profiles), TAG);
        }

        // Get the available profiles from the server
        final Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_GET_DVR_CONFIG);
        activity.startService(intent);
        intent.setAction(Constants.ACTION_GET_PROFILES);
        activity.startService(intent);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_GET_DVR_CONFIG)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Loading is done, remove the loading subtitle
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarTitle(getString(R.string.settings), TAG);
                    }

                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefRecProfiles != null && prefEnableRecProfiles != null) {
                        addProfiles(prefRecProfiles, app.getDvrConfigs());

                        prefRecProfiles.setEnabled(prefEnableRecProfiles.isChecked());
                        prefEnableRecProfiles.setEnabled(true);

                        // If no uuid is set, no selected profile exists.
                        // Preselect the default one.
                        if (recProfile.uuid.length() == 0) {
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
                    // Loading is done, remove the loading subtitle
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarTitle(getString(R.string.settings), TAG);
                    }

                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (prefProgProfiles != null && prefEnableProgProfiles != null) {
                        addProfiles(prefProgProfiles, app.getProfiles());
                        prefProgProfiles.setEnabled(prefEnableProgProfiles.isChecked());
                        prefEnableProgProfiles.setEnabled(true);

                        // If no uuid is set, no selected profile exists.
                        // Preselect the default one.
                        if (progProfile.uuid.length() == 0) {
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
    }

    @Override
    public void onBackPressed() {
        cancel();
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        settingsHaveChanged = true;
        return true;
    }
}
