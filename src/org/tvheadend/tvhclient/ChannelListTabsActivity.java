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

import org.tvheadend.tvhclient.ChangeLogDialog.ChangeLogDialogInterface;
import org.tvheadend.tvhclient.ChannelListFragment.OnChannelListListener;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class ChannelListTabsActivity extends ActionBarActivity implements ChangeLogDialogInterface, OnChannelListListener, ActionBarInterface {

    @SuppressWarnings("unused")
    private final static String TAG = ChannelListTabsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private int prevTabPosition = -1;
    private ChangeLogDialog changeLogDialog;
    private boolean isDualPane = false;
    private int selectedChannelListPosition = 0;

    private static final String MAIN_FRAGMENT_TAG = "channel_list_fragment";
    private static final String RIGHT_FRAGMENT_TAG = "program_list_fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_layout);
        Utils.setLanguage(this);

		// Check if the layout supports showing the program list next to the
		// channel list. This is usually available on tablets 
        
        /*
         * TODO Deactivate the dual pane for now. If the program list fragment
         * is shown, it updates the action bar title instead of the channel list
         * fragment. Also the handling logic and how to show the recordings or
         * the program details is not clear.
         */
//        View v = findViewById(R.id.program_fragment);
//        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

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
        
        if (savedInstanceState != null) {
            // Restore the previously selected tab
            int index = savedInstanceState.getInt("selected_channel_tab_index", 0);
            actionBar.setSelectedNavigationItem(index);
            // Get the previously selected channel item position
            selectedChannelListPosition = savedInstanceState.getInt("selected_channel_list_position", 0);
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
            Fragment clf = Fragment.instantiate(this, ChannelListFragment.class.getName());
            ft.replace(R.id.main_fragment, clf, MAIN_FRAGMENT_TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
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
            // Show the status fragment
            Fragment sf = Fragment.instantiate(this, StatusFragment.class.getName());
            ft.replace(R.id.main_fragment, sf, MAIN_FRAGMENT_TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            
			// Remove the program list fragment so it is not visible when the
			// status screen is shown
			if (isDualPane) {
				Fragment plf = getSupportFragmentManager().findFragmentByTag(RIGHT_FRAGMENT_TAG);
				if (plf != null) {
					ft.remove(plf);
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

        Utils.connect(this, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // When the orientation changes from landscape to portrait the program
        // list fragment would crash because the container is null. So we remove
        // it entirely before the orientation change happens.
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment pListFrag = getSupportFragmentManager().findFragmentByTag(RIGHT_FRAGMENT_TAG);
        if (pListFrag != null) {
            ft.remove(pListFrag);
            ft.commit();
        }

        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        int index = actionBar.getSelectedNavigationIndex();
        outState.putInt("selected_channel_tab_index", index);
        // Save the position of the selected channel list item
        outState.putInt("selected_channel_list_position", selectedChannelListPosition);
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
            if (resultCode == RESULT_OK) {
                Utils.connect(this, data.getBooleanExtra("reconnect", false));
            }
        } else if (requestCode == Utils.getResultCode(R.id.menu_settings)) {
            if (resultCode == RESULT_OK) {
                if (data.getBooleanExtra("restart", false)) {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public void setActionBarTitle(final String title, final String tag) {
        actionBar.setTitle(title);
    }

    @Override
    public void setActionBarSubtitle(final String subtitle, final String tag) {
        actionBar.setSubtitle(subtitle);
    }

    @Override
    public void dialogDismissed() {
        createTabListeners(null);
    }

    /**
     * Provided by the channel list listener interface. This method is called
     * when the user has selected an item from the channel list. In dual pane
     * mode the program list on the right side will be recreated to show the new
     * programs for the selected channel. In normal mode the activity will be
     * called that shows the program list.
     */
	@Override
	public void onChannelSelected(int position, long channelId) {
	    selectedChannelListPosition = position;
	    
	    // TODO Dual pane is disabled for now
//		if (!isDualPane) {
		    // Start the activity
			Intent intent = new Intent(this, ProgramListActivity.class);
			intent.putExtra("channelId", channelId);
			startActivity(intent);
//		} else {
//			// Recreate the fragment with the new channel id
//		    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//		    Fragment fragment = Fragment.instantiate(this, ProgramListFragment.class.getName());
//			Bundle args = new Bundle();
//            args.putLong("channelId", channelId);
//			fragment.setArguments(args);
//
//			// Replace the previous fragment with the new one
//			ft.replace(R.id.program_fragment, fragment, RIGHT_FRAGMENT_TAG);
//			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//			ft.commit();
//		}
	}

    /**
     * Provided by the channel list listener interface. This method called when
     * the channel list has been fully populated. In a two pane layout a channel
     * list item must be preselected so that the program list on the right side
     * shows its list with programs from this very selected channel.
     */
    @Override
    public void onChannelListPopulated() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
        if (f != null && isDualPane) {
            ((ChannelListFragment) f).setSelectedItem(selectedChannelListPosition);
        }
    }
}
