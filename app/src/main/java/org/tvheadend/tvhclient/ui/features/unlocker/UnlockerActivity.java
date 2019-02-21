package org.tvheadend.tvhclient.ui.features.unlocker;

import android.os.Bundle;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.BaseActivity;
import org.tvheadend.tvhclient.util.MiscUtils;

import androidx.appcompat.widget.Toolbar;

public class UnlockerActivity extends BaseActivity {

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
            UnlockerFragment fragment = new UnlockerFragment();
            Bundle bundle = new Bundle();
            bundle.putString("website", "features");
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
        }
    }
}
