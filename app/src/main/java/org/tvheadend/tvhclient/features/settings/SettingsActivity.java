package org.tvheadend.tvhclient.features.settings;

import android.app.Fragment;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.changelog.ChangeLogActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.features.shared.receivers.SnackbarMessageReceiver;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.File;

import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity implements ToolbarInterface, FolderChooserDialog.FolderCallback {

    private SnackbarMessageReceiver snackbarMessageReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        MiscUtils.setLanguage(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        snackbarMessageReceiver = new SnackbarMessageReceiver(this);

        String settingType = "default";
        if (getIntent().hasExtra("setting_type")) {
            settingType = getIntent().getStringExtra("setting_type");
        }

        Fragment fragment;
        if (savedInstanceState == null) {
            Timber.d("Saved instance is null, creating settings fragment");
            fragment = getSettingsFragment(settingType);
            fragment.setArguments(getIntent().getExtras());
        } else {
            Timber.d("Saved instance is not null, trying to find settings fragment");
            fragment = getFragmentManager().findFragmentById(R.id.main);
        }

        Timber.d("Replacing fragment");
        getFragmentManager().beginTransaction().replace(R.id.main, fragment).commit();
    }

    private Fragment getSettingsFragment(@NonNull String type) {
        Timber.d("Getting settings fragment for type '" + type + "'");
        Fragment fragment = null;
        Intent intent;
        switch (type) {
            case "list_connections":
                fragment = new SettingsListConnectionsFragment();
                break;
            case "add_connection":
                fragment = new SettingsAddConnectionFragment();
                break;
            case "edit_connection":
                fragment = new SettingsEditConnectionFragment();
                break;
            case "user_interface":
                fragment = new SettingsUserInterfaceFragment();
                break;
            case "profiles":
                fragment = new SettingsProfilesFragment();
                break;
            case "playback":
                fragment = new SettingsPlaybackFragment();
                break;
            case "advanced":
                fragment = new SettingsAdvancedFragment();
                break;
            case "changelog":
                intent = new Intent(this, ChangeLogActivity.class);
                startActivity(intent);
                break;
            default:
                fragment = new SettingsFragment();
                fragment.setArguments(getIntent().getExtras());
                break;
        }
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(snackbarMessageReceiver, new IntentFilter(SnackbarMessageReceiver.ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(snackbarMessageReceiver);
    }

    @Override
    public void onBackPressed() {
        // If a settings fragment is currently visible, let the fragment
        // handle the back press, otherwise the setting activity.
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main);
        if (fragment instanceof BackPressedInterface
                && fragment.isVisible()) {
            Timber.d("Calling back press in the fragment");
            ((BackPressedInterface) fragment).onBackPressed();
        } else {
            Timber.d("Calling back press of super");
            super.onBackPressed();
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

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        Fragment f = getFragmentManager().findFragmentById(R.id.main);
        if (f != null && f.isAdded() && f instanceof FolderChooserDialogCallback) {
            ((FolderChooserDialogCallback) f).onFolderSelected(folder);
        }
    }

    @Override
    public void onFolderChooserDismissed(@NonNull FolderChooserDialog dialog) {
        // NOP
    }
}
