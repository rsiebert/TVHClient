package org.tvheadend.tvhclient.features.dvr;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.features.dvr.recordings.RecordingAddEditFragment;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingAddEditFragment;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingAddEditFragment;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class RecordingAddEditActivity extends AppCompatActivity implements ToolbarInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.misc_content_activity);
        MiscUtils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            Fragment fragment = null;
            String type = getIntent().getStringExtra("type");
            switch (type) {
                case "recording":
                    fragment = new RecordingAddEditFragment();
                    break;
                case "series_recording":
                    fragment = new SeriesRecordingAddEditFragment();
                    break;
                case "timer_recording":
                    fragment = new TimerRecordingAddEditFragment();
                    break;
            }

            if (fragment != null) {
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment instanceof BackPressedInterface) {
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
