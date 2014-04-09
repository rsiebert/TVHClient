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
package org.tvheadend.tvhclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class SettingsActivity extends ActionBarActivity {

    private ActionBar actionBar = null;
    private static boolean restart = false;
    private static boolean reconnect = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);

        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.menu_settings);

        // Show the specified fragment
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
            
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("restart", restart);
        returnIntent.putExtra("reconnect", reconnect);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Set the default values and then load the preferences from the XML resource
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
            addPreferencesFromResource(R.xml.preferences);
            
            // Add a listener to the connection preference so that the 
            // SettingsManageConnectionsActivity can be shown.
            Preference prefManage = findPreference("pref_manage_connections");
            prefManage.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), SettingsManageConnectionsActivity.class);
                    startActivityForResult(intent, Constants.RESULT_CODE_CONNECTIONS);
                    return false;
                }
            });
            
            // Add a listener to the connection preference so that the 
            // ChangeLogDialog with all changes can be shown.
            Preference prefChangelog = findPreference("pref_changelog");
            prefChangelog.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final ChangeLogDialog cld = new ChangeLogDialog(getActivity());
                    cld.getFullLogDialog().show();
                    return false;
                }
            });
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

        /**
         * Show a notification to the user in case the theme or language
         * preference has changed.
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("lightThemePref") || key.equals("languagePref")) {
                restart = true;
                ((SettingsActivity) getActivity()).restartActivity();
            } else if (key.equals("epgMaxDays") || key.equals("epgHoursVisible")) {
                restart = true;
            }
            // Reload the data to fetch the channel icons. They are not loaded
            // (to save bandwidth) when not required.  
            if (key.equals("showIconPref")) {
                Utils.connect(getActivity(), true);
            }
        }
        
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == Constants.RESULT_CODE_SETTINGS) {
                if (resultCode == RESULT_OK) {
                    reconnect = data.getBooleanExtra("reconnect", false);
                }
            }
        }
    }

    /**
     * Restarts the current settings activity to load the newly defined theme or
     * language
     */
    private void restartActivity() {
        Intent intent = getIntent();
        intent.putExtra("restart", restart);
        intent.putExtra("reconnect", reconnect);
        setResult(RESULT_OK, intent);
        finish();
        startActivityForResult(intent, Constants.RESULT_CODE_SETTINGS);
    }
}
