package org.tvheadend.tvhclient.fragments;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.WakeOnLanTask;
import org.tvheadend.tvhclient.adapter.ConnectionListAdapter;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.SettingsInterface;
import org.tvheadend.tvhclient.model.Connection;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

@SuppressWarnings("deprecation")
public class SettingsShowConnectionsFragment extends Fragment implements ActionMode.Callback {

    @SuppressWarnings("unused")
    private final static String TAG = SettingsShowConnectionsFragment.class.getSimpleName();

    private ActionBarActivity activity;
    private ActionBarInterface actionBarInterface;
    private SettingsInterface settingsInterface;

    private ConnectionListAdapter adapter;
    private ListView listView;
    private List<Connection> connList;
    private ActionMode actionMode;

    private DatabaseHelper dbh;
    @SuppressWarnings("unused")
    private TVHClientApplication app = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.list_layout, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (ActionBarActivity) activity;
        dbh = DatabaseHelper.getInstance(activity);
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public void onDetach() {
        actionBarInterface = null;
        settingsInterface = null;
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof SettingsInterface) {
            settingsInterface = (SettingsInterface) activity;
        }

        connList = new ArrayList<Connection>();
        adapter = new ConnectionListAdapter(activity, connList);
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

        setHasOptionsMenu(true);
    }

    private void startActionMode() {
        actionMode = activity.startSupportActionMode(this);
        if (actionMode != null) {
            actionMode.invalidate();
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
        List<Connection> cl = dbh.getConnections();
        if (cl != null && cl.size() > 0) {
            for (int i = 0; i < cl.size(); ++i) {
                connList.add(cl.get(i));
            }
        }
        adapter.sort();
        adapter.notifyDataSetChanged();
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.settings));
            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.pref_connections));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.preference_connections, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
            if (settingsInterface != null) {
                settingsInterface.showAddConnection();
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Switched the selection status of the connection by setting the previous
     * connection to unselected if the selected one is set as selected.
     * 
     * @param c
     */
    private void setConnectionActive(Connection c) {
        if (settingsInterface != null) {
            settingsInterface.reconnect();
        }

        // Switch the selection status
        c.selected = (c.selected) ? false : true;
        if (c.selected) {
            Connection previousConn = dbh.getSelectedConnection();
            if (previousConn != null) {
                previousConn.selected = false;
                dbh.updateConnection(previousConn);
            }
        };
        // Update the currently selected connection and refresh the display
        dbh.updateConnection(c);
        showConnections();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Get the currently selected program from the list
        int position = listView.getCheckedItemPosition();
        final Connection c = adapter.getItem(position);

        switch (item.getItemId()) {
        case R.id.menu_set_active:
            if (!c.selected && settingsInterface != null) {
                settingsInterface.reconnect();
            }
            setConnectionActive(c);
            mode.finish();
            return true;

        case R.id.menu_set_not_active:
            if (c.selected && settingsInterface != null) {
                settingsInterface.reconnect();
            }
            c.selected = false;
            dbh.updateConnection(c);
            showConnections();
            mode.finish();
            return true;

        case R.id.menu_edit:
            if (settingsInterface != null) {
                settingsInterface.showEditConnection(c.id);
            }
            mode.finish();
            return true;

        case R.id.menu_send_wol:
            if (c != null) {
                WakeOnLanTask task= new WakeOnLanTask(activity, c);
                task.execute();
            }
            mode.finish();
            return true;

        case R.id.menu_delete:
            // Show confirmation dialog to cancel
            new MaterialDialog.Builder(activity)
                    .content(getString(R.string.delete_connection, c.name))
                    .positiveText(getString(R.string.delete))
                    .negativeText(getString(android.R.string.no))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            if (dbh.removeConnection(c.id)) {
                                adapter.remove(c);
                                adapter.notifyDataSetChanged();
                                adapter.sort();
                                if (actionBarInterface != null) {
                                    actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.pref_connections));
                                }
                                if (settingsInterface != null) {
                                    settingsInterface.reconnect();
                                }
                            }
                        }
                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            dialog.cancel();
                        }
                    }).show();
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
