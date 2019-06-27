package org.tvheadend.tvhclient.ui.features.settings


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.ListFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.sendWakeOnLanPacket
import timber.log.Timber

class SettingsListConnectionsFragment : ListFragment(), BackPressedInterface, ActionMode.Callback {

    private lateinit var toolbarInterface: ToolbarInterface
    private lateinit var connectionListAdapter: ConnectionListAdapter
    private lateinit var settingsViewModel: SettingsViewModel

    private var actionMode: ActionMode? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingsViewModel = ViewModelProviders.of(activity as AppCompatActivity).get(SettingsViewModel::class.java)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
            toolbarInterface.setTitle(getString(R.string.settings))
        }

        connectionListAdapter = ConnectionListAdapter(activity as AppCompatActivity)
        listAdapter = connectionListAdapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        setHasOptionsMenu(true)

        settingsViewModel.connectionHasChanged = false
        settingsViewModel.allConnections.observe(viewLifecycleOwner, Observer { connections ->
            if (connections != null) {
                connectionListAdapter.clear()
                connectionListAdapter.addAll(connections)
                connectionListAdapter.notifyDataSetChanged()
                context?.let {
                    toolbarInterface.setSubtitle(it.resources.getQuantityString(
                            R.plurals.number_of_connections,
                            connectionListAdapter.count,
                            connectionListAdapter.count))
                }
            }
        })
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        if (actionMode == null) {
            listView.setItemChecked(position, true)
            listView.isSelected = true
            startActionMode()
        }
    }

    private fun startActionMode() {
        actionMode = activity?.startActionMode(this)
        actionMode?.invalidate()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.connection_add_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add_connection -> addConnection()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val ctx = context ?: return false
        val position = listView.checkedItemPosition
        if (connectionListAdapter.count <= position) return false

        val connection = connectionListAdapter.getItem(position) ?: return false
        return when (item.itemId) {
            R.id.menu_set_connection_active -> setConnectionActiveOrInactive(connection, mode, true)
            R.id.menu_set_connection_not_active -> setConnectionActiveOrInactive(connection, mode, false)
            R.id.menu_edit_connection -> editConnection(connection, mode)
            R.id.menu_send_wol -> sendWakeOnLanPacket(ctx, connection, mode)
            R.id.menu_delete_connection -> deleteConnection(ctx, connection, mode)
            else -> false
        }
    }

    private fun addConnection(): Boolean {
        val intent = Intent(activity, SettingsActivity::class.java)
        intent.putExtra("setting_type", "add_connection")
        startActivity(intent)
        return true
    }

    private fun editConnection(connection: Connection, mode: ActionMode): Boolean {
        val intent = Intent(activity, SettingsActivity::class.java)
        intent.putExtra("setting_type", "edit_connection")
        intent.putExtra("connection_id", connection.id)
        startActivity(intent)
        mode.finish()
        return true
    }

    private fun setConnectionActiveOrInactive(connection: Connection, mode: ActionMode, active: Boolean): Boolean {
        connection.isActive = active
        settingsViewModel.updateConnection(connection)
        settingsViewModel.connectionHasChanged = true
        mode.finish()
        return true
    }

    private fun deleteConnection(context: Context, connection: Connection, mode: ActionMode): Boolean {
        MaterialDialog(context).show {
            message(text = getString(R.string.delete_connection, connection.name))
            positiveButton(R.string.delete) {
                settingsViewModel.removeConnection(connection)
            }
            negativeButton(R.string.cancel) {
                cancel()
            }
        }
        mode.finish()
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.connection_list_options_menu, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Get the currently selected program from the list
        val position = listView.checkedItemPosition
        val connection = connectionListAdapter.getItem(position)
        if (connection != null) {
            // Show or hide the wake on LAN menu item
            menu.getItem(0).isVisible = connection.isWolEnabled && !connection.wolMacAddress.isNullOrEmpty()
            // Show or hide the activate / deactivate menu items
            menu.getItem(1).isVisible = !connection.isActive
            menu.getItem(2).isVisible = connection.isActive

            mode.title = connection.name
        }
        return true
    }

    override fun onBackPressed() {
        when {
            settingsViewModel.activeConnectionId < 0 -> context?.let {
                MaterialDialog(it).show {
                    title(R.string.dialog_title_disconnect_from_server)
                    message(R.string.dialog_content_disconnect_from_server)
                    positiveButton(R.string.disconnect) {
                        reconnect()
                    }
                }
            }
            settingsViewModel.connectionHasChanged -> context?.let {
                MaterialDialog(it).show {
                    title(R.string.dialog_title_connection_changed)
                    message(R.string.dialog_content_connection_changed)
                    positiveButton(R.string.connect) { reconnect() }
                }
            }
            else -> {
                settingsViewModel.connectionHasChanged = false
                activity?.finish()
            }
        }
    }

    /**
     * Save the information that a new sync is required and stop
     * the service so it can refresh the active connection.
     * Then restart the application to show the startup fragment
     */
    private fun reconnect() {
        Timber.d("Reconnecting to server, new initial sync will be done")
        settingsViewModel.connectionHasChanged = false
        settingsViewModel.updateConnectionAndRestartApplication(context)
    }
}
