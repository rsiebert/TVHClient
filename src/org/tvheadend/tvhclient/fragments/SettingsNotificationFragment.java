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

import org.tvheadend.tvhclient.PreferenceFragment;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;

public class SettingsNotificationFragment extends PreferenceFragment {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsNotificationFragment.class.getSimpleName();

    private CheckBoxPreference prefShowNotifications;
    private ListPreference prefShowNotificationOffset;

    private Activity activity;
    private TVHClientApplication app;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_notifications);

        prefShowNotifications = (CheckBoxPreference) findPreference("pref_show_notifications");
        prefShowNotificationOffset = (ListPreference) findPreference("pref_show_notification_offset");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Add a listener so that the notifications can be selected.
        prefShowNotifications.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!app.isUnlocked()) {
                    Snackbar.make(getView(), R.string.feature_not_available_in_free_version, 
                            Snackbar.LENGTH_SHORT).show();
                    prefShowNotifications.setChecked(false);
                }

                // If the checkbox is checked then add all 
                // required notifications, otherwise remove them
                if (prefShowNotifications.isChecked()) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                    final long offset = Integer.valueOf(prefs.getString("pref_show_notification_offset", "0"));
                    app.addNotifications(offset);
                } else {
                    app.cancelNotifications();
                }

                return true;
            }
        });

        prefShowNotificationOffset.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object offset) {
                // Refresh all notifications by removing adding them again
                app.cancelNotifications();
                app.addNotifications(Long.valueOf((String) offset));
                return true;
            }
        });
    }
}
