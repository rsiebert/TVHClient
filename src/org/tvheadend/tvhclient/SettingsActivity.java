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
import org.tvheadend.tvhclient.interfaces.SettingsInterface;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SettingsActivity extends ActionBarActivity implements SettingsInterface {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsActivity.class.getSimpleName();

    private Toolbar toolbar = null;
    private Fragment fragment;

    private static boolean restart = false;
    private static boolean reconnect = false;

    private final static int MAIN_SETTINGS = 1;
    private final static int MAIN_SETTINGS_OTHERS = 2;

    private int currentSettingsMode = MAIN_SETTINGS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);
        setContentView(R.layout.settings_layout);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menu_settings);
        toolbar.setNavigationIcon((Utils.getThemeId(this) == R.style.CustomTheme_Light) ? R.drawable.ic_menu_back_light
                : R.drawable.ic_menu_back_dark);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

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
        fragment = (Fragment) getFragmentManager().findFragmentById(R.id.settings_fragment);
        if (fragment == null) {
            mainSettings();
        } else {
            // Show the available fragment
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        // Depending on the current mode either show the previous settings
        // screen or exit the settings.
        if (currentSettingsMode == MAIN_SETTINGS) {
            restartActivity();
        } else {
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
    public void restartActivity() {
        Intent intent = getIntent();
        intent.putExtra(Constants.BUNDLE_RESTART, restart);
        intent.putExtra(Constants.BUNDLE_RECONNECT, reconnect);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 
     */
    private void mainSettings() {
        currentSettingsMode = MAIN_SETTINGS;

        removePreviousFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, new SettingsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void manageConnections() {
        currentSettingsMode = MAIN_SETTINGS_OTHERS;

        removePreviousFragment();
        Intent settingsIntent = new Intent(this, SettingsShowConnectionsActivity.class);
        startActivity(settingsIntent);
    }

    /**
     * Removes the previous fragment from the view and back stack so that
     * navigating back would not show the old fragment again.
     */
    private void removePreviousFragment() {
        Fragment f = (Fragment) getFragmentManager().findFragmentById(R.id.settings_fragment);
        if (f != null) {
            getFragmentManager().beginTransaction().remove(f).commit();
        }
    }

    @Override
    public void showPreference(int pref) {
        currentSettingsMode = MAIN_SETTINGS_OTHERS;

        Fragment f = Fragment.instantiate(this, SettingsFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.BUNDLE_SETTINGS_PREFS, pref);
        f.setArguments(bundle);

        removePreviousFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }
}
