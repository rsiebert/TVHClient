/*
 *  Copyright (C) 2013 Robert Siebert
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide;


import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class ChannelListTabsActivity extends Activity {

    private int prevTabPosition = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Apply the specified theme
        setTheme(Utils.getThemeId(this));

        super.onCreate(savedInstanceState);
        
        DatabaseHelper.init(this.getApplicationContext()); 
        
        // setup action bar for tabs
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setDisplayUseLogoEnabled(Utils.showChannelIcons(this));

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
                Fragment prevFragment = getFragmentManager().findFragmentByTag(tab.getText().toString());
                if (prevFragment != null) {
                    ft.detach(prevFragment);
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
            Fragment currentFrag = getFragmentManager().findFragmentByTag(tab.getText().toString());
            if (currentFrag == null) {
                Fragment fragment = Fragment.instantiate(this, ChannelListFragment.class.getName());
                ft.add(android.R.id.content, fragment, tab.getText().toString());
            }
            else {
                ft.attach(currentFrag);
            }
            break;
        case 1:
            // Show the list of recordings
            Intent intent = new Intent(this, RecordingListTabsActivity.class);
            startActivity(intent);
            break;
        case 2:
            // Show the program guide
            Toast.makeText(this, "No implemented yet", Toast.LENGTH_SHORT).show();
            break;
        case 3:
        	Fragment statusFrag = getFragmentManager().findFragmentByTag(tab.getText().toString());
            if (statusFrag == null) {
                Fragment fragment = Fragment.instantiate(this, StatusFragment.class.getName());
                ft.add(android.R.id.content, fragment, tab.getText().toString());
            }
            else {
                ft.attach(statusFrag);
            }
            break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
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
        
        Utils.connect(this, false);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        int index = getActionBar().getSelectedNavigationIndex();
        outState.putInt("selected_channel_tab_index", index);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Disable the refresh menu if no connection is available
        if (DatabaseHelper.getInstance().getSelectedConnection() == null) {
            MenuItem item = menu.findItem(R.id.menu_refresh);
            item.setVisible(false);
        }
        return true;
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
            case R.id.menu_settings: {
                // Save the current tab position so we show the previous tab
                // again when we return from the settings menu.
                prevTabPosition = getActionBar().getSelectedNavigationIndex();
                // Now start the settings activity 
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, R.id.menu_settings);
                return true;
            }
            case R.id.menu_refresh:
                Utils.connect(this, true);
                return true;
            case R.id.menu_connections:
                // Show a popup with the available 
                // connection that the user can choose
                
                return true;
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }
}
