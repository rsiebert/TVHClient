package org.tvheadend.tvhclient.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.fragments.settings.SettingsManageConnectionFragment;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class SettingsManageConnectionActivity extends AppCompatActivity implements ToolbarInterfaceLight {
    private boolean initialSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        MiscUtils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initialSetup = getIntent().getBooleanExtra("initial_setup", false);

        if (savedInstanceState == null) {
            SettingsManageConnectionFragment fragment = new SettingsManageConnectionFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (!initialSetup) {
            Fragment fragment = getFragmentManager().findFragmentById(R.id.main);
            if (fragment != null && fragment instanceof BackPressedInterface) {
                ((BackPressedInterface) fragment).onBackPressed();
            }
        } else {
            Intent intent = new Intent(this, NavigationDrawerActivity.class);
            startActivity(intent);
            finish();
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
