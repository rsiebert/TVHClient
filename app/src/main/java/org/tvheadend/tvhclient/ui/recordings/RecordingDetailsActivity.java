package org.tvheadend.tvhclient.ui.recordings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.programs.ProgramDetailsFragment;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class RecordingDetailsActivity extends AppCompatActivity implements ToolbarInterface {

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
                case "program":
                    fragment = new ProgramDetailsFragment();
                    break;
                case "recording":
                    fragment = new RecordingDetailsFragment();
                    break;
                case "series_recording":
                    fragment = new SeriesRecordingDetailsFragment();
                    break;
                case "timer_recording":
                    fragment = new TimerRecordingDetailsFragment();
                    break;
            }

            if (fragment != null) {
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction().add(R.id.content_frame, fragment).commit();
            }
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
