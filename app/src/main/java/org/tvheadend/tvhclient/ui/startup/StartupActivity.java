package org.tvheadend.tvhclient.ui.startup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.information.ChangeLogFragment;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.List;

public class StartupActivity extends AppCompatActivity implements ToolbarInterface {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);
        MiscUtils.setLanguage(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the toolbar so that the fragments can set the title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            if (isShowChangelogRequired()) {
                // Show certain fragment depending on the current status.
                ChangeLogFragment fragment = new ChangeLogFragment();
                fragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();

            } else if (!isConnectionDefined()
                    || !isActiveConnectionDefined()
                    || !isNetworkAvailable()) {
                // Show the fragment with the connection info are defined
                Bundle bundle = new Bundle();
                if (!isConnectionDefined()) {
                    bundle.putString("type", "no_connections");
                } else if (!isActiveConnectionDefined()) {
                    bundle.putString("type", "no_active_connection");
                } else if (!isNetworkAvailable()) {
                    bundle.putString("type", "no_network");
                }
                ConnectionStatusFragment fragment = new ConnectionStatusFragment();
                fragment.setArguments(bundle);
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();

            } else {
                // connect to the server and show the sync status if required
                SyncStatusFragment fragment = new SyncStatusFragment();
                fragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private boolean isConnectionDefined() {
        List<Connection> connectionList = DatabaseHelper.getInstance(getApplicationContext()).getConnections();
        return connectionList != null && connectionList.size() > 0;
    }

    private boolean isActiveConnectionDefined() {
        return DatabaseHelper.getInstance(getApplicationContext()).getSelectedConnection() != null;
    }

    private boolean isShowChangelogRequired() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String appVersionName = sharedPreferences.getString("app_version_name_for_changelog", "");
        return (!BuildConfig.VERSION_NAME.equals(appVersionName));
    }

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }
}
