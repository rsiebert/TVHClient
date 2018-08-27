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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.design.widget.Snackbar;

import org.tvheadend.tvhclient.R;

public class SettingsPlaybackFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    private CheckBoxPreference internalPlayerPreference;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_playback);

        toolbarInterface.setTitle("Playback");
        toolbarInterface.setSubtitle("");

        internalPlayerPreference = (CheckBoxPreference) findPreference("internal_player_enabled");
        internalPlayerPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "playback":
                handlePreferenceInternalPlayerSelected();
                break;
        }
        return false;
    }

    private void handlePreferenceInternalPlayerSelected() {
        if (getView() != null) {
            if (!isUnlocked) {
                Snackbar.make(getView(), R.string.feature_not_available_in_free_version, Snackbar.LENGTH_SHORT).show();
                internalPlayerPreference.setChecked(false);
            }
        }
    }
}
