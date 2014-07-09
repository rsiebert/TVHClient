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
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.ListPositionInterface;
import org.tvheadend.tvhclient.interfaces.ProgramLoadingInterface;
import org.tvheadend.tvhclient.model.Channel;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;

public class ChannelListTabsActivity extends ActionBarActivity implements ChangeLogDialogInterface, ActionBarInterface, ProgramLoadingInterface, ListPositionInterface {

    @SuppressWarnings("unused")
    private final static String TAG = ChannelListTabsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private int prevTabPosition = -1;
    private ChangeLogDialog changeLogDialog;
    private boolean isDualPane = false;
    private int selectedChannelListPosition = 0;
    private int selectedProgramListPosition = 0;

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
        View v = findViewById(R.id.program_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

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
        // Remove any previously active fragment on the right side. In case the
        // connection can't be established the screen is blank.
        if (isDualPane) {
            Fragment plf = getSupportFragmentManager().findFragmentByTag(RIGHT_FRAGMENT_TAG);
            if (plf != null) {
                ft.remove(plf);
            }
        }

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
            startActivityForResult(recIntent, Constants.RESULT_CODE_RECORDINGS);
            break;
        case 2:
            // Show the program guide screen
            Intent epgIntent = new Intent(this, ProgramGuideTabsActivity.class);
            startActivityForResult(epgIntent, Constants.RESULT_CODE_PROGRAM_GUIDE);
            break;
        case 3:
            // Show the status fragment
            Fragment sf = Fragment.instantiate(this, StatusFragment.class.getName());
            ft.replace(R.id.main_fragment, sf, MAIN_FRAGMENT_TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
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
    public void onDestroy() {
        // Remove all listeners and stop the service when the application is closed
        TVHClientApplication app = (TVHClientApplication) getApplication();
        app.removeListeners();
        Intent intent = new Intent(this, HTSService.class);
        stopService(intent);

        super.onDestroy();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Called when an activity was closed and this one is active again. Restarts
     * the activity if required or reconnects to the server to update all data.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_CONNECTIONS) {
            if (resultCode == RESULT_OK) {
                // Reload all data from the server
                if (data.getBooleanExtra("reconnect", false)) {
                    Utils.connect(this, true);
                }
            }
        } else if (requestCode == Constants.RESULT_CODE_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // Reload all data from the server
                if (data.getBooleanExtra("reconnect", false)) {
                    Utils.connect(this, true);
                }
                // Restart the activity
                if (data.getBooleanExtra("restart", false)) {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        } else if (requestCode == Constants.RESULT_CODE_RECORDINGS || 
                requestCode == Constants.RESULT_CODE_PROGRAM_GUIDE) {
            if (resultCode == RESULT_OK) {
                // Restart the activity
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
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void setActionBarSubtitle(final String subtitle, final String tag) {
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public void setActionBarIcon(Channel channel, String tag) {
        if (actionBar != null && channel != null) {
            // Show or hide the channel icon if required
            boolean showIcon = Utils.showChannelIcons(this);
            actionBar.setDisplayUseLogoEnabled(showIcon);
            if (showIcon) {
                actionBar.setIcon(new BitmapDrawable(getResources(), channel.iconBitmap));
            }
        }
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
	public void onChannelSelected(int position, Channel channel) {
	    selectedChannelListPosition = position;

	    if (channel == null) {
	        return;
	    }

		if (!isDualPane) {
		    if (!channel.epg.isEmpty()) {
    		    // Show the program list when there is data available
    			Intent intent = new Intent(this, ProgramListActivity.class);
    			intent.putExtra("channelId", channel.id);
    			startActivity(intent);
		    }
		} else {
			// Recreate the fragment with the new channel id
		    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		    Fragment fragment = Fragment.instantiate(this, ProgramListFragment.class.getName());
			Bundle args = new Bundle();
            args.putLong("channelId", channel.id);
            args.putBoolean("dual_pane", isDualPane);
			fragment.setArguments(args);

			// Replace the previous fragment with the new one
			ft.replace(R.id.program_fragment, fragment, RIGHT_FRAGMENT_TAG);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}
	}

    /**
     * Provided by the channel list listener interface. This method called when
     * the channel list has been fully populated. In a two pane layout a channel
     * list item must be preselected so that the program list on the right side
     * shows its list with programs from this very selected channel.
     */
    public void onChannelListPopulated() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
//        if (f != null && isDualPane) {
//            ((ChannelListFragment) f).setSelectedItem(selectedChannelListPosition);
//        }
    }

    @Override
    public void loadMorePrograms(Channel channel) {
        Utils.loadMorePrograms(this, channel);
    }

    @Override
    public int getPreviousListPosition() {
        return selectedProgramListPosition;
    }

    @Override
    public void saveCurrentListPosition(int position) {
        selectedProgramListPosition = position;
    }
}
