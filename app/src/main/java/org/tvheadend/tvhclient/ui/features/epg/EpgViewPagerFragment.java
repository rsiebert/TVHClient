package org.tvheadend.tvhclient.ui.features.epg;

import android.app.SearchManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.databinding.EpgViewpagerFragmentBinding;

import java.util.Calendar;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

public class EpgViewPagerFragment extends Fragment implements EpgScrollInterface {

    private ConstraintLayout constraintLayout;
    private RecyclerView recyclerView;
    private ImageView currentTimeIndication;

    @Inject
    protected SharedPreferences sharedPreferences;

    private EpgViewPagerRecyclerViewAdapter recyclerViewAdapter;

    private boolean showTimeIndication;
    private long startTime;
    private long endTime;
    private float pixelsPerMinute;

    private Handler updateViewHandler;
    private Runnable updateViewTask;
    private Handler updateTimeIndicationHandler;
    private Runnable updateTimeIndicationTask;
    private ConstraintSet constraintSet;
    private boolean enableScrolling;
    private LinearLayoutManager recyclerViewLinearLayoutManager;
    private String searchQuery;
    private FragmentActivity activity;
    private EpgViewpagerFragmentBinding itemBinding;

    public static EpgViewPagerFragment newInstance(Long startTime, Long endTime, boolean timeIndicationEnabled, String searchQuery) {
        EpgViewPagerFragment fragment = new EpgViewPagerFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("epg_start_time", startTime);
        bundle.putLong("epg_end_time", endTime);
        // Used to only show the vertical current time indication in the first fragment
        bundle.putBoolean("time_indication_enabled", timeIndicationEnabled);
        bundle.putString(SearchManager.QUERY, searchQuery);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        itemBinding = DataBindingUtil.inflate(inflater, R.layout.epg_viewpager_fragment, container, false);
        View view = itemBinding.getRoot();

        constraintLayout = view.findViewById(R.id.constraint_layout);
        recyclerView = view.findViewById(R.id.viewpager_recycler_view);
        currentTimeIndication = view.findViewById(R.id.current_time);
        return view;
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
        MainApplication.getComponent().inject(this);
        activity = getActivity();

        constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        Bundle bundle = getArguments();
        if (bundle != null) {
            startTime = bundle.getLong("epg_start_time", 0);
            endTime = bundle.getLong("epg_end_time", 0);
            showTimeIndication = bundle.getBoolean("time_indication_enabled", false);
            searchQuery = bundle.getString(SearchManager.QUERY);
        }

        itemBinding.setStartTime(startTime);
        itemBinding.setEndTime(endTime);
        // Calculates the available display width of one minute in pixels. This depends
        // how wide the screen is and how many hours shall be shown in one screen.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int displayWidth = displaymetrics.widthPixels;

        // The defined value should not be zero due to checking the value
        // in the settings. Check it anyway to prevent a divide by zero.
        //noinspection ConstantConditions
        int hoursToShow = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", getResources().getString(R.string.pref_default_hours_of_epg_data_per_screen)));
        if (hoursToShow == 0) {
            hoursToShow++;
        }
        pixelsPerMinute = ((float) (displayWidth - 221) / (60.0f * (float) hoursToShow));

        recyclerViewAdapter = new EpgViewPagerRecyclerViewAdapter(activity, pixelsPerMinute, startTime, endTime);
        recyclerViewLinearLayoutManager = new LinearLayoutManager(activity.getApplicationContext(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(recyclerViewLinearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(activity.getApplicationContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != SCROLL_STATE_IDLE) {
                    enableScrolling = true;
                } else if (enableScrolling) {
                    enableScrolling = false;
                    Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.main);
                    if (fragment instanceof EpgScrollInterface) {
                        ((EpgScrollInterface) fragment).onScrollStateChanged();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (enableScrolling) {
                    int position = recyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    View v = recyclerViewLinearLayoutManager.getChildAt(0);
                    int offset = (v == null) ? 0 : v.getTop() - recyclerView.getPaddingTop();

                    Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.main);
                    if (fragment instanceof EpgScrollInterface) {
                        ((EpgScrollInterface) fragment).onScroll(position, offset);
                    }
                }
            }
        });

        EpgViewModel viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);
        viewModel.getEpgChannels().observe(getViewLifecycleOwner(), channels -> {
            if (channels != null) {
                recyclerViewAdapter.addItems(channels);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("layout", recyclerViewLinearLayoutManager.onSaveInstanceState());
        outState.putString(SearchManager.QUERY, searchQuery);
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * It is only visible in the first screen. This method is called every minute.
     */
    private void setCurrentTimeIndication() {
        // Get the difference between the current time and the given start time. Calculate
        // from this value in minutes the width in pixels. This will be horizontal offset
        // for the time indication. If channel icons are shown then we need to add a
        // the icon width to the offset.
        final long currentTime = System.currentTimeMillis();
        final long durationTime = (currentTime - startTime) / 1000 / 60;
        final int offset = (int) (durationTime * pixelsPerMinute);

        // Set the left constraint of the time indication so it shows the actual time
        if (currentTimeIndication != null) {
            constraintSet.connect(currentTimeIndication.getId(), ConstraintSet.LEFT, constraintLayout.getId(), ConstraintSet.LEFT, offset);
            constraintSet.connect(currentTimeIndication.getId(), ConstraintSet.START, constraintLayout.getId(), ConstraintSet.START, offset);
            constraintSet.applyTo(constraintLayout);
        }
    }

    @Override
    public void onScroll(int position, int offset) {
        recyclerViewLinearLayoutManager.scrollToPositionWithOffset(position, offset);
    }

    @Override
    public void onScrollStateChanged() {

    }
}
