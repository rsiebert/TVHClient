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
package org.tvheadend.tvhclient.ui.features.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import timber.log.Timber;

public class SettingsUserInterfaceFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private CheckBoxPreference programArtworkEnabledPreference;
    private CheckBoxPreference castMiniControllerPreference;
    private CheckBoxPreference multipleChannelTagsPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_ui);

        getToolbarInterface().setTitle(getString(R.string.pref_user_interface));
        getToolbarInterface().setSubtitle("");

        programArtworkEnabledPreference = findPreference("program_artwork_enabled");
        programArtworkEnabledPreference.setOnPreferenceClickListener(this);
        castMiniControllerPreference = findPreference("casting_minicontroller_enabled");
        castMiniControllerPreference.setOnPreferenceClickListener(this);
        multipleChannelTagsPreference = findPreference("multiple_channel_tags_enabled");
        multipleChannelTagsPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_ui);
    }

    @Override
    public void onResume() {
        super.onResume();
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "multiple_channel_tags_enabled":
                handlePreferenceMultipleChannelTagsSelected();
                break;
            case "program_artwork_enabled":
                handlePreferenceShowArtworkSelected();
                break;
            case "casting":
                handlePreferenceCastingSelected();
                break;
        }
        return true;
    }

    private void handlePreferenceMultipleChannelTagsSelected() {
        if (!isUnlocked()) {
            if (getView() != null) {
                SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_available_in_free_version);
            }
            multipleChannelTagsPreference.setChecked(false);
        }
    }

    private void handlePreferenceShowArtworkSelected() {
        if (!isUnlocked()) {
            if (getView() != null) {
                SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_available_in_free_version);
            }
            programArtworkEnabledPreference.setChecked(false);
        }
    }

    private void handlePreferenceCastingSelected() {
        if (getView() != null) {
            if (getHtspVersion() < 16) {
                SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_supported_by_server);
                castMiniControllerPreference.setChecked(false);
            } else if (!isUnlocked()) {
                SnackbarUtils.sendSnackbarMessage(getActivity(), R.string.feature_not_available_in_free_version);
                castMiniControllerPreference.setChecked(false);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Timber.d("Preference " + key + " has changed");
        switch (key) {
            case "hours_of_epg_data_per_screen":
                try {
                    //noinspection ConstantConditions
                    int value = Integer.parseInt(prefs.getString(key, getResources().getString(R.string.pref_default_hours_of_epg_data_per_screen)));
                    if (value < 1) {
                        ((EditTextPreference) findPreference(key)).setText("1");
                        prefs.edit().putString(key, "1").apply();
                    }
                    if (value > 24) {
                        ((EditTextPreference) findPreference(key)).setText("24");
                        prefs.edit().putString(key, "24").apply();
                    }
                } catch (NumberFormatException ex) {
                    prefs.edit().putString(key, getResources().getString(R.string.pref_default_hours_of_epg_data_per_screen)).apply();
                }
                break;

            case "days_of_epg_data":
                try {
                    //noinspection ConstantConditions
                    int value = Integer.parseInt(prefs.getString(key, getResources().getString(R.string.pref_default_days_of_epg_data)));
                    if (value < 1) {
                        ((EditTextPreference) findPreference(key)).setText("1");
                        prefs.edit().putString(key, "1").apply();
                    }
                    if (value > 14) {
                        ((EditTextPreference) findPreference(key)).setText("24");
                        prefs.edit().putString(key, "24").apply();
                    }
                } catch (NumberFormatException ex) {
                    prefs.edit().putString(key, getResources().getString(R.string.pref_default_days_of_epg_data)).apply();
                }
                break;
        }
    }
}
