package org.tvheadend.tvhclient.ui.features.epg

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

internal class EpgViewPagerRecyclerViewAdapter(private val activity: FragmentActivity, private val epgViewModel: EpgViewModel, private val fragmentId: Int) : RecyclerView.Adapter<EpgViewPagerViewHolder>(), Filterable {

    private val viewPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()
    private val channelList = ArrayList<EpgChannel>()
    private var channelListFiltered: MutableList<EpgChannel> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return EpgViewPagerViewHolder(view, activity, epgViewModel, fragmentId, viewPool)
    }

    override fun onBindViewHolder(holder: EpgViewPagerViewHolder, position: Int) {
        val epgChannel = channelListFiltered[position]
        holder.bindData(epgChannel)
    }

    override fun getItemCount(): Int {
        return channelListFiltered.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_program_list_adapter
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                channelListFiltered = if (charString.isEmpty()) {
                    channelList
                } else {
                    val filteredList = ArrayList<EpgChannel>()
                    // Iterate over the available channels. Use a copy on write
                    // array in case the channel list changes during filtering.
                    for (channel in CopyOnWriteArrayList(channelList)) {
                        if (channel.name != null && channel.name!!.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(channel)
                        }
                    }
                    filteredList
                }

                val filterResults = FilterResults()
                filterResults.values = channelListFiltered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                @Suppress("UNCHECKED_CAST")
                channelListFiltered = filterResults.values as ArrayList<EpgChannel>
                notifyDataSetChanged()
            }
        }
    }

    fun addItems(channels: List<EpgChannel>) {
        channelList.clear()
        channelListFiltered.clear()
        channelList.addAll(channels)
        channelListFiltered.addAll(channels)
        notifyDataSetChanged()
    }
}
