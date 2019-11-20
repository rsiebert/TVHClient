package org.tvheadend.tvhclient.ui.features.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.ConnectionListAdapterBinding
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.ui.common.callbacks.RecyclerViewClickCallback
import java.util.*

class ConnectionRecyclerViewAdapter internal constructor(private val clickCallback: RecyclerViewClickCallback) : RecyclerView.Adapter<ConnectionRecyclerViewAdapter.ConnectionViewHolder>() {

    private var connectionList: MutableList<Connection> = ArrayList()
    var selectedPosition = 0
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ConnectionListAdapterBinding.inflate(layoutInflater, parent, false)
        val viewHolder = ConnectionViewHolder(itemBinding)
        itemBinding.lifecycleOwner = viewHolder
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

    override fun onViewAttachedToWindow(holder: ConnectionViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    override fun onViewDetachedFromWindow(holder: ConnectionViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    class ConnectionViewHolder(private val binding: ConnectionListAdapterBinding) : RecyclerView.ViewHolder(binding.root), LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        }

        fun markAttach() {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }

        fun markDetach() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }

        fun bind(connection: Connection, position: Int, clickCallback: RecyclerViewClickCallback) {
            binding.connection = connection
            binding.position = position
            binding.callback = clickCallback
            binding.executePendingBindings()
        }
    }
}
