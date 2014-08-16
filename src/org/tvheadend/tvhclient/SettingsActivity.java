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
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

public class SettingsActivity extends ActionBarActivity implements ActionBarInterface, SettingsInterface {

    private final static String TAG = SettingsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private Fragment fragment;

    private static boolean restart = false;
    private static boolean reconnect = false;
    private boolean manageConnections;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);

        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        
        // When the orientation was changed the last visible fragment is
        // available from the manager. If this is the case get it and show it
        // again.
        fragment = (Fragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
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
                getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new SettingsFragment())
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .addToBackStack(null)
                        .commit();
            }
        } else {
            // Show the available fragment
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
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
        Log.d(TAG, "onBackPressed bs count" + getSupportFragmentManager().getBackStackEntryCount());

        if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
            Log.d(TAG, "onBackPressed, SettingsFragment, calling finish");
            restartNow();
        } else {
            Log.d(TAG, "onBackPressed pop stack");
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void restart() {
        Log.d(TAG, "restart");
        restart = true;
    }

    @Override
    public void reconnect() {
        Log.d(TAG, "reconnect");
        reconnect = true;
    }

    @Override
    public void restartNow() {
        Log.d(TAG, "restartNow");
        Intent intent = getIntent();
        intent.putExtra(Constants.BUNDLE_RESTART, restart);
        intent.putExtra(Constants.BUNDLE_RECONNECT, reconnect);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void manageConnections() {
        Log.d(TAG, "manageConnections");
        
        removePreviousFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsShowConnectionsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void addConnection() {
        Log.d(TAG, "addConnection");
        
        removePreviousFragment();
        
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsManageConnectionFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void editConnection(long id) {
        Log.d(TAG, "editConnection " + id);
        
        removePreviousFragment();
        
        Bundle bundle = new Bundle();
        bundle.putLong(Constants.BUNDLE_CONNECTION_ID, id);
        Fragment f = Fragment.instantiate(this, SettingsManageConnectionFragment.class.getName());
        f.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    /**
     * 
     */
    private void removePreviousFragment() {
        Fragment f = (Fragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (f != null) {
            getSupportFragmentManager().beginTransaction().remove(f).commit();
        }
    }

    @Override
    public void setActionBarTitle(final String title, final String tag) {
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void setActionBarSubtitle(final String subtitle, final String tag) {
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public void setActionBarIcon(Bitmap bitmap, String tag) {
        // NOP
    }

    @Override
    public void setActionBarIcon(int resource, String tag) {
        // NOP
    }
}
