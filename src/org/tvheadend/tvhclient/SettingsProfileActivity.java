package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.fragments.SettingsProfileFragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

public class SettingsProfileActivity extends ActionBarActivity {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsProfileActivity.class.getSimpleName();
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);
        setContentView(R.layout.settings_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon((Utils.getThemeId(this) == R.style.CustomTheme_Light) ? R.drawable.ic_menu_back_light
                : R.drawable.ic_menu_back_dark);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Get the fragment that is showing the connection details
                SettingsProfileFragment fragment = (SettingsProfileFragment) getFragmentManager()
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
        toolbar.inflateMenu(R.menu.save_cancel_menu);
        toolbar.setTitle(R.string.menu_settings);

        // Display the fragment with the connection details
        Fragment f = new SettingsProfileFragment();
        f.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, f)
                .commit();
    }
}
