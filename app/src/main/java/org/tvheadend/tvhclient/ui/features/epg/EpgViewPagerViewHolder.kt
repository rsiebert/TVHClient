package org.tvheadend.tvhclient.ui.features.epg

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.epg_program_list_adapter.*
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import org.tvheadend.tvhclient.ui.common.gone
import org.tvheadend.tvhclient.ui.common.visible
import timber.log.Timber
import java.util.concurrent.Executors

class EpgViewPagerViewHolder(override val containerView: View, private val activity: FragmentActivity, private val epgViewModel: EpgViewModel, private val fragmentId: Int, viewPool: RecyclerView.RecycledViewPool) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private val recyclerViewAdapter: EpgProgramListRecyclerViewAdapter
    private val viewModel: EpgViewModel = ViewModelProviders.of(activity).get(EpgViewModel::class.java)
    private val execService = Executors.newScheduledThreadPool(10)

    init {
        program_list_recycler_view.layoutManager = CustomHorizontalLayoutManager(containerView.context)
        program_list_recycler_view.addItemDecoration(DividerItemDecoration(containerView.context, LinearLayoutManager.HORIZONTAL))
        program_list_recycler_view.itemAnimator = DefaultItemAnimator()
        program_list_recycler_view.setRecycledViewPool(viewPool)
        recyclerViewAdapter = EpgProgramListRecyclerViewAdapter(viewModel, fragmentId)
        program_list_recycler_view.adapter = recyclerViewAdapter
    }

    fun bindData(epgChannel: EpgChannel) {

        program_list_recycler_view.gone()
        progress_bar.visible()
        no_programs.gone()

        execService.execute {
            val programs = viewModel.getProgramsByChannelAndBetweenTimeSync(epgChannel.id, fragmentId)
            if (programs.isNotEmpty()) {
                Timber.d("Loaded ${programs.size} programs for channel ${epgChannel.name}")
                activity.runOnUiThread {
                    recyclerViewAdapter.addItems(programs.toMutableList())
                    program_list_recycler_view.visible()
                    progress_bar.gone()
                    no_programs.gone()
                }
            } else {
                Timber.d("Loaded no programs for channel ${epgChannel.name}")
                activity.runOnUiThread {
                    program_list_recycler_view.gone()
                    progress_bar.gone()
                    no_programs.visible()
                }
            }
        }

        viewModel.getRecordingsByChannel(epgChannel.id).observe(activity, Observer { recordings ->
            if (recordings != null) {
                recyclerViewAdapter.addRecordings(recordings)
            }
        })
    }
}
