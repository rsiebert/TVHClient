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

import java.lang.reflect.Field;

import org.tvheadend.tvhclient.interfaces.ActionBarInterface;

import android.content.Intent;
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
import android.view.ViewConfiguration;

public class RecordingListTabsActivity extends ActionBarActivity implements ActionBarInterface {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingListTabsActivity.class.getSimpleName();

    private ActionBar actionBar = null;
    private RecordingListPagerAdapter adapter = null;
    private static ViewPager viewPager = null;
    private boolean restart = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_layout);
        Utils.setLanguage(this);

        // setup action bar for tabs
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.menu_recordings);

        adapter = new RecordingListPagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);

        // Make the action bar collapse even when the hardware keys are present.
        // This overrides the default behavior of the action bar.
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // When the tab is selected, switch to the
                // corresponding page in the ViewPager.
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabUnselected(Tab tab, FragmentTransaction ft) {

            }
        };

        // Add the tabs with the different recording states
        Tab tab = actionBar.newTab().setText(R.string.completed).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.upcoming).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.failed).setTabListener(tabListener);
        actionBar.addTab(tab);

        adapter.notifyDataSetChanged();

        // Select the corresponding tab when the user swipes between pages with
        // a touch gesture.
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

        // Restore the previously selected tab. This is usually required when
        // the user has rotated the screen.
        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt("selected_recording_tab_index", 0);
            actionBar.setSelectedNavigationItem(index);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        int index = actionBar.getSelectedNavigationIndex();
        outState.putInt("selected_recording_tab_index", index);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Disable the refresh menu if no connection is 
        // available or the loading process is already active
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
        actionBar.setSubtitle(subtitle);
    }
}
