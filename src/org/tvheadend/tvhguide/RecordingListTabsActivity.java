package org.tvheadend.tvhguide;

import java.lang.reflect.Field;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.ViewConfiguration;

public class RecordingListTabsActivity extends FragmentActivity {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingListTabsActivity.class.getSimpleName();
    private ActionBar actionBar = null;
    private RecordingListPagerAdapter adapter = null;
    private static ViewPager viewPager = null;
    private int selectedTabIndex = 0;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_layout);

        // setup action bar for tabs
        actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.menu_recordings);

        adapter = new RecordingListPagerAdapter(getFragmentManager());
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
        }
        catch (Exception ex) {
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
        tab = actionBar.newTab().setText(R.string.autorec).setTabListener(tabListener);
        actionBar.addTab(tab);

        adapter.notifyDataSetChanged();

        // Select the corresponding tab when the user swipes between pages with
        // a touch gesture.
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getActionBar().setSelectedNavigationItem(position);
                
                // Get the fragment of the current tabs. The default tag given by the 
                // FragmentPagerAdapater is "android:switcher:" + viewId + ":" + position;
                RecordingListFragment fragment = (RecordingListFragment) getFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + adapter.getItemId(position));
                
                // Update the action bar subtitle
                if (fragment != null)
                    fragment.updateTitle();
            }
        });

        // Restore the previously selected tab. This is usually required when
        // the user has rotated the screen.
        if (savedInstanceState != null) {
            selectedTabIndex = savedInstanceState.getInt("selected_tab_index", 0);
            getActionBar().setSelectedNavigationItem(selectedTabIndex);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        selectedTabIndex = getActionBar().getSelectedNavigationIndex();
        outState.putInt("selected_tab_index", selectedTabIndex);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, ChannelListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private class RecordingListPagerAdapter extends FragmentPagerAdapter {
        public RecordingListPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new RecordingListFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("tabIndex", position);
            bundle.putInt("selectedTabIndex", selectedTabIndex);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getCount() {
            return getActionBar().getTabCount();
        }
    }
}
