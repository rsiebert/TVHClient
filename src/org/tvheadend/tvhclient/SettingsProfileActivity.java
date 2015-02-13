package org.tvheadend.tvhclient;

import org.tvheadend.tvhclient.fragments.SettingsProfileFragment;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

public class SettingsProfileActivity extends ActionBarActivity implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsProfileActivity.class.getSimpleName();
    private Toolbar toolbar;
    private Connection conn;
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);
        setContentView(R.layout.settings_layout);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
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
        toolbar.setTitle(R.string.pref_profiles);

        // Get the selected connection and set the toolbar texts
        long id = getIntent().getLongExtra(Constants.BUNDLE_CONNECTION_ID, 0);
        conn = DatabaseHelper.getInstance().getConnection(id);
        // Display the fragment with the connection details otherwise
        // exit this activity because no valid connection was given.
        if (conn != null) {
            toolbar.setSubtitle(conn.name);
            SettingsProfileFragment f = (SettingsProfileFragment) getFragmentManager()
                    .findFragmentById(R.id.settings_fragment);
            if (f == null) {
                f = new SettingsProfileFragment();
                f.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.settings_fragment, f).commit();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, getIntent());
        finish();
    }

    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.addListener(this); 
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListener(this);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_LOADING)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    if (!loading) {
                        toolbar.setSubtitle(conn != null ? conn.name : getString(R.string.no_connection_available));
                    } else {
                        toolbar.setSubtitle(R.string.loading_profiles);
                    }
                }
            });
        }
    }
}
