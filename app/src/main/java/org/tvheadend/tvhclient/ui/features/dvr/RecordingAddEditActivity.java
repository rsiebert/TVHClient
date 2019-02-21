package org.tvheadend.tvhclient.ui.features.dvr;

import android.os.Bundle;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.BaseActivity;
import org.tvheadend.tvhclient.ui.base.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingAddEditFragment;
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingAddEditFragment;
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingAddEditFragment;
import org.tvheadend.tvhclient.util.MiscUtils;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

// TODO split into 3 activities

public class RecordingAddEditActivity extends BaseActivity {

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
}
