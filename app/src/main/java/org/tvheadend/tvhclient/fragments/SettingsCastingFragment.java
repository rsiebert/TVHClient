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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Profiles;

import java.util.List;

public class SettingsCastingFragment extends PreferenceFragment implements HTSListener, BackPressedInterface {


    private final static String TAG = SettingsCastingFragment.class.getSimpleName();
    private static final String CAST_PROFILE_UUID = "cast_profile_uuid";

    private Activity activity;
    private ActionBarInterface actionBarInterface;
    private Connection conn = null;
    private Profile castProfile = null;
    private CheckBoxPreference prefEnableCasting;
    private ListPreference prefCastProfiles;
    private TVHClientApplication app;
    private DatabaseHelper dbh;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_casting);

        prefEnableCasting = (CheckBoxPreference) findPreference("pref_enable_casting");
        prefCastProfiles = (ListPreference) findPreference("pref_cast_profiles");

        conn = dbh.getSelectedConnection();
        castProfile = dbh.getProfile(conn.cast_profile_id);
        if (castProfile == null) {
            app.log(TAG, "No casting profile defined in the connection");
            castProfile = new Profile();
        } else {
            app.log(TAG, "Casting profile " + castProfile.name + ", " + castProfile.uuid + " is defined in the connection");
        }

        // If the state is null then this activity has been started for
        // the first time. If the state is not null then the screen has
        // been rotated and we have to reuse the values.
        if (savedInstanceState != null) {
            castProfile.uuid = savedInstanceState.getString(CAST_PROFILE_UUID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(CAST_PROFILE_UUID, prefCastProfiles.getValue());
        super.onSaveInstanceState(outState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        app = (TVHClientApplication) activity.getApplication();
        dbh = DatabaseHelper.getInstance(activity);
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
            actionBarInterface.setActionBarTitle(getString(R.string.pref_casting));
        }
        
        prefEnableCasting.setOnPreferenceClickListener((new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                prefCastProfiles.setEnabled(prefEnableCasting.isChecked());

                // try to set the default profile
                setCastProfile();
                return false;
            }
        }));

        prefCastProfiles.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                for (Profiles p : app.getProfiles()) {
                    if (p.uuid.equals(newValue) && !p.name.equals(Constants.CAST_PROFILE_DEFAULT)) {
                        new MaterialDialog.Builder(activity)
                                .content(getString(R.string.cast_profile_invalid, p.name, Constants.CAST_PROFILE_DEFAULT))
                                .positiveText(android.R.string.ok)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.cancel();
                                    }
                                }).show();
                    }
                }
                return true;
            }
        });
    }

    public void onResume() {
        super.onResume();
        app.addListener(this);

        if (actionBarInterface != null) {
            actionBarInterface.setActionBarSubtitle(conn.name);
        }

        loadProfiles();
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    private void save() {
        // Save the values into the program profile (play and streaming)
        castProfile.enabled = prefEnableCasting.isChecked();
        castProfile.name = (prefCastProfiles.getEntry() != null ? prefCastProfiles.getEntry().toString() : "");
        castProfile.uuid = prefCastProfiles.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (castProfile.id == 0) {
            conn.cast_profile_id = (int) dbh.addProfile(castProfile);
            dbh.updateConnection(conn);
        } else {
            dbh.updateProfile(castProfile);
        }
    }

    /**
     * 
     */
    private void loadProfiles() {
        if (prefCastProfiles == null) {
            return;
        }

        // Disable the preferences until the profiles have been loaded.
        // If the server does not support it then it stays disabled
        prefEnableCasting.setEnabled(false);
        prefCastProfiles.setEnabled(false);

        // Get the connection status to check if the profile can be loaded from
        // the server. If not show a message and return
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final String connectionStatus = prefs.getString(Constants.LAST_CONNECTION_STATE, "");
        if (!connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.err_connect, Snackbar.LENGTH_LONG).show();
            }
            return;
        }

        // Set the loading indication
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarSubtitle(getString(R.string.loading_profiles));
        }

        // Get the available profiles from the server
        final Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_GET_PROFILES);
        activity.startService(intent);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_GET_PROFILES)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    // Loading is done, remove the loading subtitle
                    if (actionBarInterface != null) {
                        actionBarInterface.setActionBarSubtitle(conn.name);
                    }

                    if (prefCastProfiles != null && prefEnableCasting != null) {
                        addProfiles(prefCastProfiles, app.getProfiles());
                        prefCastProfiles.setEnabled(prefEnableCasting.isChecked());
                        prefEnableCasting.setEnabled(true);
                        setCastProfile();
                    }
                }
            });
        }
    }

    private void setCastProfile() {
        // If no uuid or name is set, no selected profile 
        // exists. Preselect the default one, if it exists.
        if (castProfile.uuid == null
            || (castProfile.name != null && castProfile.name.length() == 0)
            || (castProfile.uuid != null && castProfile.uuid.length() == 0)) {
            app.log(TAG, "No valid casting profile defined in the current connection, setting default");

            for (Profiles p : app.getProfiles()) {
                if (p.name.equals(Constants.CAST_PROFILE_DEFAULT)) {
                    castProfile.uuid = p.uuid;
                    break;
                }
            }
        }
        // show the currently selected profile name, if none is
        // available then the default value is used
        app.log(TAG, "Setting casting profile " + castProfile.name);
        prefCastProfiles.setValue(castProfile.uuid);
    }

    /**
     * Adds the names and uuids of the available profiles to the preference widget
     *
     * @param preferenceList Preference list widget that shall hold the values
     * @param profileList Profile list with the data
     */
    private void addProfiles(ListPreference preferenceList, final List<Profiles> profileList) {
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
        save();
    }
}
