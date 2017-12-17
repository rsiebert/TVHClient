package org.tvheadend.tvhclient.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.fragments.UnlockerFragment;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class UnlockerActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        MiscUtils.setLanguage(this);

        // Setup the action bar and show the title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        TextView actionBarTitle = toolbar.findViewById(R.id.actionbar_title);
        TextView actionBarSubtitle = toolbar.findViewById(R.id.actionbar_subtitle);
        actionBarTitle.setText(getString(R.string.pref_unlocker));
        actionBarTitle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        actionBarSubtitle.setVisibility(View.GONE);

        if (savedInstanceState == null) {
            UnlockerFragment fragment = new UnlockerFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.main_fragment, fragment).commit();
        }
    }
}
