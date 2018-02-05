package org.tvheadend.tvhclient.ui.startup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionDataRepository;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.misc.ChangeLogFragment;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.List;

public class StartupActivity extends AppCompatActivity implements ToolbarInterface {
    private String TAG = getClass().getSimpleName();
    private ConnectionDataRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        MiscUtils.setLanguage(this);

        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");

        // Get the toolbar so that the fragments can set the title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        repository = new ConnectionDataRepository(this);

        if (savedInstanceState == null) {
            if (isShowChangelogRequired()) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
                // Show certain fragment depending on the current status.
                ChangeLogFragment fragment = new ChangeLogFragment();
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction().add(R.id.main, fragment).commit();

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
                Log.d(TAG, "onCreate: showing connection fragment");
                ConnectionStatusFragment fragment = new ConnectionStatusFragment();
                fragment.setArguments(bundle);
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();

            } else {
                Log.d(TAG, "onCreate: showing sync fragment");
                // connect to the server and show the sync status if required
                SyncStatusFragment fragment = new SyncStatusFragment();
                fragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
        }
        Intent intent = new Intent(this, StartupActivity.class);
        startActivity(intent);
        super.onBackPressed();
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
        List<Connection> connectionList = repository.getAllConnectionsSync();
        return connectionList != null && connectionList.size() > 0;
    }

    private boolean isActiveConnectionDefined() {
        return repository.getActiveConnectionSync() != null;
    }

    private boolean isShowChangelogRequired() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String versionName = sharedPreferences.getString("version_name_for_changelog", "");
        return (!BuildConfig.VERSION_NAME.equals(versionName));
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
