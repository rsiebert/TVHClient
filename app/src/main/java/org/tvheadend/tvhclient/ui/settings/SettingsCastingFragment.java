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

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.Constants;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.repository.ConnectionDataRepository;
import org.tvheadend.tvhclient.data.repository.ProfileDataRepository;
import org.tvheadend.tvhclient.data.repository.ServerDataRepository;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;

import java.util.List;

public class SettingsCastingFragment extends PreferenceFragment implements BackPressedInterface, OnPreferenceClickListener, OnPreferenceChangeListener {

    private Activity activity;
    private ToolbarInterface toolbarInterface;
    private ServerProfile castingServerProfile = null;
    private CheckBoxPreference castingEnabledPreference;
    private ListPreference castingProfileListPreference;
    private ProfileDataRepository profileRepository;
    private ServerDataRepository serverRepository;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_casting);

        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        Connection connection = new ConnectionDataRepository(activity).getActiveConnectionSync();
        toolbarInterface.setTitle(getString(R.string.pref_casting));
        toolbarInterface.setSubtitle(connection.getName());

        castingEnabledPreference = (CheckBoxPreference) findPreference("pref_enable_casting");
        castingProfileListPreference = (ListPreference) findPreference("pref_cast_profiles");
        castingEnabledPreference.setOnPreferenceClickListener(this);
        castingProfileListPreference.setOnPreferenceChangeListener(this);

        serverRepository = new ServerDataRepository(activity);
        profileRepository = new ProfileDataRepository(activity);
        castingServerProfile = profileRepository.getCastingServerProfile();

        addProfiles(profileRepository.getAllPlaybackServerProfiles());

        // Restore the currently selected id after an orientation change
        if (savedInstanceState != null) {
            castingServerProfile.setUuid(savedInstanceState.getString("casting_profile_uuid"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("casting_profile_uuid", castingProfileListPreference.getValue());
        super.onSaveInstanceState(outState);
    }

    private void save() {
        // Save the values into the program profile (play and streaming)
        castingServerProfile.setEnabled(castingEnabledPreference.isChecked());
        castingServerProfile.setName((castingProfileListPreference.getEntry() != null ? castingProfileListPreference.getEntry().toString() : ""));
        castingServerProfile.setUuid(castingProfileListPreference.getValue());

        serverRepository.updateCastingServerProfile(castingServerProfile.getId());
    }

    @Override
    public void onBackPressed() {
        save();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        switch (preference.getKey()) {
            case "pref_cast_profiles":
                for (ServerProfile p : profileRepository.getAllPlaybackServerProfiles()) {
                    if (p.getUuid().equals(o) && !p.getName().equals(Constants.CAST_PROFILE_DEFAULT)) {
                        new MaterialDialog.Builder(activity)
                                .content(getString(R.string.cast_profile_invalid, p.getName(), Constants.CAST_PROFILE_DEFAULT))
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
                castingProfileListPreference.setValue(castingServerProfile.getUuid());
                return true;
        }
        return false;
    }

    private void addProfiles(final List<ServerProfile> serverProfileList) {
        // Initialize the arrays that contain the profile values
        final int size = serverProfileList.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] entryValues = new CharSequence[size];

        // Add the available profiles to list preference
        for (int i = 0; i < size; i++) {
            entries[i] = serverProfileList.get(i).getName();
            entryValues[i] = serverProfileList.get(i).getUuid();
        }
        castingProfileListPreference.setEntries(entries);
        castingProfileListPreference.setEntryValues(entryValues);
    }
}
