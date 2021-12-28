package org.tvheadend.tvhclient.ui.features.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ConnectionListAdapterBinding
import org.tvheadend.tvhclient.ui.common.interfaces.RecyclerViewClickInterface
import java.util.*

class ConnectionRecyclerViewAdapter internal constructor(private val clickCallback: RecyclerViewClickInterface, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<ConnectionRecyclerViewAdapter.ConnectionViewHolder>() {

    private var connectionList: MutableList<Connection> = ArrayList()
    var selectedPosition = 0
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ConnectionListAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = ConnectionViewHolder(itemBinding)
        itemBinding.lifecycleOwner = lifecycleOwner
        return viewHolder
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        if (connectionList.size > position) {
            val channel = connectionList[position]
            holder.bind(channel, position, clickCallback)
        }
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int, payloads: List<Any>) {
        onBindViewHolder(holder, position)
    }

    internal fun addItems(newItems: List<Connection>) {
        connectionList.clear()
        connectionList.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return connectionList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.connection_list_adapter
    }

    fun setPosition(pos: Int) {
        notifyItemChanged(selectedPosition)
        selectedPosition = pos
        notifyItemChanged(pos)
    }

    fun getItem(position: Int): Connection? {
        return if (connectionList.size > position && position >= 0) {
            connectionList[position]
        } else {
            null
        }
    }

    class ConnectionViewHolder(private val binding: ConnectionListAdapterBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(connection: Connection, position: Int, clickCallback: RecyclerViewClickInterface) {
            binding.connection = connection
            binding.position = position
            binding.callback = clickCallback
            binding.executePendingBindings()
        }
    }
}
