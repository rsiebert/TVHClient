package org.tvheadend.tvhclient.ui.features.settings


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
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.ui.common.callbacks.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.tasks.WakeOnLanTask
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity
import timber.log.Timber

class SettingsListConnectionsFragment : ListFragment(), BackPressedInterface, ActionMode.Callback {

    private lateinit var toolbarInterface: ToolbarInterface
    private lateinit var connectionListAdapter: ConnectionListAdapter
    private var actionMode: ActionMode? = null

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        settingsViewModel = ViewModelProviders.of(activity as AppCompatActivity).get(SettingsViewModel::class.java)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
            toolbarInterface.setTitle(getString(R.string.settings))
        }

        connectionListAdapter = ConnectionListAdapter(activity)
        listAdapter = connectionListAdapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        setHasOptionsMenu(true)

        settingsViewModel.connectionHasChanged = false
        activity?.let {
            settingsViewModel.allConnections.observe(it, Observer { connections ->
                if (connections != null) {
                    connectionListAdapter.clear()
                    connectionListAdapter.addAll(connections)
                    connectionListAdapter.notifyDataSetChanged()
                    toolbarInterface.setSubtitle(it.resources.getQuantityString(
                            R.plurals.number_of_connections,
                            connectionListAdapter.count,
                            connectionListAdapter.count))
                }
            })
        }
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
            R.id.menu_add -> {
                val intent = Intent(activity, SettingsActivity::class.java)
                intent.putExtra("setting_type", "add_connection")
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val position = listView.checkedItemPosition
        val connection = connectionListAdapter.getItem(position) ?: return false
        val intent: Intent
        when (item.itemId) {
            R.id.menu_set_active -> {
                connection.isActive = true
                settingsViewModel.updateConnection(connection)
                settingsViewModel.connectionHasChanged = true
                mode.finish()
                return true
            }

            R.id.menu_set_not_active -> {
                connection.isActive = false
                settingsViewModel.updateConnection(connection)
                mode.finish()
                return true
            }

            R.id.menu_edit -> {
                intent = Intent(activity, SettingsActivity::class.java)
                intent.putExtra("setting_type", "edit_connection")
                intent.putExtra("connection_id", connection.id)
                startActivity(intent)
                mode.finish()
                return true
            }

            R.id.menu_send_wol -> {
                context?.let {
                    WakeOnLanTask(it, connection).execute()
                }
                mode.finish()
                return true
            }

            R.id.menu_delete -> {
                context?.let {
                    MaterialDialog.Builder(it)
                            .content(getString(R.string.delete_connection, connection.name))
                            .positiveText(getString(R.string.delete))
                            .negativeText(getString(R.string.cancel))
                            .onPositive { _, _ -> settingsViewModel.removeConnection(connection) }
                            .onNegative { dialog, _ -> dialog.cancel() }
                            .show()
                }
                mode.finish()
                return true
            }

            else -> return false
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Inflate a menu resource providing context menu items
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.connection_list_options_menu, menu)
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
            menu.getItem(0).isVisible = !connection.wolMacAddress.isNullOrEmpty()
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
                MaterialDialog.Builder(it)
                        .title(R.string.dialog_title_disconnect_from_server)
                        .content(R.string.dialog_content_disconnect_from_server)
                        .positiveText(R.string.disconnect)
                        .onPositive { _, _ -> reconnect() }
                        .show()
            }
            settingsViewModel.connectionHasChanged -> context?.let {
                MaterialDialog.Builder(it)
                        .title(R.string.dialog_title_connection_changed)
                        .content(R.string.dialog_content_connection_changed)
                        .positiveText(R.string.connect)
                        .onPositive { _, _ -> reconnect() }
                        .show()
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
        activity?.stopService(Intent(activity, HtspService::class.java))

        settingsViewModel.setSyncRequiredForActiveConnection()

        val intent = Intent(activity, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity?.startActivity(intent)

        settingsViewModel.connectionHasChanged = false
        activity?.finish()
    }
}
