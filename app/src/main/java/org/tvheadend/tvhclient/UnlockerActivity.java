package org.tvheadend.tvhclient;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.fragments.UnlockerFragment;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class UnlockerActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Utils.setLanguage(this);

        // Setup the action bar and show the title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            setTitle(getString(R.string.pref_unlocker));
        }

        if (savedInstanceState == null) {
            UnlockerFragment fragment = new UnlockerFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.main_fragment, fragment).commit();
        }
    }
}
