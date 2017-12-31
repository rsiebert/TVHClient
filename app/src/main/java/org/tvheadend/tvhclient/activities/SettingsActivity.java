package org.tvheadend.tvhclient.activities;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.callbacks.FolderChooserDialogCallback;
import org.tvheadend.tvhclient.fragments.ChangeLogFragment;
import org.tvheadend.tvhclient.fragments.InfoFragment;
import org.tvheadend.tvhclient.fragments.UnlockerFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsAdvancedFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsCastingFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsListConnectionsFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsNotificationFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsProfilesFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsTranscodingFragment;
import org.tvheadend.tvhclient.fragments.settings.SettingsUserInterfaceFragment;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.File;

public class SettingsActivity extends AppCompatActivity implements ToolbarInterface, FolderChooserDialog.FolderCallback {

    private String settingType;

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

        if (savedInstanceState == null) {
            settingType = getIntent().getStringExtra("setting_type");
            if (settingType == null) {
                // Show the default fragment Create the default settings
                SettingsFragment fragment = new SettingsFragment();
                fragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
            } else {
                Fragment fragment = null;
                switch (settingType) {
                    case "list_connections":
                        fragment = new SettingsListConnectionsFragment();
                        break;
                    case "user_interface":
                        fragment = new SettingsUserInterfaceFragment();
                        break;
                    case "notifications":
                        fragment = new SettingsNotificationFragment();
                        break;
                    case "profiles":
                        fragment = new SettingsProfilesFragment();
                        break;
                    case "casting":
                        fragment = new SettingsCastingFragment();
                        break;
                    case "transcoding":
                        fragment = new SettingsTranscodingFragment();
                        break;
                    case "advanced":
                        fragment = new SettingsAdvancedFragment();
                        break;
                    case "information":
                        fragment = new InfoFragment();
                        break;
                    case "unlocker":
                        fragment = new UnlockerFragment();
                        break;
                    case "changelog":
                        fragment = new ChangeLogFragment();
                        break;
                }
                if (fragment != null) {
                    fragment.setArguments(getIntent().getExtras());
                    getFragmentManager().beginTransaction().add(R.id.main, fragment).commit();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("setting_type", settingType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        // If a settings fragment is currently visible, let the fragment
        // handle the back press, otherwise the setting activity.
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment instanceof BackPressedInterface) {
            ((BackPressedInterface) fragment).onBackPressed();
        }
        super.onBackPressed();
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
