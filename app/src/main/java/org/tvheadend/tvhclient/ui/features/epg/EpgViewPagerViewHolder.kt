package org.tvheadend.tvhclient.ui.features.epg

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import timber.log.Timber
import java.util.concurrent.Executors

class EpgViewPagerViewHolder internal constructor(private val activity: FragmentActivity, view: View, pixelsPerMinute: Float, private val startTime: Long, private val endTime: Long, viewPool: RecyclerView.RecycledViewPool) : RecyclerView.ViewHolder(view) {

    private val recyclerViewAdapter: EpgProgramListRecyclerViewAdapter
    private val viewModel: EpgViewModel
    private val execService = Executors.newScheduledThreadPool(10)

    @BindView(R.id.program_list_recycler_view)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.progress_bar)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.no_programs)
    lateinit var noProgramsTextView: TextView

    init {
        ButterKnife.bind(this, view)

        recyclerView.layoutManager = CustomHorizontalLayoutManager(view.context)
        recyclerView.addItemDecoration(DividerItemDecoration(view.context, LinearLayoutManager.HORIZONTAL))
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.setRecycledViewPool(viewPool)
        recyclerViewAdapter = EpgProgramListRecyclerViewAdapter(pixelsPerMinute, startTime, endTime)
        recyclerView.adapter = recyclerViewAdapter

        viewModel = ViewModelProviders.of(activity).get(EpgViewModel::class.java)
    }

    fun bindData(epgChannel: EpgChannel) {

        recyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        noProgramsTextView.visibility = View.GONE

        execService.execute {
            val programs = viewModel.getProgramsByChannelAndBetweenTimeSync(epgChannel.id, startTime, endTime)
            if (programs.isNotEmpty()) {
                Timber.d("Loaded ${programs.size} programs for channel ${epgChannel.name}")
                activity.runOnUiThread {
                    recyclerViewAdapter.addItems(programs.toMutableList())
                    recyclerView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    noProgramsTextView.visibility = View.GONE
                }
            } else {
                Timber.d("Loaded no programs for channel ${epgChannel.name}")
                activity.runOnUiThread {
                    recyclerView.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    noProgramsTextView.visibility = View.VISIBLE
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
