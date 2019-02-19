package org.tvheadend.tvhclient.ui.features.programs;

import android.os.Bundle;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.BaseActivity;
import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.util.MiscUtils;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class ProgramDetailsActivity extends BaseActivity implements ToolbarInterface {

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
            Fragment fragment = new ProgramDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
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
