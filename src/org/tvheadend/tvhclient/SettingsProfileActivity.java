package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.fragments.SettingsProfilesFragment;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class SettingsProfileActivity extends ActionBarActivity implements ActionBarInterface {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsProfileActivity.class.getSimpleName();

    private ActionBar actionBar = null;

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
    }

    @Override
    public void onResume() {
        super.onResume();
        // Removes the previous fragment from the view and back stack so that
        // navigating back would not show the old fragment again.
        Fragment fragment = (Fragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsProfilesFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        } else {
            // Show the available fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = (Fragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
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
