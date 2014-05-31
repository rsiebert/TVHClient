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

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.adapter.ConnectionListAdapter;
import org.tvheadend.tvhclient.model.Connection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class SettingsManageConnectionsActivity extends ActionBarActivity implements ActionMode.Callback {

    private ActionBar actionBar = null;
    private ConnectionListAdapter adapter;
    private List<Connection> connList;
    private ListView listView;
    protected int prevPosition;
    private boolean connectionChanged;
    private ActionMode actionMode;

    @Override
    public void onCreate(Bundle icicle) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(icicle);
        setContentView(R.layout.list_layout);
        
        // Setup the action bar and show the title
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.pref_manage_connections);
        
        connList = new ArrayList<Connection>();
        adapter = new ConnectionListAdapter(this, connList);
        listView = (ListView) findViewById(R.id.item_list);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Show the available menu options when the user clicks on a connection.
        // The options are realized by using the action mode instead of a
        // regular context menu. 
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode != null) {
                    return;
                }
                // Set the currently selected item as checked so we know which
                // position the user has clicked
                listView.setItemChecked(position, true);
                actionMode = startSupportActionMode(SettingsManageConnectionsActivity.this);
                view.setSelected(true);
                return;
            }
        });

        // Send out the wake on LAN package to wake up the selected connection
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Connection conn = adapter.getItem(position);
                wakeUpServer(conn);
                return true;
            }
        });
    }

    /**
     * 
     * @param conn
     */
    private void wakeUpServer(final Connection conn) {
        if (conn != null) {
            WakeOnLanTask task= new WakeOnLanTask(this, conn);
            task.execute();
        }    
    }

    @Override
    public void onResume() {
        super.onResume();
        showConnections();
    }

    /**
     * Shows all available connections from the database in the list.
     */
    private void showConnections() {
        connList.clear();
        List<Connection> cl = DatabaseHelper.getInstance().getConnections();
        if (cl != null && cl.size() > 0) {
            for (int i = 0; i < cl.size(); ++i) {
                connList.add(cl.get(i));
            }
        }
        adapter.sort();
        adapter.notifyDataSetChanged();
        actionBar.setSubtitle(adapter.getCount() + " " + getString(R.string.pref_connections));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.preference_connections, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;

        case R.id.menu_add:
            Intent intent = new Intent(this, SettingsAddConnectionActivity.class);
            startActivityForResult(intent, Constants.RESULT_CODE_CONNECTIONS);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("reconnect", connectionChanged);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /**
     * Switched the selection status of the connection by setting the previous
     * connection to unselected if the selected one is set as selected.
     * 
     * @param c
     */
    private void setConnectionActive(Connection c) {
        connectionChanged = true;

        // Switch the selection status
        c.selected = (c.selected) ? false : true;
        if (c.selected) {
            Connection previousConn = DatabaseHelper.getInstance().getSelectedConnection();
            if (previousConn != null) {
                previousConn.selected = false;
                DatabaseHelper.getInstance().updateConnection(previousConn);
            }
        };
        // Update the currently selected connection and refresh the display
        DatabaseHelper.getInstance().updateConnection(c);
        showConnections();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RESULT_CODE_CONNECTIONS) {
            if (resultCode == RESULT_OK) {
                connectionChanged = data.getBooleanExtra("reconnect", false);
            }
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Get the currently selected program from the list
        int position = listView.getCheckedItemPosition();
        final Connection c = adapter.getItem(position);
        
        switch (item.getItemId()) {
        case R.id.menu_set_active:
            setConnectionActive(c);
            mode.finish();
            return true;

        case R.id.menu_set_not_active:
            c.selected = false;
            DatabaseHelper.getInstance().updateConnection(c);
            showConnections();
            mode.finish();
            return true;

        case R.id.menu_edit:
            Intent intent = new Intent(this, SettingsAddConnectionActivity.class);
            intent.putExtra("id", c.id);
            startActivityForResult(intent, Constants.RESULT_CODE_SETTINGS);
            mode.finish();
            return true;
            
        case R.id.menu_delete:
            // Show confirmation dialog to cancel 
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.delete_connection, c.name));
            builder.setTitle(getString(R.string.menu_delete));
            // Define the action of the yes button
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (DatabaseHelper.getInstance().removeConnection(c.id)) {
                        adapter.remove(c);
                        adapter.notifyDataSetChanged();
                        adapter.sort();
                        actionBar.setSubtitle(adapter.getCount() + " " + getString(R.string.pref_connections));
                        connectionChanged = true;
                    }
                }
            });
            // Define the action of the no button
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            mode.finish();
            return true;

        default:
            return false;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.connection_menu, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Get the currently selected program from the list
        int position = listView.getCheckedItemPosition();
        final Connection c = adapter.getItem(position);

        // Show or hide the activate / deactivate menu item
        if (c != null && c.selected) {
            menu.getItem(0).setVisible(false);
        } else {
            menu.getItem(1).setVisible(false);
        }
        mode.setTitle(c.name);
        return true;
    }
}
