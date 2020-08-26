package org.tvheadend.tvhclient.ui.features.epg

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.epg_vertical_recyclerview_adapter.*
import org.tvheadend.data.entity.EpgChannel
import org.tvheadend.data.entity.EpgProgram
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.invisible
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber

internal class EpgVerticalRecyclerViewAdapter(private val activity: FragmentActivity, private val epgViewModel: EpgViewModel, private val fragmentId: Int) : RecyclerView.Adapter<EpgVerticalRecyclerViewAdapter.EpgViewPagerViewHolder>() {

    private val viewPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()
    private var channelList = ArrayList<EpgChannel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return EpgViewPagerViewHolder(view, activity, fragmentId, viewPool, epgViewModel)
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

    override fun onViewAttachedToWindow(holder: EpgViewPagerViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    override fun onViewDetachedFromWindow(holder: EpgViewPagerViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    class EpgViewPagerViewHolder(override val containerView: View,
                                 private val activity: FragmentActivity,
                                 fragmentId: Int,
                                 viewPool: RecyclerView.RecycledViewPool,
                                 private val epgViewModel: EpgViewModel) : RecyclerView.ViewHolder(containerView), LayoutContainer, LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val recyclerViewAdapter: EpgHorizontalChildRecyclerViewAdapter

        init {
            horizontal_child_recycler_view.layoutManager = CustomHorizontalLayoutManager(containerView.context)
            horizontal_child_recycler_view.setRecycledViewPool(viewPool)
            recyclerViewAdapter = EpgHorizontalChildRecyclerViewAdapter(epgViewModel, fragmentId)
            horizontal_child_recycler_view.adapter = recyclerViewAdapter
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

        fun bindData(programs: List<EpgProgram>) {

            horizontal_child_recycler_view.gone()
            progress_bar.visible()
            no_programs.gone()

            if (programs.isNotEmpty()) {
                recyclerViewAdapter.addItems(programs.toMutableList())
                horizontal_child_recycler_view.visible()
                progress_bar.gone()
                no_programs.invisible()
            } else {
                horizontal_child_recycler_view.invisible()
                progress_bar.gone()
                no_programs.visible()
            }

            epgViewModel.recordings.observe(activity, { recordings ->
                if (recordings != null) {
                    recyclerViewAdapter.addRecordings(recordings)
                }
            })
        }

        internal class CustomHorizontalLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }
    }
}
