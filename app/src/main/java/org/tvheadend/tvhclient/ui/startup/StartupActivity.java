package org.tvheadend.tvhclient.ui.startup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.misc.ChangeLogActivity;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class StartupActivity extends AppCompatActivity implements ToolbarInterface {
    @SuppressWarnings("unused")
    private String TAG = getClass().getSimpleName();
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
            if (isShowChangelogRequired()) {
                Intent intent = new Intent(this, ChangeLogActivity.class);
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
