package org.tvheadend.tvhclient.features.epg;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.features.shared.UIUtils;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class EpgViewPagerFragment extends Fragment {

    @BindView(R.id.constraint_layout)
    ConstraintLayout constraintLayout;
    @BindView(R.id.viewpager_title_date)
    TextView titleDate;
    @BindView(R.id.viewpager_title_hours)
    TextView titleHours;
    @BindView(R.id.viewpager_recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.current_time)
    ImageView currentTimeIndication;

    private Unbinder unbinder;
    private FragmentActivity activity;
    private EpgViewPagerRecyclerViewAdapter recyclerViewAdapter;
    private EpgViewModel viewModel;

    private boolean showTimeIndication;
    private long startTime;
    private long endTime;
    private int displayWidth;
    private float pixelsPerMinute;

    private Handler updateViewHandler;
    private Runnable updateViewTask;
    private Handler updateTimeIndicationHandler;
    private Runnable updateTimeIndicationTask;
    private ConstraintSet constraintSet;


    public static EpgViewPagerFragment newInstance(Long startTime, Long endTime, float pixelsPerMinute, int displayWidth, boolean timeIndicationEnabled) {
        EpgViewPagerFragment fragment = new EpgViewPagerFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("epg_start_time", startTime);
        bundle.putLong("epg_end_time", endTime);
        // Required so that the correct width of each program guide entry
        // can be calculated. Also used to determine the exact position of
        // the vertical current time indication
        bundle.putInt("display_width", displayWidth);
        // Required to know how many pixels of the display are remaining
        bundle.putFloat("pixels_per_minute", pixelsPerMinute);
        // Used to only show the vertical current time indication in the first fragment
        bundle.putBoolean("time_indication_enabled", timeIndicationEnabled);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.epg_viewpager_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (showTimeIndication) {
            updateViewHandler.removeCallbacks(updateViewTask);
            updateTimeIndicationHandler.removeCallbacks(updateTimeIndicationTask);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();

        constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        Bundle bundle = getArguments();
        if (bundle != null) {
            startTime = bundle.getLong("epg_start_time", 0);
            endTime = bundle.getLong("epg_end_time", 0);
            displayWidth = bundle.getInt("display_width");
            pixelsPerMinute = bundle.getFloat("pixels_per_minute");
            showTimeIndication = bundle.getBoolean("time_indication_enabled", false);
        }

        String date = UIUtils.getDate(activity, startTime);
        String time = UIUtils.getTimeText(activity, startTime) + " - " + UIUtils.getTimeText(activity, endTime);
        titleDate.setText(date);
        titleHours.setText(time);

        recyclerViewAdapter = new EpgViewPagerRecyclerViewAdapter(activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(recyclerViewAdapter);

        viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);
        viewModel.getChannels().observe(this, channels -> {
            Timber.d("Loaded channels in view pager fragment");
            if (channels != null) {
                int position = 0;
                for (ChannelSubset channel : channels) {
                    position++;
                    loadPrograms(position, channel);
                }
            }
        });

        currentTimeIndication.setVisibility(showTimeIndication ? View.VISIBLE : View.GONE);
        if (showTimeIndication) {
            // Create the handler and the timer task that will update the
            // entire view every 30 minutes if the first screen is visible.
            // This prevents the time indication from moving to far to the right
            updateViewHandler = new Handler();
            updateViewTask = new Runnable() {
                public void run() {
                    recyclerViewAdapter.notifyDataSetChanged();
                    updateViewHandler.postDelayed(this, 1200000);
                }
            };
            // Create the handler and the timer task that will update the current
            // time indication every minute.
            updateTimeIndicationHandler = new Handler();
            updateTimeIndicationTask = new Runnable() {
                public void run() {
                    setCurrentTimeIndication();
                    updateTimeIndicationHandler.postDelayed(this, 60000);
                }
            };
            updateViewHandler.postDelayed(updateViewTask, 60000);
            updateTimeIndicationHandler.post(updateTimeIndicationTask);
        }
    }

    private void loadPrograms(int position, ChannelSubset channel) {
        viewModel.getProgramsByChannelAndBetweenTime(channel.getId(), startTime, endTime).observe(this, programs -> {
            recyclerViewAdapter.addItems(position, channel, programs);
        });
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * This line is only visible in the first screen where the current time is
     * shown. This method is called every minute.
     */
    private void setCurrentTimeIndication() {
        // Get the difference between the current time and the given start time. Calculate
        // from this value in minutes the width in pixels. This will be horizontal offset
        // for the time indication. If channel icons are shown then we need to add a
        // the icon width to the offset.
        final long currentTime = Calendar.getInstance().getTimeInMillis();
        final long durationTime = (currentTime - startTime) / 1000 / 60;
        final int offset = (int) (durationTime * pixelsPerMinute);

        // Set the left constraint of the time indication so it shows the actual time
        constraintSet.connect(currentTimeIndication.getId(), ConstraintSet.LEFT, constraintLayout.getId(), ConstraintSet.LEFT, offset);
        constraintSet.connect(currentTimeIndication.getId(), ConstraintSet.START, constraintLayout.getId(), ConstraintSet.START, offset);
        constraintSet.applyTo(constraintLayout);
    }
}
