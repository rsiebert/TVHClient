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
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.SettingsToolbarInterface;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.Profiles;

import java.util.List;

public class SettingsCastingFragment extends PreferenceFragment implements HTSListener, BackPressedInterface, OnPreferenceClickListener, OnPreferenceChangeListener {


    private final static String TAG = SettingsCastingFragment.class.getSimpleName();

    private Activity activity;
    private SettingsToolbarInterface toolbarInterface;
    private Connection connection = null;
    private Profile castingProfile = null;
    private CheckBoxPreference castingEnabledPreference;
    private ListPreference castingProfileListPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_casting);

        activity = getActivity();
        if (activity instanceof SettingsToolbarInterface) {
            toolbarInterface = (SettingsToolbarInterface) activity;
        }

        connection = DatabaseHelper.getInstance(getActivity()).getSelectedConnection();
        toolbarInterface.setTitle(getString(R.string.pref_casting));
        toolbarInterface.setSubtitle(connection.name);

        castingEnabledPreference = (CheckBoxPreference) findPreference("pref_enable_casting");
        castingProfileListPreference = (ListPreference) findPreference("pref_cast_profiles");
        castingEnabledPreference.setOnPreferenceClickListener(this);
        castingProfileListPreference.setOnPreferenceChangeListener(this);

        castingProfile = DatabaseHelper.getInstance(getActivity()).getProfile(connection.cast_profile_id);
        if (castingProfile == null) {
            castingProfile = new Profile();
        }
        // Restore the currently selected id after an orientation change
        if (savedInstanceState != null) {
            castingProfile.uuid = savedInstanceState.getString("casting_profile_uuid");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("casting_profile_uuid", castingProfileListPreference.getValue());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
        loadProfiles();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    private void save() {
        // Save the values into the program profile (play and streaming)
        castingProfile.enabled = castingEnabledPreference.isChecked();
        castingProfile.name = (castingProfileListPreference.getEntry() != null ? castingProfileListPreference.getEntry().toString() : "");
        castingProfile.uuid = castingProfileListPreference.getValue();

        // If the profile does not contain an id then it is a new one. Add it
        // to the database and update the connection with the new id. Otherwise
        // just update the profile.
        if (castingProfile.id == 0) {
            connection.cast_profile_id = (int) DatabaseHelper.getInstance(getActivity()).addProfile(castingProfile);
            DatabaseHelper.getInstance(getActivity()).updateConnection(connection);
        } else {
            DatabaseHelper.getInstance(getActivity()).updateProfile(castingProfile);
        }
    }

    private void loadProfiles() {
        // Disable the preferences until the profiles have been loaded.
        // If the server does not support it then it stays disabled
        castingEnabledPreference.setEnabled(false);
        castingProfileListPreference.setEnabled(false);

        // Get the connection status to check if the profile can be loaded from
        // the server. If not show a message and return
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final String connectionStatus = prefs.getString("last_connection_state", "");
        if (!connectionStatus.equals(Constants.ACTION_CONNECTION_STATE_OK)) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.err_connect, Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        // Set the loading indication
        toolbarInterface.setSubtitle(getString(R.string.loading_profiles));

        // Get the available profiles from the server
        final Intent intent = new Intent(activity, HTSService.class);
        intent.setAction("getProfiles");
        activity.startService(intent);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals("getProfiles")) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    toolbarInterface.setSubtitle(connection.name);
                    addProfiles(castingProfileListPreference, DataStorage.getInstance().getProfiles());
                    castingProfileListPreference.setEnabled(castingEnabledPreference.isChecked());
                    castingEnabledPreference.setEnabled(true);
                    setCastProfile();
                }
            });
        }
    }

    private void setCastProfile() {
        // If no uuid or name is set, no selected profile 
        // exists. Preselect the default one, if it exists.
        if (castingProfile.uuid == null
            || (castingProfile.name != null && castingProfile.name.length() == 0)
            || (castingProfile.uuid != null && castingProfile.uuid.length() == 0)) {
            Logger.getInstance().log(TAG, "No valid casting profile defined in the current connection, setting default");

            for (Profiles p : DataStorage.getInstance().getProfiles()) {
                if (p.name.equals(Constants.CAST_PROFILE_DEFAULT)) {
                    castingProfile.uuid = p.uuid;
                    break;
                }
            }
        }
        // show the currently selected profile name, if none is
        // available then the default value is used
        Logger.getInstance().log(TAG, "Setting casting profile " + castingProfile.name);
        castingProfileListPreference.setValue(castingProfile.uuid);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        switch (preference.getKey()) {
            case "pref_cast_profiles":
                for (Profiles p : DataStorage.getInstance().getProfiles()) {
                    if (p.uuid.equals(o) && !p.name.equals(Constants.CAST_PROFILE_DEFAULT)) {
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
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "pref_enable_casting":
                castingProfileListPreference.setEnabled(castingEnabledPreference.isChecked());
                setCastProfile();
                return true;
        }
        return false;
    }
}
