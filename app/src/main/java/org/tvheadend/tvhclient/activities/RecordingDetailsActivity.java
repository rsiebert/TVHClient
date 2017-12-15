package org.tvheadend.tvhclient.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.fragments.recordings.RecordingDetailsFragment;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

public class RecordingDetailsActivity extends AppCompatActivity implements ToolbarInterfaceLight {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_details);
        Utils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            RecordingDetailsFragment fragment = new RecordingDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, fragment).commit();
        }
    }

    @Override
    public void onBackPressed() {
        // If a settings fragment is currently visible, let the fragment
        // handle the back press, otherwise the setting activity.
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (fragment != null && fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
        }
        super.onBackPressed();
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
