package org.tvheadend.tvhclient.ui.features.dvr

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.util.getIconUrl
import java.lang.ref.WeakReference

// TODO convert to data binding

class ChannelListSelectionAdapter internal constructor(context: Context, private val channelList: List<Channel>) : RecyclerView.Adapter<ChannelListSelectionAdapter.ViewHolder>() {

    private val context: WeakReference<Context> = WeakReference(context)
    private var callback: Callback? = null

    interface Callback {
        fun onItemClicked(channel: Channel)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.channel_list_selection_dialog_adapter, parent, false)
        return ViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channelList[position]
        holder.itemView.tag = channel
        val context = this.context.get()
        if (context != null && holder.iconImageView != null && !TextUtils.isEmpty(channel.icon)) {
            Glide.with(context)
                    .load(getIconUrl(context, channel.icon))
                    .into(holder.iconImageView)
        }
        if (holder.titleTextView != null) {
            holder.titleTextView.text = channel.name
            holder.titleTextView.tag = position
        }
    }

    override fun getItemCount(): Int {
        return channelList.size
    }

    class ViewHolder internal constructor(view: View, private val channelListAdapter: ChannelListSelectionAdapter?) : RecyclerView.ViewHolder(view), View.OnClickListener {
        internal val iconImageView: ImageView? = view.findViewById(R.id.icon)
        internal val titleTextView: TextView? = view.findViewById(R.id.title)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (channelListAdapter?.callback != null) {
                val channel = channelListAdapter.channelList[adapterPosition]
                channelListAdapter.callback!!.onItemClicked(channel)
            }
        }
    }
}
