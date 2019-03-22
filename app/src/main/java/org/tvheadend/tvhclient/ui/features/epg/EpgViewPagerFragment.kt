package org.tvheadend.tvhclient.ui.features.epg

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.EpgViewpagerFragmentBinding
import javax.inject.Inject

class EpgViewPagerFragment : Fragment(), EpgScrollInterface {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: EpgViewPagerRecyclerViewAdapter
    private var currentTimeIndication: ImageView? = null

    private var showTimeIndication: Boolean = false
    private var pixelsPerMinute: Float = 0f

    private lateinit var updateViewHandler: Handler
    private lateinit var updateViewTask: Runnable
    private lateinit var updateTimeIndicationHandler: Handler
    private lateinit var updateTimeIndicationTask: Runnable
    private lateinit var constraintSet: ConstraintSet
    private lateinit var recyclerViewLinearLayoutManager: LinearLayoutManager
    private lateinit var itemBinding: EpgViewpagerFragmentBinding
    private var enableScrolling: Boolean = false
    private var fragmentId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.epg_viewpager_fragment, container, false)
        val view = itemBinding.root

        constraintLayout = view.findViewById(R.id.constraint_layout)
        recyclerView = view.findViewById(R.id.viewpager_recycler_view)
        currentTimeIndication = view.findViewById(R.id.current_time)
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        if (showTimeIndication) {
            updateViewHandler.removeCallbacks(updateViewTask)
            updateTimeIndicationHandler.removeCallbacks(updateTimeIndicationTask)
        }
    }

    private lateinit var viewModel: EpgViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        MainApplication.getComponent().inject(this)

        viewModel = ViewModelProviders.of(activity as AppCompatActivity).get(EpgViewModel::class.java)

        // Required to show the vertical current time indication
        constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        // Get the id that defines the position of the fragment in the viewpager
        fragmentId = arguments?.getInt("fragmentId") ?: 0
        showTimeIndication = fragmentId == 0

        itemBinding.startTime = viewModel.startTimes[fragmentId]
        itemBinding.endTime = viewModel.endTimes[fragmentId]

        // Calculates the available display width of one minute in pixels. This depends
        // how wide the screen is and how many hours shall be shown in one screen.
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val displayWidth = displayMetrics.widthPixels

        pixelsPerMinute = (displayWidth - 221).toFloat() / (60.0f * viewModel.hoursToShow.toFloat())

        recyclerViewAdapter = EpgViewPagerRecyclerViewAdapter(requireActivity(), pixelsPerMinute, viewModel.startTimes[fragmentId], viewModel.endTimes[fragmentId])
        recyclerViewLinearLayoutManager = LinearLayoutManager(appContext, RecyclerView.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(appContext, LinearLayoutManager.VERTICAL))
        recyclerView.layoutManager = recyclerViewLinearLayoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = recyclerViewAdapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                        val position = recyclerViewLinearLayoutManager.findFirstVisibleItemPosition()
                        val view = recyclerViewLinearLayoutManager.getChildAt(0)
                        val offset = if (view == null) 0 else view.top - recyclerView.paddingTop
                        val fragment = it.supportFragmentManager.findFragmentById(R.id.main)
                        if (fragment is EpgScrollInterface) {
                            (fragment as EpgScrollInterface).onScroll(position, offset)
                        }
                    }
                }
            }
        })
        activity?.let {
            val viewModel = ViewModelProviders.of(it).get(EpgViewModel::class.java)
            viewModel.epgChannels.observe(viewLifecycleOwner, Observer { channels ->
                if (channels != null) {
                    recyclerViewAdapter.addItems(channels)
                }
            })
        }

        currentTimeIndication?.visibility = if (showTimeIndication) View.VISIBLE else View.GONE

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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("layout", recyclerViewLinearLayoutManager.onSaveInstanceState())
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
        val currentTime = System.currentTimeMillis()
        val durationTime = (currentTime - viewModel.startTimes[fragmentId]) / 1000 / 60
        val offset = (durationTime * pixelsPerMinute).toInt()

        // Set the left constraint of the time indication so it shows the actual time
        currentTimeIndication?.let {
            constraintSet.connect(it.id, ConstraintSet.LEFT, constraintLayout.id, ConstraintSet.LEFT, offset)
            constraintSet.connect(it.id, ConstraintSet.START, constraintLayout.id, ConstraintSet.START, offset)
            constraintSet.applyTo(constraintLayout)
        }
    }

    override fun onScroll(position: Int, offset: Int) {
        recyclerViewLinearLayoutManager.scrollToPositionWithOffset(position, offset)
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
