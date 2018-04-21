package org.tvheadend.tvhclient.features.startup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.features.changelog.ChangeLogActivity;
import org.tvheadend.tvhclient.utils.MiscUtils;

import timber.log.Timber;

public class StartupActivity extends AppCompatActivity implements ToolbarInterface {
    private boolean showStatusFragment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        MiscUtils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            // Show the full changelog if the changelog was never shown before (app version
            // name is empty) or if it was already shown and the version name is the same as
            // the one in the preferences. Otherwise show the changelog of the newest app version.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String versionName = sharedPreferences.getString("versionNameForChangelog", "");
            boolean showChangeLogRequired = !BuildConfig.VERSION_NAME.equals(versionName);

            if (showChangeLogRequired) {
                Timber.d("Showing changelog, version name from prefs: " + versionName + ", build version from gradle: " + BuildConfig.VERSION_NAME);
                Intent intent = new Intent(this, ChangeLogActivity.class);
                intent.putExtra("showFullChangelog", versionName.isEmpty());
                intent.putExtra("versionNameForChangelog", versionName);
                startActivityForResult(intent, 0);
            } else {
                showStatusFragment = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            showStatusFragment = true;
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (showStatusFragment) {
            StartupFragment fragment = new StartupFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().replace(R.id.main, fragment).commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
        } else {
            super.onBackPressed();
        }
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
