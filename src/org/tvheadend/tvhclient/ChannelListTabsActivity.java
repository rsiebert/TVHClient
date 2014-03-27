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
package org.tvheadend.tvhclient;

import java.util.Locale;

import org.tvheadend.tvhclient.ChangeLogDialog.ChangeLogDialogInterface;
import org.tvheadend.tvhclient.ChannelListFragment.OnChannelSelectedListener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ChannelListTabsActivity extends ActionBarActivity implements ChangeLogDialogInterface, OnChannelSelectedListener {

    private ActionBar actionBar = null;
    private boolean reconnect = false;
    private int prevTabPosition = -1;
    private ChangeLogDialog changeLogDialog;
    private Configuration config;
    private boolean isDualPane = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_layout);

        View v = findViewById(R.id.program_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;
        Log.i("CTA", "isDualPane " + isDualPane);
        
        // Change the language to the defined setting. If the default is set
        // then let the application decide which language shall be used.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String locale = prefs.getString("languagePref", "default");
        if (!locale.equals("default")) {
            config = new Configuration(getResources().getConfiguration());
            config.locale = new Locale(locale);
            getResources().updateConfiguration(config,getResources().getDisplayMetrics());
        }

        DatabaseHelper.init(this.getApplicationContext()); 
        changeLogDialog = new ChangeLogDialog(this);

        // Setup action bar for tabs
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayUseLogoEnabled(Utils.showChannelIcons(this));

        // Show the change log once when the application was upgraded.
        // In any other case show the selected fragment.
        if (changeLogDialog.firstRun()) {
            changeLogDialog.getLogDialog().show();
        }
        else {
            createTabListeners(savedInstanceState);
        }        
    }

    /**
     * Creates the tab listener that is called when the user has selected or
     * unselected a tab. In the latter tab position will be saved.
     * 
     * @param savedInstanceState
     */
    protected void createTabListeners(Bundle savedInstanceState) {
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
                Fragment prevFragment = getSupportFragmentManager().findFragmentByTag(tab.getText().toString());
                if (prevFragment != null) {
                    ft.detach(prevFragment);
                }
            }
        };

        // Add the tabs to the action bar
        Tab tab = actionBar.newTab().setText(R.string.channels).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.recordings).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.program_guide).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.status).setTabListener(tabListener);
        actionBar.addTab(tab);
        
        // Restore the previously selected tab
        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt("selected_channel_tab_index", 0);
            actionBar.setSelectedNavigationItem(index);
        }
    }

    /**
     * Depending on the selected tab, the channel or recording list, the program
     * guide or status screen is shown.
     * 
     * @param tab
     * @param ft
     */
    protected void handleTabSelection(Tab tab, FragmentTransaction ft) {
        switch (tab.getPosition()) {
        case 0:
            // Show the channel list fragment
            Fragment currentFrag = getSupportFragmentManager().findFragmentByTag(tab.getText().toString());
            if (currentFrag == null) {
                // If the fragment is not already initialized, it will be
                // instantiated and added to the activity. If it exists, it will
                // simply attached to show it.
                Fragment fragment = Fragment.instantiate(this, ChannelListFragment.class.getName());
                ft.add(R.id.main_fragment, fragment, tab.getText().toString());
            }
            else {
                ft.attach(currentFrag);
            }
            break;
        case 1:
            // Show the list of recordings screen
            Intent recIntent = new Intent(this, RecordingListTabsActivity.class);
            startActivity(recIntent);
            break;
        case 2:
            // Show the program guide screen
            Intent epgIntent = new Intent(this, ProgramGuideTabsActivity.class);
            startActivity(epgIntent);
            break;
        case 3:
            // Show the status screen
        	Fragment statusFrag = getSupportFragmentManager().findFragmentByTag(tab.getText().toString());
            if (statusFrag == null) {
                // If the fragment is not already initialized, it will be
                // instantiated and added to the activity. If it exists, it will
                // simply attached to show it.
                Fragment fragment = Fragment.instantiate(this, StatusFragment.class.getName());
                ft.add(R.id.main_fragment, fragment, tab.getText().toString());
            }
            else {
                ft.attach(statusFrag);
            }
            
			// Remove the program list fragment so it is not visible when the
			// status screen is shown
			if (isDualPane) {
				Fragment currentProgramListFrag = getSupportFragmentManager()
						.findFragmentByTag(ProgramListFragment.class.getName());
				if (currentProgramListFrag != null) {
					ft.remove(currentProgramListFrag);
				}
			}
            break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the user has pressed the back button, the currently selected tab
        // would be active (like the recordings or program guide tab) and
        // would show nothing. So we need to set the previously selected tab.
        if (prevTabPosition >= 0
                && (actionBar.getSelectedNavigationIndex() == 1 || actionBar.getSelectedNavigationIndex() == 2)) {
            actionBar.setSelectedNavigationItem(prevTabPosition);
            prevTabPosition = -1;
        }
        
        Utils.connect(this, reconnect);
        reconnect = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        int index = actionBar.getSelectedNavigationIndex();
        outState.putInt("selected_channel_tab_index", index);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Disable the refresh menu if no connection is 
        // available or the loading process is active
        MenuItem item = menu.findItem(R.id.menu_refresh);
        if (item != null) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            item.setVisible(DatabaseHelper.getInstance().getSelectedConnection() != null && !app.isLoading());
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
        Intent intent = null;
        switch (item.getItemId()) {
        case R.id.menu_settings:
            // Save the current tab position so we show the previous tab
            // again when we return from the settings menu.
            prevTabPosition = actionBar.getSelectedNavigationIndex();
            // Now start the settings activity
            intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, Utils.getResultCode(R.id.menu_settings));
            return true;

        case R.id.menu_refresh:
            Utils.connect(this, true);
            return true;

        case R.id.menu_connections:
            // Save the current tab position so we show the previous tab
            // again when we return from the settings menu. Then show
            // the manage connections activity
            prevTabPosition = actionBar.getSelectedNavigationIndex();
            intent = new Intent(this, SettingsManageConnectionsActivity.class);
            startActivityForResult(intent, Utils.getResultCode(R.id.menu_connections));
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Reloads all data if the connection details have changed or a new one was created.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Utils.getResultCode(R.id.menu_connections)) {
            if (resultCode == RESULT_OK){
                reconnect = data.getBooleanExtra("reconnect", false);
            }
        }
    }

    public void setActionBarTitle(final String title) {
        actionBar.setTitle(title);
    }

    public void setActionBarSubtitle(final String subtitle) {
        actionBar.setSubtitle(subtitle);
    }

    @Override
    public void dialogDismissed() {
        createTabListeners(null);
    }

    /**
	 * This method is called when the user has selected an item in the channel
	 * list fragment. The program list activity will be called in portrait mode.
	 * In landscape mode the fragment will be recreated with the given channel 
	 * number and added to the layout.
	 */
	@Override
	public void onChannelSelected(long channelId) {
		if (!isDualPane) {
			Intent intent = new Intent(this, ProgramListActivity.class);
			intent.putExtra("channelId", channelId);
			startActivity(intent);
		} else {
			// Recreate the fragment with the new channel id
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			final String tag = ProgramListFragment.class.getName();

			Fragment currentProgramListFrag = getSupportFragmentManager().findFragmentByTag(tag);
			if (currentProgramListFrag != null) {
				ft.remove(currentProgramListFrag);
			}

			Bundle args = new Bundle();
			args.putLong("channelId", channelId);
			Fragment fragment = Fragment.instantiate(this, tag);
			fragment.setArguments(args);
			ft.add(R.id.program_fragment, fragment, tag);
			ft.commit();
		}
	}
}
