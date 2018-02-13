package org.tvheadend.tvhclient.ui.settings;

import android.app.ListFragment;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.ConnectionRepository;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.data.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.ui.common.BackPressedInterface;
import org.tvheadend.tvhclient.ui.startup.StartupActivity;

public class SettingsListConnectionsFragment extends ListFragment implements BackPressedInterface, ActionMode.Callback, WakeOnLanTaskCallback {

    private ToolbarInterface toolbarInterface;
    private ConnectionListAdapter connectionListAdapter;
    private ActionMode actionMode;
    private AppCompatActivity activity;
    private ConnectionRepository repository;
    private int currentActiveConnectionId;
    private int initialActiveConnectionId;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        toolbarInterface.setTitle(getString(R.string.settings));

        repository = new ConnectionRepository(activity);
        connectionListAdapter = new ConnectionListAdapter(activity);
        setListAdapter(connectionListAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        setHasOptionsMenu(true);

        ConnectionViewModel viewModel = ViewModelProviders.of(activity).get(ConnectionViewModel.class);
        viewModel.getAllConnections().observe(activity, connections -> {
            if (connections != null) {
                connectionListAdapter.clear();
                connectionListAdapter.addAll(connections);
                connectionListAdapter.notifyDataSetChanged();
                toolbarInterface.setSubtitle(getResources().getQuantityString(
                        R.plurals.number_of_connections,
                        connectionListAdapter.getCount(),
                        connectionListAdapter.getCount()));

                currentActiveConnectionId = 0;
                for (Connection connection : connections) {
                    if (connection.isActive()) {
                        currentActiveConnectionId = connection.getId();
                        break;
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            initialActiveConnectionId = savedInstanceState.getInt("initialActiveConnectionId");
        } else {
            initialActiveConnectionId = (repository.getActiveConnectionSync() != null) ? repository.getActiveConnectionSync().getId() : 0;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("initialActiveConnectionId", initialActiveConnectionId);
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
        actionMode = activity.startActionMode(this);
        if (actionMode != null) {
            actionMode.invalidate();
        }
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
                Intent intent = new Intent(activity, SettingsManageConnectionActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int position = getListView().getCheckedItemPosition();
        Connection connection = connectionListAdapter.getItem(position);
        if (connection == null) {
            return false;
        }
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_set_active:
                connection.setActive(true);
                repository.updateConnectionSync(connection);
                mode.finish();
                return true;

            case R.id.menu_set_not_active:
                connection.setActive(false);
                repository.updateConnectionSync(connection);
                mode.finish();
                return true;

            case R.id.menu_edit:
                intent = new Intent(activity, SettingsManageConnectionActivity.class);
                intent.putExtra("connection_id", connection.getId());
                startActivity(intent);
                mode.finish();
                return true;

            case R.id.menu_send_wol:
                WakeOnLanTask task = new WakeOnLanTask(activity, this, connection);
                task.execute();
                mode.finish();
                return true;

            case R.id.menu_delete:
                new MaterialDialog.Builder(activity)
                        .content(getString(R.string.delete_connection, connection.getName()))
                        .positiveText(getString(R.string.delete))
                        .negativeText(getString(R.string.cancel))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                repository.removeConnectionSync(connection.getId());
                                currentActiveConnectionId = 0;
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
            menu.getItem(0).setVisible(!TextUtils.isEmpty(connection.getWolMacAddress()));
            // Show or hide the activate / deactivate menu items
            menu.getItem(1).setVisible(!connection.isActive());
            menu.getItem(2).setVisible(connection.isActive());

            mode.setTitle(connection.getName());
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
        if (initialActiveConnectionId != currentActiveConnectionId) {
            new MaterialDialog.Builder(activity)
                    .title("Reconnect to server required")
                    .content("A new active connection was defined. " +
                            "The application will be restarted and a new initial sync will be performed.")
                    .positiveText(android.R.string.ok)
                    .onPositive((dialog, which) -> reconnect())
                    .show();
        } else if (currentActiveConnectionId == 0) {
            new MaterialDialog.Builder(activity)
                    .title("Connection will be closed")
                    .content("No active connection is defined. The existing connection to the server will be closed.")
                    .positiveText(android.R.string.ok)
                    .onPositive((dialog, which) -> reconnect())
                    .show();
        } else {
            activity.finish();
        }
    }

    private void reconnect() {
        // Save the information that a new sync is required
        // Then restart the application to show the sync fragment
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("initial_sync_done", false);
        editor.apply();
        Intent intent = new Intent(activity, StartupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
