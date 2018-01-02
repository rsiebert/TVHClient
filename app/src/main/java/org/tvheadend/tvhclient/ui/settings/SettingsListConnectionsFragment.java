package org.tvheadend.tvhclient.ui.settings;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.NavigationActivity;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.service.HTSService;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTaskCallback;

import java.util.ArrayList;
import java.util.List;

// TODO consider initial_setup argument

public class SettingsListConnectionsFragment extends ListFragment implements BackPressedInterface, ActionMode.Callback, WakeOnLanTaskCallback {

    private ToolbarInterface toolbarInterface;
    private ConnectionListAdapter connectionListAdapter;
    private List<Connection> connectionList;
    private ActionMode actionMode;
    private boolean newActiveConnectionSelected;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) getActivity();
        }
        toolbarInterface.setTitle(getString(R.string.settings));

        connectionList = new ArrayList<>();
        connectionListAdapter = new ConnectionListAdapter(getActivity(), connectionList);
        setListAdapter(connectionListAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        setHasOptionsMenu(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (actionMode == null) {
            getListView().setItemChecked(position, true);
            getListView().setSelected(true);
            startActionMode();
        }
    }

    private void startActionMode() {
        actionMode = getActivity().startActionMode(this);
        if (actionMode != null) {
            actionMode.invalidate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showConnections();
    }

    private void showConnections() {
        // Add all connections to the list
        connectionList.clear();
        connectionList.addAll(DatabaseHelper.getInstance(getActivity()).getConnections());
        connectionListAdapter.sort();
        connectionListAdapter.notifyDataSetChanged();
        showConnectionCount();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.connection_add_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                Intent intent = new Intent(getActivity(), SettingsManageConnectionActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setConnectionStatus(Connection c, boolean active) {
        // Stop the service
        Intent intent = new Intent(getActivity(), HTSService.class);
        getActivity().stopService(intent);
        // Set the new connection to active
        c.selected = active;
        if (c.selected) {
            Connection previousConnection = DatabaseHelper.getInstance(getActivity()).getSelectedConnection();
            if (previousConnection != null) {
                previousConnection.selected = false;
                DatabaseHelper.getInstance(getActivity()).updateConnection(previousConnection);
            }
        }
        // Update the currently selected connection and refresh the display
        DatabaseHelper.getInstance(getActivity()).updateConnection(c);
        showConnections();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int position = getListView().getCheckedItemPosition();
        Connection connection = connectionListAdapter.getItem(position);
        if (connection == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.menu_set_active:
                setConnectionStatus(connection, true);
                newActiveConnectionSelected = true;
                mode.finish();
                return true;
            case R.id.menu_set_not_active:
                setConnectionStatus(connection, false);
                newActiveConnectionSelected = false;
                mode.finish();
                return true;
            case R.id.menu_edit:
                Intent intent = new Intent(getActivity(), SettingsManageConnectionActivity.class);
                intent.putExtra("connection_id", connection.id);
                startActivity(intent);
                mode.finish();
                return true;
            case R.id.menu_send_wol:
                WakeOnLanTask task = new WakeOnLanTask(getActivity(), this, connection);
                task.execute();
                mode.finish();
                return true;
            case R.id.menu_delete:
                new MaterialDialog.Builder(getActivity())
                        .content(getString(R.string.delete_connection, connection.name))
                        .positiveText(getString(R.string.delete))
                        .negativeText(getString(R.string.cancel))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                if (DatabaseHelper.getInstance(getActivity()).removeConnection(connection.id)) {
                                    connectionListAdapter.remove(connection);
                                    connectionListAdapter.notifyDataSetChanged();
                                    showConnectionCount();
                                }
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.cancel();
                            }
                        }).show();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private void showConnectionCount() {
        toolbarInterface.setSubtitle(getResources().getQuantityString(
                R.plurals.number_of_connections,
                connectionListAdapter.getCount(),
                connectionListAdapter.getCount()));
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.connection_list_options_menu, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Get the currently selected program from the list
        int position = getListView().getCheckedItemPosition();
        final Connection connection = connectionListAdapter.getItem(position);
        if (connection != null) {
            // Show or hide the wake on LAN menu item
            menu.getItem(0).setVisible(!TextUtils.isEmpty(connection.wol_mac_address));
            // Show or hide the activate / deactivate menu items
            menu.getItem(1).setVisible(!connection.selected);
            menu.getItem(2).setVisible(connection.selected);

            mode.setTitle(connection.name);
        }
        return true;
    }

    @Override
    public void notify(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }


    @Override
    public void onBackPressed() {
        // If a new connection has been selected and it is
        // active restart the service to do a new initial sync
        if (newActiveConnectionSelected && DatabaseHelper.getInstance(getActivity()).getSelectedConnection() != null) {
            Intent intent = new Intent(getActivity(), NavigationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
