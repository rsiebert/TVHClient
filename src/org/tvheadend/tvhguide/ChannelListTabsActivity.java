package org.tvheadend.tvhguide;

import java.lang.reflect.Field;

import org.tvheadend.tvhguide.htsp.HTSService;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.Toast;

public class ChannelListTabsActivity extends Activity {

    private ChannelListFragment clf;
    private int prevTabPosition = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Apply the specified theme
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean theme = prefs.getBoolean("lightThemePref", false);
        setTheme(theme ? android.R.style.Theme_Holo_Light : android.R.style.Theme_Holo);
        
        super.onCreate(savedInstanceState);
        
        // setup action bar for tabs
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActionBar().setDisplayHomeAsUpEnabled(false);

        // Make the action bar collapse even when the hardware keys are present.
        // This overrides the default behavior of the action bar.
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        }
        catch (Exception ex) {
            // Ignore
        }

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                handleTabSelection(tab, ft);
            }

            @Override
            public void onTabReselected(Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                // Save the position of the tab that has been unselected.
                prevTabPosition = tab.getPosition();
                
                // Detach the channel list fragment, because another will be attached
                if (tab.getPosition() == 0 && clf != null) {
                    ft.detach(clf);
                }
            }
        };

        // Add the tabs
        Tab tab = getActionBar().newTab().setText(R.string.channels).setTabListener(tabListener);
        getActionBar().addTab(tab);
        tab = getActionBar().newTab().setText(R.string.recordings).setTabListener(tabListener);
        getActionBar().addTab(tab);
        tab = getActionBar().newTab().setText(R.string.program_guide).setTabListener(tabListener);
        getActionBar().addTab(tab);
        tab = getActionBar().newTab().setText(R.string.status).setTabListener(tabListener);
        getActionBar().addTab(tab);
        
        // Restore the previously selected tab
        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt("selected_channel_tab_index", 0);
            getActionBar().setSelectedNavigationItem(index);
        }
    }

    /**
     * 
     * @param tab
     * @param ft
     */
    protected void handleTabSelection(Tab tab, FragmentTransaction ft) {
        switch (tab.getPosition()) {
        case 0:
            // Checks if the fragment is already initialized.
            // If not, it will be instantiated and added to the
            // activity. If it exists, it will simply attached to show it.
            if (clf == null) {
                clf = (ChannelListFragment) Fragment.instantiate(this, ChannelListFragment.class.getName());
                ft.replace(android.R.id.content, clf);
            }
            else {
                ft.attach(clf);
            }
            break;
        case 1:
            // Show the list of recordings
            Intent intent = new Intent(this, RecordingListTabsActivity.class);
            startActivity(intent);
            break;
        case 2:
            // Show the program guide
            Toast.makeText(this, "No implmeneted yet", Toast.LENGTH_SHORT).show();
            break;
        case 3:
            // Show the activity with the status information
            Toast.makeText(this, "No implmeneted yet", Toast.LENGTH_SHORT).show();
            break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the user has pressed the back button, the currently selected tab
        // would be active (like the recordings tab) which would show nothing
        // here. So we set the previously selected tab.
        if (prevTabPosition >= 0 &&
                getActionBar().getSelectedNavigationIndex() == 1) {
            getActionBar().setSelectedNavigationItem(prevTabPosition);
            prevTabPosition = -1;
        }
        
        connect(false);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        int index = getActionBar().getSelectedNavigationIndex();
        outState.putInt("selected_channel_tab_index", index);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_settings: {
                // Save the current tab position so we show the previous tab
                // again when we return from the settings menu.
                prevTabPosition = getActionBar().getSelectedNavigationIndex();
                // Now start the settings activity 
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, R.id.mi_settings);
                return true;
            }
            case R.id.mi_refresh:
                connect(true);
                return true;
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }
    
    public void connect(boolean force) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String hostname = prefs.getString("serverHostPref", "localhost");
        int port = Integer.parseInt(prefs.getString("serverPortPref", "9982"));
        String username = prefs.getString("usernamePref", "");
        String password = prefs.getString("passwordPref", "");

        Intent intent = new Intent(this, HTSService.class);
        intent.setAction(HTSService.ACTION_CONNECT);
        intent.putExtra("hostname", hostname);
        intent.putExtra("port", port);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("force", force);

        startService(intent);
    }
}
