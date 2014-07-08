package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.adapter.DrawerListAdapter;
import org.tvheadend.tvhclient.model.DrawerItem;

import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private DrawerListAdapter drawerAdapter;
    private List<DrawerItem> drawerItemList;
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence drawerTitle;
    private CharSequence mainTitle;

    private ActionBar actionBar = null;
    private ChangeLogDialog changeLogDialog;

    // Indication weather the layout supports two fragments. This is usually
    // only available on tablets.
    private boolean isDualPane = false;

    // Saves the selected navigation drawer menu position so it can be
    // reselected when the orientation has changed.
    private int selectedDrawerMenuPosition = 0;

    private static final String SELECTED_DRAWER_MENU_POSITION = "selected_drawer_menu_position";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Utils.setLanguage(this);
        Log.i(TAG, "onCreate");

        // Check if the layout supports dual view (two fragments at the same
        // time). This is usually available on tablets
        View v = findViewById(R.id.right_fragment);
        isDualPane = v != null && v.getVisibility() == View.VISIBLE;

        DatabaseHelper.init(this.getApplicationContext());
        changeLogDialog = new ChangeLogDialog(this);

        // Setup action bar for tabs
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayUseLogoEnabled(Utils.showChannelIcons(this));

        // Save the current title of the action bar
        mainTitle = drawerTitle = getTitle();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Listens for drawer open and close events so we can modify the
        // contents of the action bar when the drawer is visible, such as to
        // change the title and remove action items that are contextual to the
        // main content.
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open,
                R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                actionBar.setTitle(mainTitle);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                actionBar.setTitle(drawerTitle);
                supportInvalidateOptionsMenu();

                // Disable the refresh menu if no connection is available or the
                // loading process is active. This is handled in the adapter
                drawerAdapter.notifyDataSetChanged();
            }
        };
        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        // Create the custom adapter for the menus in the navigation drawer.
        // Also set the listener to react to the user selection.
        drawerItemList = getDrawerMenuItemList();
        drawerAdapter = new DrawerListAdapter(this, drawerItemList, R.layout.drawer_list_item);
        drawerList.setAdapter(drawerAdapter);
        drawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleItemSelection(position);
            }
        });

        // Get any saved values from the bundle
        if (savedInstanceState != null) {
            // Get the previously selected channel item position
            selectedDrawerMenuPosition = savedInstanceState.getInt(SELECTED_DRAWER_MENU_POSITION, 0);
        }
    }

    /**
     * Populates the navigation drawer item list with the names, icon resources
     * and initial status and count information.
     * 
     * @return
     */
    private List<DrawerItem> getDrawerMenuItemList() {
        Log.i(TAG, "getDrawerMenuItemList");

        List<DrawerItem> list = new ArrayList<DrawerItem>();
        return list;
    }

    /**
     * Loads and shows the correct fragment depending on the selected navigation
     * menu item.
     * 
     * @param position
     */
    private void handleItemSelection(int position) {
        Log.i(TAG, "handleItemSelection " + position);

        // Highlight the selected item and close the drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);
    }
}
