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
package org.tvheadend.tvhclient.features.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.TaskStackBuilder;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.features.navigation.NavigationActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

public class SettingsUserInterfaceFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private CheckBoxPreference programArtworkEnabledPreference;
    private ToolbarInterface toolbarInterface;
    private boolean isUnlocked;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_ui);

        if (getActivity() instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) getActivity();
        }
        toolbarInterface.setTitle(getString(R.string.pref_user_interface));
        isUnlocked = MainApplication.getInstance().isUnlocked();

        programArtworkEnabledPreference = (CheckBoxPreference) findPreference("program_artwork_enabled");
        CheckBoxPreference lightThemeEnabledPreference = (CheckBoxPreference) findPreference("light_theme_enabled");
        programArtworkEnabledPreference.setOnPreferenceClickListener(this);
        lightThemeEnabledPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "light_theme_enabled":
                handlePreferenceThemeSelected();
                break;
            case "program_artwork_enabled":
                handlePreferenceShowArtworkSelected();
                break;
        }
        return true;
    }

    private void handlePreferenceThemeSelected() {
        TaskStackBuilder.create(getActivity())
                .addNextIntent(new Intent(getActivity(), NavigationActivity.class))
                .addNextIntent(getActivity().getIntent())
                .startActivities();
    }

    private void handlePreferenceShowArtworkSelected() {
        if (isUnlocked) {
            if (getView() != null) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version, Snackbar.LENGTH_SHORT).show();
            }
            programArtworkEnabledPreference.setChecked(false);
        }
    }


}
