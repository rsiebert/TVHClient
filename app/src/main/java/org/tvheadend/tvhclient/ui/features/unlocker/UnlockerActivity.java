package org.tvheadend.tvhclient.ui.features.purchase;

import android.os.Bundle;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class UnlockerActivity extends AppCompatActivity implements ToolbarInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        MiscUtils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            UnlockerFragment fragment = new UnlockerFragment();
            Bundle bundle = new Bundle();
            bundle.putString("website", "features");
            fragment.setArguments(bundle);
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
