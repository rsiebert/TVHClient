package org.tvheadend.tvhclient.ui.features.epg

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import kotlinx.android.synthetic.main.epg_viewpager_fragment.*
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgViewpagerFragmentBinding
import org.tvheadend.tvhclient.util.extensions.visibleOrGone
import timber.log.Timber
import java.util.*

class EpgViewPagerFragment : Fragment(), EpgScrollInterface {

    private lateinit var epgViewModel: EpgViewModel
    private lateinit var recyclerViewAdapter: EpgVerticalRecyclerViewAdapter

    /**
     * Defines if the current time indication (vertical line) shall be shown.
     * The indication shall only be shown for the first fragment.
     */
    private val showTimeIndication: Boolean
        get() {
            return fragmentId == 0
        }

    private lateinit var updateViewHandler: Handler
    private lateinit var updateViewTask: Runnable
    private lateinit var updateTimeIndicationHandler: Handler
    private lateinit var updateTimeIndicationTask: Runnable
    private lateinit var constraintSet: ConstraintSet
    private lateinit var itemBinding: EpgViewpagerFragmentBinding
    private var recyclerViewLinearLayoutManager: LinearLayoutManager? = null
    private var enableScrolling = false
    private var fragmentId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.epg_viewpager_fragment, container, false)
        return itemBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (showTimeIndication) {
            updateViewHandler.removeCallbacks(updateViewTask)
            updateTimeIndicationHandler.removeCallbacks(updateTimeIndicationTask)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("Initializing")
        epgViewModel = ViewModelProviders.of(activity as AppCompatActivity).get(EpgViewModel::class.java)

        // Required to show the vertical current time indication
        constraintSet = ConstraintSet()
        constraintSet.clone(constraint_layout)

        // Get the id that defines the position of the fragment in the viewpager
        fragmentId = arguments?.getInt("fragmentId") ?: 0

        itemBinding.startTime = epgViewModel.getStartTime(fragmentId)
        itemBinding.endTime = epgViewModel.getEndTime(fragmentId)

        recyclerViewAdapter = EpgVerticalRecyclerViewAdapter(requireActivity(), epgViewModel, fragmentId)
        recyclerViewLinearLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        viewpager_recycler_view.layoutManager = recyclerViewLinearLayoutManager
        viewpager_recycler_view.setHasFixedSize(true)
        viewpager_recycler_view.adapter = recyclerViewAdapter

        viewpager_recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != SCROLL_STATE_IDLE) {
                    enableScrolling = true
                } else if (enableScrolling) {
                    enableScrolling = false
                    activity?.let {
                        val fragment = it.supportFragmentManager.findFragmentById(R.id.main)
                        if (fragment is EpgScrollInterface) {
                            (fragment as EpgScrollInterface).onScrollStateChanged()
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (enableScrolling) {
                    activity?.let {
                        val position = recyclerViewLinearLayoutManager?.findFirstVisibleItemPosition() ?: -1
                        val view = recyclerViewLinearLayoutManager?.getChildAt(0)
                        val offset = if (view == null) 0 else view.top - recyclerView.paddingTop
                        val fragment = it.supportFragmentManager.findFragmentById(R.id.main)
                        if (fragment is EpgScrollInterface && position >= 0) {
                            (fragment as EpgScrollInterface).onScroll(position, offset)
                        }
                    }
                }
            }
        })

        // In case the channels and hours and days to show have changed invalidate
        // the adapter so that the UI can be updated with the new data
        Timber.d("Observing trigger to reload epg data")
        epgViewModel.viewAndEpgDataIsInvalid.observe(viewLifecycleOwner, Observer { reload ->
            Timber.d("Trigger to reload epg data has changed to $reload")
            if (reload) {
                recyclerViewAdapter.loadProgramData()
            }
        })

        current_time?.visibleOrGone(showTimeIndication)

        if (showTimeIndication) {
            // Create the handler and the timer task that will update the
            // entire view every 30 minutes if the first screen is visible.
            // This prevents the time indication from moving to far to the right
            updateViewHandler = Handler()
            updateViewTask = object : Runnable {
                override fun run() {
                    recyclerViewAdapter.notifyDataSetChanged()
                    updateViewHandler.postDelayed(this, 1200000)
                }
            }
            // Create the handler and the timer task that will update the current
            // time indication every minute.
            updateTimeIndicationHandler = Handler()
            updateTimeIndicationTask = object : Runnable {
                override fun run() {
                    setCurrentTimeIndication()
                    updateTimeIndicationHandler.postDelayed(this, 60000)
                }
            }
            updateViewHandler.postDelayed(updateViewTask, 60000)
            updateTimeIndicationHandler.post(updateTimeIndicationTask)
        }

        // The program data needs to be loaded when the fragment is created
        recyclerViewAdapter.loadProgramData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        recyclerViewLinearLayoutManager?.let {
            outState.putParcelable("layout", it.onSaveInstanceState())
        }
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * It is only visible in the first screen. This method is called every minute.
     */
    private fun setCurrentTimeIndication() {
        // Get the difference between the current time and the given start time. Calculate
        // from this value in minutes the width in pixels. This will be horizontal offset
        // for the time indication. If channel icons are shown then we need to add a
        // the icon width to the offset.
        val currentTime = Calendar.getInstance().timeInMillis
        val durationTime = (currentTime - epgViewModel.getStartTime(fragmentId)) / 1000 / 60
        val offset = (durationTime * epgViewModel.pixelsPerMinute).toInt()
        Timber.d("Fragment id: $fragmentId, current time: $currentTime, start time: ${epgViewModel.getStartTime(fragmentId)}, offset: $offset, durationTime: $durationTime, pixelsPerMinute: ${epgViewModel.pixelsPerMinute}")

        // Set the left constraint of the time indication so it shows the actual time
        current_time?.let {
            constraintSet.connect(it.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, offset)
            constraintSet.connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, offset)
            constraintSet.applyTo(constraint_layout)
        }
    }

    override fun onScroll(position: Int, offset: Int) {
        recyclerViewLinearLayoutManager?.scrollToPositionWithOffset(position, offset)
    }

    override fun onScrollStateChanged() {
        // NOP
    }

    companion object {

        fun newInstance(fragmentId: Int): EpgViewPagerFragment {
            val fragment = EpgViewPagerFragment()
            val bundle = Bundle()
            bundle.putInt("fragmentId", fragmentId)
            fragment.arguments = bundle
            return fragment
        }
    }
}
