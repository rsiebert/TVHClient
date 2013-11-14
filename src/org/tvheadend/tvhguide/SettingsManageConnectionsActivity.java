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

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhguide.adapter.ConnectionListAdapter;
import org.tvheadend.tvhguide.model.Connection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class SettingsManageConnectionsActivity extends Activity {

    private ConnectionListAdapter connAdapter;
    private List<Connection> connList;
    private ListView connListView;
    protected int prevPosition;
    
    @Override
    public void onCreate(Bundle icicle) {
        
        // Apply the specified theme
        setTheme(Utils.getThemeId(this));
        
        super.onCreate(icicle);
        setContentView(R.layout.list_layout);
        
        // Setup the action bar and show the title
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(R.string.pref_manage_connections);
        
        connList = new ArrayList<Connection>();
        connAdapter = new ConnectionListAdapter(this, connList);
        connListView = (ListView) findViewById(R.id.item_list);
        connListView.setAdapter(connAdapter);
        registerForContextMenu(connListView);
    }

    public void onResume() {
        super.onResume();
        
        showConnections();
    }
    
    private void showConnections() {
        connList.clear();
        
        List<Connection> cl = DatabaseHelper.getInstance().getConnections();
        if (cl != null && cl.size() > 0) {
            for (int i = 0; i < cl.size(); ++i) {
                connList.add(cl.get(i));
            }
        }
        
        connAdapter.sort();
        connAdapter.notifyDataSetChanged();
        getActionBar().setSubtitle(connAdapter.getCount() + " " + getString(R.string.pref_connections));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
        // Get the currently selected program from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Connection c = connAdapter.getItem(info.position);
        
        switch (item.getItemId()) {
        case R.id.menu_select:
            Log.i("Manage", "select active connection");
            
            // Switch the selection status
            c.selected = (c.selected) ? false : true;
            
            // Set the previous connection to unselected 
            // if the selected one is set as selected
            if (c.selected) {
                Connection previousConn = DatabaseHelper.getInstance().getSelectedConnection();
                if (previousConn != null) {
                    previousConn.selected = false;
                    DatabaseHelper.getInstance().updateConnection(previousConn);
                }
            };
            
            // Update the currently selected connection
            DatabaseHelper.getInstance().updateConnection(c);

            // Refresh the display
            showConnections();
            
            return true;

        case R.id.menu_deselect:
            c.selected = false;
            DatabaseHelper.getInstance().updateConnection(c);
            return true;

        case R.id.menu_edit:
            Log.i("Manage", "Editing connection " + c.id);
            Intent intent = new Intent(this, SettingsAddConnectionActivity.class);
            intent.putExtra("id", c.id);
            startActivity(intent);
            return true;

        case R.id.menu_delete:
            
            // Show confirmation dialog to cancel 
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete connection");
            builder.setMessage(getString(R.string.confirm_delete));

            // Define the action of the yes button
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (DatabaseHelper.getInstance().removeConnection(c.id)) {
                        connAdapter.remove(c);
                        connAdapter.notifyDataSetChanged();
                        connAdapter.sort();
                    }
                }
            });
            // Define the action of the no button
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.connection_menu, menu);
        
        // Get the currently selected program from the list
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Connection connection = connAdapter.getItem(info.position);
        
        
        
        // Set the title of the context menu and show or hide 
        // the menu items depending on the connection
        menu.setHeaderTitle(connection.name);
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
            startActivity(intent);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
