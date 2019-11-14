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
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.invisible
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors

internal class EpgVerticalRecyclerViewAdapter(private val activity: FragmentActivity, private val epgViewModel: EpgViewModel, private val fragmentId: Int) : RecyclerView.Adapter<EpgVerticalRecyclerViewAdapter.EpgViewPagerViewHolder>() {

    private val viewPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()
    private val channelList = ArrayList<EpgChannel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return EpgViewPagerViewHolder(view, activity, fragmentId, viewPool, epgViewModel)
    }

    override fun onBindViewHolder(holder: EpgViewPagerViewHolder, position: Int) {
        val epgChannel = channelList[position]
        holder.bindData(epgChannel)
    }

    override fun getItemCount(): Int {
        return channelList.size
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.epg_vertical_recyclerview_adapter
    }

    fun addItems(channels: List<EpgChannel>) {
        channelList.clear()
        channelList.addAll(channels)
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
                                 private val fragmentId: Int,
                                 viewPool: RecyclerView.RecycledViewPool,
                                 private val epgViewModel: EpgViewModel) : RecyclerView.ViewHolder(containerView), LayoutContainer, LifecycleOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val recyclerViewAdapter: EpgHorizontalChildRecyclerViewAdapter
        private val execService = Executors.newScheduledThreadPool(10)

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

        fun bindData(epgChannel: EpgChannel) {

            horizontal_child_recycler_view.gone()
            progress_bar.visible()
            no_programs.gone()

            execService.execute {
                val programs = epgViewModel.getProgramsByChannelAndBetweenTimeSync(epgChannel.id, fragmentId)
                if (programs.isNotEmpty()) {
                    Timber.d("Loaded ${programs.size} programs for channel ${epgChannel.name}")
                    activity.runOnUiThread {
                        recyclerViewAdapter.addItems(programs.toMutableList())
                        horizontal_child_recycler_view.visible()
                        progress_bar.gone()
                        no_programs.invisible()
                    }
                } else {
                    Timber.d("Loaded no programs for channel ${epgChannel.name}")
                    activity.runOnUiThread {
                        horizontal_child_recycler_view.invisible()
                        progress_bar.gone()
                        no_programs.visible()
                    }
                }
            }

            epgViewModel.getRecordingsByChannel(epgChannel.id).observe(activity, androidx.lifecycle.Observer { recordings ->
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
