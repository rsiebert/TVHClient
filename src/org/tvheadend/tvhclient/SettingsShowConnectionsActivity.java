package org.tvheadend.tvhclient;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.adapter.ConnectionListAdapter;
import org.tvheadend.tvhclient.model.Connection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class SettingsShowConnectionsActivity extends ActionBarActivity implements Callback {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsShowConnectionsActivity.class.getSimpleName();
    
    private ConnectionListAdapter adapter;
    private ListView listView;
    private List<Connection> connList;
    private android.view.ActionMode actionMode;
    private Toolbar toolbar = null;
    private boolean reconnect = false;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getThemeId(this));
        super.onCreate(savedInstanceState);
        Utils.setLanguage(this);
        setContentView(R.layout.list_layout);

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
                startActionMode();
                view.setSelected(true);
                return;
            }
        });

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon((Utils.getThemeId(this) == R.style.CustomTheme_Light) ? R.drawable.ic_menu_back_light
                : R.drawable.ic_menu_back_dark);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                addConnection();
                return true;
            }
        });
        toolbar.inflateMenu(R.menu.preference_connections);
        toolbar.setTitle(R.string.menu_connections);
        
        // TODO add home button
    }

    private void startActionMode() {
        actionMode = toolbar.startActionMode(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        showConnections();
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        intent.putExtra(Constants.BUNDLE_RECONNECT, reconnect);
        setResult(RESULT_OK, intent);
        finish();
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

        if (adapter.getCount() > 0) {
            toolbar.setSubtitle(getString(R.string.number_of_connections, adapter.getCount()));
        } else {
            toolbar.setSubtitle(getString(R.string.no_connection_available));
        }
    }

    /**
     * 
     */
    private void addConnection() {
        Intent i = new Intent(this, SettingsManageConnectionActivity.class);
        startActivityForResult(i, Constants.RESULT_CODE_SETTINGS);
    }

    /**
     * Switched the selection status of the connection by setting the previous
     * connection to unselected if the selected one is set as selected.
     * 
     * @param c
     */
    private void setConnectionActive(Connection c) {
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
    public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
        // Get the currently selected program from the list
        int position = listView.getCheckedItemPosition();
        final Connection c = adapter.getItem(position);
        
        switch (item.getItemId()) {
        case R.id.menu_set_active:
            reconnect = true;
            setConnectionActive(c);
            mode.finish();
            return true;

        case R.id.menu_set_not_active:
            reconnect = true;
            c.selected = false;
            DatabaseHelper.getInstance().updateConnection(c);
            showConnections();
            mode.finish();
            return true;

        case R.id.menu_edit:
            Intent intent = new Intent(this, SettingsManageConnectionActivity.class);
            intent.putExtra(Constants.BUNDLE_CONNECTION_ID, c.id);
            startActivityForResult(intent, Constants.RESULT_CODE_SETTINGS);
            mode.finish();
            return true;

        case R.id.menu_send_wol:
            if (c != null) {
                WakeOnLanTask task= new WakeOnLanTask(this, c);
                task.execute();
            }
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
                        reconnect = true;
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
    public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.connection_menu, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(android.view.ActionMode mode) {
        actionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
        // Get the currently selected program from the list
        int position = listView.getCheckedItemPosition();
        final Connection c = adapter.getItem(position);

        // Show or hide the wake on LAN menu item
        if (c != null && c.wol_address != null) {
            menu.getItem(0).setVisible((c.wol_address.length() > 0));
        } else {
            menu.getItem(0).setVisible(false);
        }

        // Show or hide the activate / deactivate menu item
        if (c != null && c.selected) {
            menu.getItem(1).setVisible(false);
        } else {
            menu.getItem(2).setVisible(false);
        }
        
        mode.setTitle(c.name);
        return true;
    }
}
