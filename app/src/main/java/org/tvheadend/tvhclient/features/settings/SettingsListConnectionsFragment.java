package org.tvheadend.tvhclient.features.settings;

import android.app.ListFragment;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.features.shared.tasks.WakeOnLanTask;
import org.tvheadend.tvhclient.features.shared.tasks.WakeOnLanTaskCallback;
import org.tvheadend.tvhclient.features.startup.SplashActivity;

import javax.inject.Inject;

import timber.log.Timber;

public class SettingsListConnectionsFragment extends ListFragment implements BackPressedInterface, ActionMode.Callback, WakeOnLanTaskCallback {

    private ToolbarInterface toolbarInterface;
    private ConnectionListAdapter connectionListAdapter;
    private ActionMode actionMode;
    private AppCompatActivity activity;
    @Inject
    protected AppRepository appRepository;
    private ConnectionViewModel viewModel;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainApplication.getComponent().inject(this);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        toolbarInterface.setTitle(getString(R.string.settings));

        connectionListAdapter = new ConnectionListAdapter(activity);
        setListAdapter(connectionListAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        setHasOptionsMenu(true);

        viewModel = ViewModelProviders.of(activity).get(ConnectionViewModel.class);
        viewModel.getAllConnections().observe(activity, connections -> {
            if (connections != null) {
                connectionListAdapter.clear();
                connectionListAdapter.addAll(connections);
                connectionListAdapter.notifyDataSetChanged();
                toolbarInterface.setSubtitle(getResources().getQuantityString(
                        R.plurals.number_of_connections,
                        connectionListAdapter.getCount(),
                        connectionListAdapter.getCount()));
            }
        });
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
                Intent intent = new Intent(activity, SettingsActivity.class);
                intent.putExtra("setting_type", "add_connection");
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
                appRepository.getConnectionData().updateItem(connection);
                viewModel.setConnectionHasChanged(true);
                mode.finish();
                return true;

            case R.id.menu_set_not_active:
                connection.setActive(false);
                appRepository.getConnectionData().updateItem(connection);
                mode.finish();
                return true;

            case R.id.menu_edit:
                intent = new Intent(activity, SettingsActivity.class);
                intent.putExtra("setting_type", "edit_connection");
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
                        .onPositive((dialog, which) -> appRepository.getConnectionData().removeItem(connection))
                        .onNegative((dialog, which) -> dialog.cancel())
                        .show();
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
        if (viewModel.getActiveConnectionId() < 0) {
            new MaterialDialog.Builder(activity)
                    .title(R.string.dialog_title_disconnect_from_server)
                    .content(R.string.dialog_content_disconnect_from_server)
                    .positiveText(android.R.string.ok)
                    .onPositive((dialog, which) -> reconnect())
                    .show();
        } else if (viewModel.getConnectionHasChanged()) {
            new MaterialDialog.Builder(activity)
                    .title(R.string.dialog_title_connection_changed)
                    .content(R.string.dialog_content_connection_changed)
                    .positiveText(android.R.string.ok)
                    .onPositive((dialog, which) -> reconnect())
                    .show();
        } else {
            viewModel.setConnectionHasChanged(false);
            activity.finish();
        }
    }

    /**
     * Save the information that a new sync is required and stop
     * the service so it can refresh the active connection.
     * Then restart the application to show the startup fragment
     */
    private void reconnect() {
        Timber.d("Reconnecting to server, new initial sync will be done");
        activity.stopService(new Intent(activity, EpgSyncService.class));

        if (viewModel.getActiveConnectionId() >= 0) {
            Connection connection = appRepository.getConnectionData().getItemById(viewModel.getActiveConnectionId());
            connection.setSyncRequired(true);
            connection.setLastUpdate(0);
            appRepository.getConnectionData().updateItem(connection);
        }

        Intent intent = new Intent(activity, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);

        viewModel.setConnectionHasChanged(false);
        activity.finish();
    }
}
