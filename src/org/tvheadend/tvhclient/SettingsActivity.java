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

import org.tvheadend.tvhclient.fragments.SettingsFragment;
import org.tvheadend.tvhclient.fragments.SettingsManageConnectionFragment;
import org.tvheadend.tvhclient.fragments.SettingsShowConnectionsFragment;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class SettingsActivity extends ActionBarActivity implements SettingsInterface {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsActivity.class.getSimpleName();

    private Toolbar toolbar = null;
    private Fragment fragment;

    private static boolean restart = false;
    private static boolean reconnect = false;
    private boolean manageConnections = false;

    private final static int MAIN_SETTINGS = 1;
    private final static int MANAGE_CONNECTIONS = 2;
    private final static int EDIT_CONNECTION = 3;
    private final static int ADD_CONNECTION = 4;
    private final static int MAIN_SETTINGS_GENRE_COLORS = 5;
    private final static int MAIN_SETTINGS_PROGRAM_GUIDE = 6;
    private final static int MAIN_SETTINGS_MENU_VISIBILITY = 7;
    private final static int MAIN_SETTINGS_PLAYBACK_PROGRAMS = 8;
    private final static int MAIN_SETTINGS_PLAYBACK_RECORDINGS = 9;

    private int currentSettingsMode = MAIN_SETTINGS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);
        setContentView(R.layout.settings_layout);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menu_settings);

        // Get any saved values from the bundle
        if (savedInstanceState != null) {
            currentSettingsMode = savedInstanceState.getInt(Constants.BUNDLE_SETTINGS_MODE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(Constants.BUNDLE_SETTINGS_MODE, currentSettingsMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // When the orientation was changed the last visible fragment is
        // available from the manager. If this is the case get it and show it
        // again.
        fragment = (Fragment) getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
        if (fragment == null) {
            // Get the information if the connection fragment shall be shown.
            // This is the case when the user has selected the connection menu
            // from the navigation drawer.
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                manageConnections = bundle.getBoolean(Constants.BUNDLE_MANAGE_CONNECTIONS, false);
            }
            // Now show the manage connection or the general settings fragment
            if (manageConnections) {
                manageConnections();
            } else {
                mainSettings();
            }
        } else {
            // Show the available fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
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
        // Depending on the current mode either show the previous settings
        // screen or exit the settings.
        if (currentSettingsMode == MAIN_SETTINGS) {
            restartNow();
        } else if (currentSettingsMode == MANAGE_CONNECTIONS) {
            if (manageConnections) {
                restartNow();
            } else {
                mainSettings();
            }
        } else if (currentSettingsMode == ADD_CONNECTION 
                || currentSettingsMode == EDIT_CONNECTION) {
            manageConnections();
        } else if (currentSettingsMode == MAIN_SETTINGS_GENRE_COLORS || 
                currentSettingsMode == MAIN_SETTINGS_PROGRAM_GUIDE ||
                currentSettingsMode == MAIN_SETTINGS_MENU_VISIBILITY ||
                currentSettingsMode == MAIN_SETTINGS_PLAYBACK_PROGRAMS ||
                currentSettingsMode == MAIN_SETTINGS_PLAYBACK_RECORDINGS) {
            mainSettings();
        }
    }

    @Override
    public void restart() {
        restart = true;
    }

    @Override
    public void reconnect() {
        reconnect = true;
    }

    @Override
    public void restartNow() {
        Intent intent = getIntent();
        intent.putExtra(Constants.BUNDLE_RESTART, restart);
        intent.putExtra(Constants.BUNDLE_RECONNECT, reconnect);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void mainSettings() {
        currentSettingsMode = MAIN_SETTINGS;
        removePreviousFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, new SettingsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void manageConnections() {
        currentSettingsMode = MANAGE_CONNECTIONS;
        removePreviousFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, new SettingsShowConnectionsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void addConnection() {
        currentSettingsMode = ADD_CONNECTION;
        removePreviousFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, new SettingsManageConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void editConnection(long id) {
        currentSettingsMode = EDIT_CONNECTION;
        removePreviousFragment();

        Bundle bundle = new Bundle();
        bundle.putLong(Constants.BUNDLE_CONNECTION_ID, id);
        Fragment f = Fragment.instantiate(this, SettingsManageConnectionFragment.class.getName());
        f.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    /**
     * Removes the previous fragment from the view and back stack so that
     * navigating back would not show the old fragment again.
     */
    private void removePreviousFragment() {
        Fragment f = (Fragment) getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
        if (f != null) {
            getSupportFragmentManager().beginTransaction().remove(f).commit();
        }
    }

    @Override
    public void mainSettingsGenreColors() {
        currentSettingsMode = MAIN_SETTINGS_GENRE_COLORS;
        removePreviousFragment();

        Fragment f = Fragment.instantiate(this, SettingsFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.BUNDLE_SETTINGS_PREFS, R.xml.preferences_genre_colors);
        f.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void mainSettingsProgramGuide() {
        currentSettingsMode = MAIN_SETTINGS_PROGRAM_GUIDE;
        removePreviousFragment();

        Fragment f = Fragment.instantiate(this, SettingsFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.BUNDLE_SETTINGS_PREFS, R.xml.preferences_program_guide);
        f.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void mainSettingsMenuVisibility() {
        currentSettingsMode = MAIN_SETTINGS_MENU_VISIBILITY;
        removePreviousFragment();

        Fragment f = Fragment.instantiate(this, SettingsFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.BUNDLE_SETTINGS_PREFS, R.xml.preferences_menu_visibility);
        f.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void mainSettingsPlaybackPrograms() {
        currentSettingsMode = MAIN_SETTINGS_PLAYBACK_PROGRAMS;
        removePreviousFragment();

        Fragment f = Fragment.instantiate(this, SettingsFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.BUNDLE_SETTINGS_PREFS, R.xml.preferences_playback_programs);
        f.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void mainSettingsPlaybackRecordings() {
        currentSettingsMode = MAIN_SETTINGS_PLAYBACK_RECORDINGS;
        removePreviousFragment();

        Fragment f = Fragment.instantiate(this, SettingsFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.BUNDLE_SETTINGS_PREFS, R.xml.preferences_playback_recordings);
        f.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }
}
