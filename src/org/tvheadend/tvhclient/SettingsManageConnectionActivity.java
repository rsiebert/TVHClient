package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.fragments.SettingsManageConnectionFragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class SettingsManageConnectionActivity extends ActionBarActivity {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsManageConnectionActivity.class.getSimpleName();
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);
        setContentView(R.layout.settings_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Get the fragment that is showing the connection details
                SettingsManageConnectionFragment fragment = (SettingsManageConnectionFragment) getFragmentManager()
                        .findFragmentById(R.id.settings_fragment);

                // Call the methods directly from the fragment because the
                // toolbar listener is here and not in the fragment.
                switch (item.getItemId()) {
                case R.id.menu_save:
                    if (fragment != null) {
                        fragment.save();
                    }
                    return true;
                case R.id.menu_cancel:
                    if (fragment != null) {
                        fragment.cancel();
                    }
                    return true;
                default:
                    return false;
                }
            }
        });
        toolbar.inflateMenu(R.menu.preference_edit_connection);
        toolbar.setTitle(R.string.menu_settings);

        // TODO add home button

        // Display the fragment with the connection details
        Fragment f = new SettingsManageConnectionFragment();
        f.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .commit();
    }
}
