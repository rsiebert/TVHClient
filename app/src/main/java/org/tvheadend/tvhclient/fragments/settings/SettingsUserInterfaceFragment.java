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

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.TaskStackBuilder;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.NavigationDrawerActivity;
import org.tvheadend.tvhclient.activities.ToolbarInterfaceLight;

public class SettingsUserInterfaceFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private CheckBoxPreference prefShowProgramArtwork;
    private CheckBoxPreference prefTheme;
    private ToolbarInterfaceLight toolbarInterface;
    private boolean isUnlocked;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_ui);

        if (getActivity() instanceof ToolbarInterfaceLight) {
            toolbarInterface = (ToolbarInterfaceLight) getActivity();
        }
        toolbarInterface.setTitle(getString(R.string.pref_user_interface));
        isUnlocked = TVHClientApplication.getInstance().isUnlocked();

        prefShowProgramArtwork = (CheckBoxPreference) findPreference("pref_show_program_artwork");
        prefTheme = (CheckBoxPreference) findPreference("lightThemePref");
        prefShowProgramArtwork.setOnPreferenceClickListener(this);
        prefTheme.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "showIconPref":
                handlePreferenceShowIconsSelected();
                break;
            case "lightThemePref":
                handlePreferenceThemeSelected();
                break;
            case "pref_show_program_artwork":
                handlePreferenceShowArtworkSelected();
                break;
        }
        return true;
    }

    private void handlePreferenceShowIconsSelected() {
        // TODO Reload all icons from the server

    }

    private void handlePreferenceThemeSelected() {
        TaskStackBuilder.create(getActivity())
                .addNextIntent(new Intent(getActivity(), NavigationDrawerActivity.class))
                .addNextIntent(getActivity().getIntent())
                .startActivities();
    }

    private void handlePreferenceShowArtworkSelected() {
        if (isUnlocked) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version, Snackbar.LENGTH_SHORT).show();
            }
            prefShowProgramArtwork.setChecked(false);
        }
    }


}
