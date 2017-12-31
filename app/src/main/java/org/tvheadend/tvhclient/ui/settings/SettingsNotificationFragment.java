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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import org.tvheadend.tvhclient.data.local.NotificationHandler;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;

public class SettingsNotificationFragment extends PreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener {

    private Activity activity;
    private ToolbarInterface toolbarInterface;
    private CheckBoxPreference prefShowNotifications;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_notifications);

        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        toolbarInterface.setTitle(getString(R.string.pref_notifications));

        prefShowNotifications = (CheckBoxPreference) findPreference("pref_show_notifications");
        ListPreference prefShowNotificationOffset = (ListPreference) findPreference("pref_show_notification_offset");
        prefShowNotifications.setOnPreferenceClickListener(this);
        prefShowNotificationOffset.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "pref_show_notifications":
                // Add all notifications if the preference is checked, otherwise
                // remove all existing in case it has been unchecked
                if (prefShowNotifications.isChecked()) {
                    long offset = 0;
                    try {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                        offset = Integer.valueOf(prefs.getString("pref_show_notification_offset", "0"));
                    } catch(NumberFormatException ex) {
                        // NOP
                    }
                    NotificationHandler.getInstance().addNotifications(offset);
                } else {
                    NotificationHandler.getInstance().cancelNotifications();
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        switch (preference.getKey()) {
            case "pref_show_notification_offset":
                long offset = 0;
                try {
                    offset = Long.valueOf((String) o);
                } catch (NumberFormatException ex) {
                    // NOP
                }
                // The offset has changes refresh all existing
                // notifications by removing adding them again
                NotificationHandler.getInstance().cancelNotifications();
                NotificationHandler.getInstance().addNotifications(offset);
                return true;
        }
        return false;
    }
}
