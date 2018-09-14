package org.tvheadend.tvhclient.features.epg;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.utils.UIUtils;
import org.tvheadend.tvhclient.features.shared.callbacks.ChannelTagSelectionCallback;

import java.util.Calendar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;

public class EpgViewPagerFragment extends Fragment implements EpgScrollInterface, ChannelTagSelectionCallback {

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

    @Inject
    SharedPreferences sharedPreferences;

    private Unbinder unbinder;
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

        String date = UIUtils.getDate(activity, startTime);
        String time = UIUtils.getTimeText(activity, startTime) + " - " + UIUtils.getTimeText(activity, endTime);
        titleDate.setText(date);
        titleHours.setText(time);

        // Calculates the available display width of one minute in pixels. This depends
        // how wide the screen is and how many hours shall be shown in one screen.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int displayWidth = displaymetrics.widthPixels;
        int hoursToShow = Integer.parseInt(sharedPreferences.getString("hours_of_epg_data_per_screen", "4"));
        pixelsPerMinute = ((float) (displayWidth - 221) / (60.0f * (float) hoursToShow));

        recyclerViewAdapter = new EpgViewPagerRecyclerViewAdapter(activity, pixelsPerMinute, startTime, endTime);
        recyclerViewLinearLayoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(recyclerViewLinearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(activity, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerViewAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != SCROLL_STATE_IDLE) {
                    enableScrolling = true;
                } else if (enableScrolling) {
                    enableScrolling = false;
                    Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.main);
                    if (fragment != null && fragment instanceof EpgScrollInterface) {
                        ((EpgScrollInterface) fragment).onScrollStateChanged();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (enableScrolling) {
                    int position = recyclerViewLinearLayoutManager.findFirstVisibleItemPosition();
                    View v = recyclerViewLinearLayoutManager.getChildAt(0);
                    int offset = (v == null) ? 0 : v.getTop() - recyclerView.getPaddingTop();

                    Fragment fragment = activity.getSupportFragmentManager().findFragmentById(R.id.main);
                    if (fragment != null && fragment instanceof EpgScrollInterface) {
                        ((EpgScrollInterface) fragment).onScroll(position, offset);
                    }
                }
            }
        });

        EpgViewModel viewModel = ViewModelProviders.of(activity).get(EpgViewModel.class);
        viewModel.getChannels().observe(this, channels -> {
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
        final long currentTime = Calendar.getInstance().getTimeInMillis();
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

    @Override
    public void onChannelTagIdSelected(int id) {
        // NOP
    }
}
