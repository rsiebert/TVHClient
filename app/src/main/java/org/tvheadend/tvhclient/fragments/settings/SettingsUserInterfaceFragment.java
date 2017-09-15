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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;

public class SettingsUserInterfaceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsUserInterfaceFragment.class.getSimpleName();

    private Activity activity;
    private SettingsInterface settingsInterface;
    private CheckBoxPreference prefShowProgramArtwork;
    private TVHClientApplication app;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_ui);

        prefShowProgramArtwork = (CheckBoxPreference) findPreference("pref_show_program_artwork");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public void onDetach() {
        settingsInterface = null;
        super.onDetach();
    }

    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof SettingsInterface) {
            settingsInterface = (SettingsInterface) activity;
        }

        // Add a listener to the logger will be enabled or disabled depending on the setting
        prefShowProgramArtwork.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!app.isUnlocked()) {
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.feature_not_available_in_free_version,
                                Snackbar.LENGTH_SHORT).show();
                    }
                    prefShowProgramArtwork.setChecked(false);
                }
                return false;
            }
        });
    }

    /**
     * Show a notification to the user in case the theme or language
     * preference has changed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "lightThemePref":
                if (settingsInterface != null) {
                    settingsInterface.restart();
                    settingsInterface.restartNow();
                }
                break;
            case "epgMaxDays":
            case "epgHoursVisible":
                if (settingsInterface != null) {
                    settingsInterface.restart();
                }
                break;
        }
        // Reload the data to fetch the channel icons. They are not loaded
        // (to save bandwidth) when not required.
        if (key.equals("showIconPref")) {
            if (settingsInterface != null) {
                settingsInterface.reconnect();
            }
        }
    }
}
