package org.tvheadend.tvhclient;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.fragments.InfoFragment;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class InfoActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private final static String TAG = InfoActivity.class.getSimpleName();

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
            actionBar.setTitle(getString(R.string.pref_information));
        }

        if (savedInstanceState == null) {
            InfoFragment fragment = new InfoFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.main_fragment, fragment).commit();
        }
    }
}
