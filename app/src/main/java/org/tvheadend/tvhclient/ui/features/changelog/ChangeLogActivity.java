package org.tvheadend.tvhclient.ui.features.changelog;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class ChangeLogActivity extends AppCompatActivity implements ToolbarInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelog_activity);
        MiscUtils.setLanguage(this);

        // Get the toolbar so that the fragments can set the title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            ChangeLogFragment fragment = new ChangeLogFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
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
        if (fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
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
