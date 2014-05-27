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

import org.tvheadend.tvhclient.RecordingListFragment.OnRecordingListListener;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Recording;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class RecordingListTabsActivity extends ActionBarActivity implements ActionBarInterface, OnRecordingListListener {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingListTabsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private RecordingListPagerAdapter adapter = null;
    private static ViewPager viewPager = null;
    private boolean restart = false;
    private boolean isDualPane = false;
    private int selectedRecordingListPosition = 0;

    private static final String MAIN_FRAGMENT_TAG = "recording_list_fragment";
    private static final String RIGHT_FRAGMENT_TAG = "recording_details_fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording_layout);
        Utils.setLanguage(this);

        // Check if the layout supports showing the recording details next to
        // the recording list. This is usually available on tablets
        View v = findViewById(R.id.details_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

        // setup action bar for tabs
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.menu_recordings);

        viewPager = (ViewPager) findViewById(R.id.pager);
        if (viewPager != null) {
            adapter = new RecordingListPagerAdapter(getSupportFragmentManager());
            viewPager.setAdapter(adapter);
        }

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // When the tab is selected, switch to the
                // corresponding page in the ViewPager.
                if (viewPager != null) {
                    viewPager.setCurrentItem(tab.getPosition());
                } else {
                    handleTabSelection(tab, ft);
                }
            }

            @Override
            public void onTabReselected(Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                if (viewPager == null) {
                    // Detach the channel list fragment, because another will be attached
                    Fragment prevFragment = getSupportFragmentManager().findFragmentByTag(tab.getText().toString());
                    if (prevFragment != null) {
                        ft.detach(prevFragment);
                    }
                }
            }
        };

        // Add the tabs with the different recording states
        Tab tab = actionBar.newTab().setText(R.string.completed).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.upcoming).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.failed).setTabListener(tabListener);
        actionBar.addTab(tab);

        // Select the corresponding tab when the user swipes between pages with
        // a touch gesture.
        if (viewPager != null) {
            adapter.notifyDataSetChanged();
            viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    actionBar.setSelectedNavigationItem(position);
                    // Get the fragment of the current tabs. The default tag given by the 
                    // FragmentPagerAdapater is "android:switcher:" + viewId + ":" + position;
                    RecordingListFragment fragment = (RecordingListFragment) getSupportFragmentManager().findFragmentByTag(
                                    "android:switcher:" + viewPager.getId() + ":" + adapter.getItemId(position));
                    // Update the action bar subtitle
                    if (fragment != null) {
                        updateTitle(position, fragment.getRecordingCount());
                    }
                }
            });
        }

        // Restore the previously selected tab. This is usually required when
        // the user has rotated the screen.
        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt("selected_recording_tab_index", 0);
            actionBar.setSelectedNavigationItem(index);
            // Get the previously selected channel item position
            selectedRecordingListPosition = savedInstanceState.getInt("selected_recording_list_position", 0);
        }
    }

    /**
     * Depending on the selected tab, the recording list of the type is shown.
     * 
     * @param tab
     * @param ft
     */
    protected void handleTabSelection(Tab tab, FragmentTransaction ft) {
        Fragment rlf = Fragment.instantiate(this, RecordingListFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putInt("tabIndex", tab.getPosition());
        rlf.setArguments(bundle);
        ft.replace(R.id.main_fragment, rlf, MAIN_FRAGMENT_TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // When the orientation changes from landscape to portrait the program
        // list fragment would crash because the container is null. So we remove
        // it entirely before the orientation change happens.
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment f = getSupportFragmentManager().findFragmentByTag(RIGHT_FRAGMENT_TAG);
        if (f != null) {
            ft.remove(f);
            ft.commit();
        }

        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        int index = actionBar.getSelectedNavigationIndex();
        outState.putInt("selected_recording_tab_index", index);
        // Save the position of the selected channel list item
        outState.putInt("selected_recording_list_position", selectedRecordingListPosition);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Disable the refresh menu if no connection is 
        // available or the loading process is already active
        MenuItem item = menu.findItem(R.id.menu_refresh);
        if (item != null) {
            TVHClientApplication app = (TVHClientApplication) getApplication();
            if (app != null && DatabaseHelper.getInstance() != null) { 
                item.setVisible(DatabaseHelper.getInstance().getSelectedConnection() != null && !app.isLoading());
            }
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
        case android.R.id.home:
            onBackPressed();
            return true;

        case R.id.menu_settings:
            // Start the settings activity 
            intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, Constants.RESULT_CODE_SETTINGS);
            return true;

        case R.id.menu_refresh:
            Utils.connect(this, true);
            return true;

        case R.id.menu_connections:
            // Show the manage connections activity where 
            // the user can choose a connection
            intent = new Intent(this, SettingsManageConnectionsActivity.class);
            startActivityForResult(intent, Constants.RESULT_CODE_CONNECTIONS);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("restart", restart);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Updates the action bar title with the type and number of recordings.
     * 
     * @param position
     * @param count
     */
    public void updateTitle(int position, int count) {
        if (actionBar.getSelectedNavigationIndex() == position) {
            switch (position) {
            case 0:
                actionBar.setSubtitle(count + " " + getString(R.string.completed));
                break;
            case 1:
                actionBar.setSubtitle(count + " " + getString(R.string.upcoming));
                break;
            case 2:
                actionBar.setSubtitle(count + " " + getString(R.string.failed));
                break;
            }
        }
    }
    
    /**
     * Adapter that manages and holds the fragments of the different recording
     * types.
     * 
     * @author rsiebert
     * 
     */
    private class RecordingListPagerAdapter extends FragmentPagerAdapter {
        public RecordingListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new RecordingListFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("tabIndex", position);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getCount() {
            return actionBar.getTabCount();
        }
    }

    /**
     * Called when an activity was closed and this one is active again. Reloads
     * all data if the connection details have changed, a new one was created.
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
            if (resultCode == RESULT_OK){
                // Reload all data from the server
                if (data.getBooleanExtra("reconnect", false)) {
                    Utils.connect(this, true);
                }
                // Restart the activity
                restart = data.getBooleanExtra("restart", false);
                if (restart) {
                    Intent intent = getIntent();
                    intent.putExtra("restart", restart);
                    setResult(RESULT_OK, intent);
                    finish();
                    startActivityForResult(intent, Constants.RESULT_CODE_RECORDINGS);
                }
            }
        }
    }

    @Override
    public void setActionBarTitle(String string, final String tag) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setActionBarSubtitle(final String subtitle, final String tag) {
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public void setActionBarIcon(final Channel channel, String tag) {
        if (actionBar != null && channel != null) {
            // Show or hide the channel icon if required
            boolean showIcon = Utils.showChannelIcons(this);
            actionBar.setDisplayUseLogoEnabled(showIcon);
            if (showIcon) {
                actionBar.setIcon(new BitmapDrawable(getResources(), channel.iconBitmap));
            }
        }
    }
    
    /**
     * Provided by the channel list listener interface. This method is called
     * when the user has selected an item from the channel list. In dual pane
     * mode the program list on the right side will be recreated to show the new
     * programs for the selected channel. In normal mode the activity will be
     * called that shows the program list.
     */
    @Override
    public void onRecordingSelected(int position, Recording recording) {
        selectedRecordingListPosition = position;
        
        if (recording == null) {
            return;
        }

        if (!isDualPane) {
            // Start the activity
            Intent intent = new Intent(this, RecordingDetailsActivity.class);
            intent.putExtra("id", recording.id);
            startActivity(intent);
        } else {
            // Recreate the fragment with the new channel id
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment fragment = Fragment.instantiate(this, RecordingDetailsFragment.class.getName());
            Bundle args = new Bundle();
            args.putLong("id", recording.id);
            args.putBoolean("dual_pane", isDualPane);
            fragment.setArguments(args);

            // Replace the previous fragment with the new one
            ft.replace(R.id.details_fragment, fragment, RIGHT_FRAGMENT_TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }
    
    /**
     * Provided by the recording list listener interface. This method called when
     * the recording list has been fully populated. In a two pane layout a recording
     * list item must be preselected so that the recording detail on the right side
     * shows its contents from this very selected recording.
     */
    @Override
    public void onRecordingListPopulated() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
        if (f != null && isDualPane) {
            ((RecordingListFragment) f).setSelectedItem(selectedRecordingListPosition);
        }
    }
}
