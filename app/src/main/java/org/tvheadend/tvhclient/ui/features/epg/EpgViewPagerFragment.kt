package org.tvheadend.tvhclient.ui.features.epg

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
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

    private var updateViewHandler = Handler(Looper.getMainLooper())
    private var updateViewTask: Runnable? = null
    private var updateTimeIndicationHandler = Handler(Looper.getMainLooper())
    private var updateTimeIndicationTask: Runnable? = null
    private lateinit var constraintSet: ConstraintSet
    private lateinit var binding: EpgViewpagerFragmentBinding
    private var recyclerViewLinearLayoutManager: LinearLayoutManager? = null
    private var enableScrolling = false
    private var fragmentId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.epg_viewpager_fragment, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (showTimeIndication) {
            updateViewTask?.let {
                updateViewHandler.removeCallbacks(it)
            }
            updateTimeIndicationTask?.let {
                updateTimeIndicationHandler.removeCallbacks(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Initializing")
        epgViewModel = ViewModelProvider(requireActivity())[EpgViewModel::class.java]

        // Required to show the vertical current time indication
        constraintSet = ConstraintSet()
        constraintSet.clone(binding.constraintLayout)

        // Get the id that defines the position of the fragment in the viewpager
        fragmentId = arguments?.getInt("fragmentId") ?: 0

        binding.startTime = epgViewModel.getStartTime(fragmentId)
        binding.endTime = epgViewModel.getEndTime(fragmentId)

        recyclerViewAdapter = EpgVerticalRecyclerViewAdapter(requireActivity(), epgViewModel, fragmentId, viewLifecycleOwner)
        recyclerViewLinearLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.viewpagerRecyclerView.layoutManager = recyclerViewLinearLayoutManager
        binding.viewpagerRecyclerView.setHasFixedSize(true)
        binding.viewpagerRecyclerView.adapter = recyclerViewAdapter

        binding.viewpagerRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                        val childView = recyclerViewLinearLayoutManager?.getChildAt(0)
                        val offset = if (childView == null) 0 else childView.top - recyclerView.paddingTop
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
        epgViewModel.viewAndEpgDataIsInvalid.observe(viewLifecycleOwner, { reload ->
            Timber.d("Trigger to reload epg data has changed to $reload")
            if (reload) {
                recyclerViewAdapter.loadProgramData()
            }
        })

        binding.currentTime.visibleOrGone(showTimeIndication)

        if (showTimeIndication) {
            // Create the handler and the timer task that will update the
            // entire view every 30 minutes if the first screen is visible.
            // This prevents the time indication from moving to far to the right
            updateViewTask = object : Runnable {
                override fun run() {
                    recyclerViewAdapter.notifyDataSetChanged()
                    updateViewHandler.postDelayed(this, 1200000)
                }
            }
            // Create the handler and the timer task that will update the current
            // time indication every minute.
            updateTimeIndicationTask = object : Runnable {
                override fun run() {
                    setCurrentTimeIndication()
                    updateTimeIndicationHandler.postDelayed(this, 60000)
                }
            }
            updateViewTask?.let {
                updateViewHandler.postDelayed(it, 60000)
            }
            updateTimeIndicationTask?.let {
                updateTimeIndicationHandler.post(it)
            }
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
        binding.currentTime.let {
            constraintSet.connect(it.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, offset)
            constraintSet.connect(it.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, offset)
            constraintSet.applyTo(binding.constraintLayout)
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
