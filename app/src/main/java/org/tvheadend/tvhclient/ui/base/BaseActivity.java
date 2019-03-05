package org.tvheadend.tvhclient.ui.base;

import android.content.Context;
import android.view.MenuItem;

import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.util.LocaleUtils;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity implements ToolbarInterface {

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleUtils.onAttach(context));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
