package org.tvheadend.tvhclient.ui.features.settings


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.RecyclerviewFragmentBinding
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.common.WakeOnLanTask
import org.tvheadend.tvhclient.ui.common.interfaces.BackPressedInterface
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import org.tvheadend.tvhclient.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclient.ui.features.MainActivity

class SettingsListConnectionsFragment : Fragment(), BackPressedInterface, ActionMode.Callback, RecyclerViewClickInterface {

    private lateinit var binding: RecyclerviewFragmentBinding
    private var activeConnectionId: Int = -1
    private var connectionHasChanged: Boolean = false
    private lateinit var toolbarInterface: ToolbarInterface
    private lateinit var recyclerViewAdapter: ConnectionRecyclerViewAdapter
    private lateinit var settingsViewModel: SettingsViewModel

    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = RecyclerviewFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsViewModel = ViewModelProvider(activity as SettingsActivity)[SettingsViewModel::class.java]

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
            toolbarInterface.setTitle(getString(R.string.pref_connections))
        }

        recyclerViewAdapter = ConnectionRecyclerViewAdapter(this, viewLifecycleOwner)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = recyclerViewAdapter

        settingsViewModel.connectionListLiveData.observe(viewLifecycleOwner) { connections ->
            if (connections != null) {
                recyclerViewAdapter.addItems(connections)
                context?.let {
                    toolbarInterface.setSubtitle(
                        it.resources.getQuantityString(
                            R.plurals.number_of_connections,
                            recyclerViewAdapter.itemCount,
                            recyclerViewAdapter.itemCount
                        )
                    )
                }
            }
        }
        settingsViewModel.activeConnectionLiveData.observe(viewLifecycleOwner) { connection ->
            connectionHasChanged = connection != null && connection.id != settingsViewModel.connectionToEdit.id
            activeConnectionId = connection?.id ?: -1
        }

        setHasOptionsMenu(true)
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
        val position = recyclerViewAdapter.selectedPosition
        if (recyclerViewAdapter.itemCount <= position) return false

        val connection = recyclerViewAdapter.getItem(position) ?: return false
        return when (item.itemId) {
            R.id.menu_set_connection_active -> setConnectionActiveOrInactive(connection, mode, true)
            R.id.menu_set_connection_not_active -> setConnectionActiveOrInactive(connection, mode, false)
            R.id.menu_edit_connection -> editConnection(connection, mode)
            R.id.menu_send_wol -> {
                WakeOnLanTask(lifecycleScope, ctx, connection)
                mode.finish()
                true
            }
            R.id.menu_delete_connection -> deleteConnection(ctx, connection, mode)
            else -> false
        }
    }

    private fun addConnection(): Boolean {
        settingsViewModel.setNavigationMenuId("add_connection")
        return true
    }

    private fun editConnection(connection: Connection, mode: ActionMode): Boolean {
        settingsViewModel.connectionIdToBeEdited = connection.id
        settingsViewModel.setNavigationMenuId("edit_connection")
        mode.finish()
        return true
    }

    private fun setConnectionActiveOrInactive(connection: Connection, mode: ActionMode, active: Boolean): Boolean {
        connection.isActive = active
        settingsViewModel.updateConnection(connection)
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
        val position = recyclerViewAdapter.selectedPosition
        val connection = recyclerViewAdapter.getItem(position)
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
            activeConnectionId < 0 -> context?.let {
                MaterialDialog(it).show {
                    title(R.string.disconnect_from_server)
                    message(R.string.no_active_connection)
                    positiveButton(R.string.disconnect) {
                        updateConnectionAndRestartApplication()
                    }
                }
            }
            connectionHasChanged -> context?.let {
                MaterialDialog(it).show {
                    title(R.string.connect_to_new_server)
                    message(R.string.connection_changed)
                    positiveButton(R.string.connect) {
                        updateConnectionAndRestartApplication()
                    }
                }
            }
            else -> {
                (activity as RemoveFragmentFromBackstackInterface).removeFragmentFromBackstack()
            }
        }
    }

    private fun updateConnectionAndRestartApplication() {
        context?.stopService(Intent(context, ConnectionService::class.java))
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context?.startActivity(intent)
    }

    override fun onClick(view: View, position: Int) {
        actionMode?.finish()
        if (actionMode == null) {
            recyclerViewAdapter.setPosition(position)
            startActionMode()
        }
    }

    override fun onLongClick(view: View, position: Int): Boolean {
        return true
    }
}
