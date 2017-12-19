package org.tvheadend.tvhclient.activities;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.fragments.recordings.RecordingAddEditFragment;
import org.tvheadend.tvhclient.fragments.recordings.SeriesRecordingAddFragment;
import org.tvheadend.tvhclient.fragments.recordings.TimerRecordingAddFragment;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class AddEditActivity extends AppCompatActivity implements ToolbarInterface {

    private TextView actionBarTitle;
    private TextView actionBarSubtitle;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        MiscUtils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Get the widgets so we can use them later and do not need to inflate again
        actionBarTitle = toolbar.findViewById(R.id.actionbar_title);
        actionBarSubtitle = toolbar.findViewById(R.id.actionbar_subtitle);

        if (savedInstanceState == null) {
            Fragment fragment = null;
            String type = getIntent().getStringExtra("type");
            switch (type) {
                case "recording":
                    fragment = new RecordingAddEditFragment();
                    break;
                case "series_recording":
                    fragment = new SeriesRecordingAddFragment();
                    break;
                case "timer_recording":
                    fragment = new TimerRecordingAddFragment();
                    break;
            }

            if (fragment != null) {
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction().add(R.id.content_frame, fragment).commit();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // If a settings fragment is currently visible, let the fragment
        // handle the back press, otherwise the setting activity.
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (fragment != null && fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void setActionBarTitle(final String title) {
        if (actionBar != null && actionBarTitle != null) {
            actionBarTitle.setText(title);
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void setActionBarSubtitle(final String subtitle) {
        if (actionBar != null && actionBarSubtitle != null) {
            actionBarSubtitle.setText(subtitle);
            // If no subtitle string is given hide it from the view and center
            // the title vertically, otherwise place it below the title
            if (subtitle.length() == 0) {
                actionBarSubtitle.setVisibility(View.GONE);
                actionBarTitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else {
                actionBarSubtitle.setVisibility(View.VISIBLE);
                actionBarTitle.setGravity(Gravity.LEFT | Gravity.BOTTOM);
            }
        }
    }

    public void setActionBarIcon(final Bitmap bitmap) {
        // NOP
    }

    @Override
    public void setActionBarIcon(final int resource) {
        // NOP
    }
}
