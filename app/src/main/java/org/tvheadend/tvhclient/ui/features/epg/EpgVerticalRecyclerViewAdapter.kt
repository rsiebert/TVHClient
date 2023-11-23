package org.tvheadend.tvhclient.ui.features.epg

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.tvheadend.data.entity.EpgChannel
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgVerticalRecyclerviewAdapterBinding
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.invisible
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

internal class EpgVerticalRecyclerViewAdapter(private val activity: FragmentActivity, private val epgViewModel: EpgViewModel, private val fragmentId: Int, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<EpgVerticalRecyclerViewAdapter.EpgViewPagerViewHolder>() {

    private val viewPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()
    private var channelList = ArrayList<EpgChannel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgViewPagerViewHolder {
        val binding = EpgVerticalRecyclerviewAdapterBinding.inflate(LayoutInflater.from(parent.context))
        return EpgViewPagerViewHolder(binding.root, binding, activity, fragmentId, viewPool, epgViewModel, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: EpgViewPagerViewHolder, position: Int) {
        val epgChannel = channelList[position]
        val programs = epgViewModel.getProgramsByChannelAndBetweenTimeSync(epgChannel.id, fragmentId)
        Timber.d("Binding ${programs.size} programs for channel ${epgChannel.name} in viewpager fragment $fragmentId")
        holder.bindData(programs)
    }

    override fun getItemCount(): Int {
        return channelList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_vertical_recyclerview_adapter
    }

    fun loadProgramData() {
        Timber.d("Loading programs for viewpager fragment $fragmentId")
        channelList.clear()
        channelList.addAll(epgViewModel.epgChannels.value ?: ArrayList())
        notifyDataSetChanged()
    }

    class EpgViewPagerViewHolder(override val containerView: View,
                                 val binding: EpgVerticalRecyclerviewAdapterBinding,
                                 private val activity: FragmentActivity,
                                 fragmentId: Int,
                                 viewPool: RecyclerView.RecycledViewPool,
                                 private val epgViewModel: EpgViewModel,
                                 lifecycleOwner: LifecycleOwner) : RecyclerView.ViewHolder(binding.root), LayoutContainer {

        private val recyclerViewAdapter: EpgHorizontalChildRecyclerViewAdapter

        init {
            binding.horizontalChildRecyclerView.layoutManager = CustomHorizontalLayoutManager(containerView.context)
            binding.horizontalChildRecyclerView.setRecycledViewPool(viewPool)
            recyclerViewAdapter = EpgHorizontalChildRecyclerViewAdapter(epgViewModel, fragmentId, lifecycleOwner)
            binding.horizontalChildRecyclerView.adapter = recyclerViewAdapter
        }

        fun bindData(programs: List<EpgProgram>) {

            binding.horizontalChildRecyclerView.gone()
            binding.progressBar.visible()
            binding.noPrograms.gone()

            if (programs.isNotEmpty()) {
                recyclerViewAdapter.addItems(programs.toMutableList())
                binding.horizontalChildRecyclerView.visible()
                binding.progressBar.gone()
                binding.noPrograms.invisible()
            } else {
                binding.horizontalChildRecyclerView.invisible()
                binding.progressBar.gone()
                binding.noPrograms.visible()
            }

            epgViewModel.recordings.observe(activity) { recordings ->
                if (recordings != null) {
                    recyclerViewAdapter.addRecordings(recordings)
                }
            }
        }

        internal class CustomHorizontalLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }
    }
}
