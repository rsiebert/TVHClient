package org.tvheadend.tvhclient.ui.epg;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.service.HTSListener;
import org.tvheadend.tvhclient.ui.epg.ProgramGuideViewPagerContentListAdapterContentsView.ProgramContextMenuInterface;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.UIUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ProgramGuideViewPagerContentListFragment extends ListFragment implements HTSListener, ProgramGuideControlInterface, ProgramContextMenuInterface, OnScrollListener {

    @BindView(R.id.constraint_layout)
    ConstraintLayout constraintLayout;
    @BindView(R.id.pager_title_date)
    TextView titleDate;
    @BindView(R.id.pager_title_hours)
    TextView titleHours;
    @Nullable
    @BindView(R.id.current_time)
    ImageView currentTimeIndication;

    private FragmentActivity activity;
    private ProgramGuideScrollInterface programGuideScrollInterface;
    private ProgramGuideViewPagerContentListAdapter adapter;
    private Program selectedProgram = null;
    private boolean showTimeIndication;

    private Handler updateViewHandler;
    private Runnable updateViewTask;
    private Handler updateTimeIndicationHandler;
    private Runnable updateTimeIndicationTask;

    // Enables scrolling when the user has touch the screen and starts
    // scrolling. When the user is done, scrolling will be disabled to prevent
    // unwanted calls to the interface. 
    private boolean enableScrolling = false;

    private MenuUtils menuUtils;
    private Unbinder unbinder;
    private long startTime;
    private long endTime;
    private int displayWidth;
    private float pixelsPerMinute;
    private String TAG = ProgramGuideViewPagerContentListFragment.class.getSimpleName();
    private ConstraintSet constraintSet;

    public static ProgramGuideViewPagerContentListFragment newInstance(Long startTime, Long endTime, boolean showTimeIndication, int displayWidth, float pixelsPerMinute) {
        ProgramGuideViewPagerContentListFragment fragment = new ProgramGuideViewPagerContentListFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("epgStartTime", startTime);
        bundle.putLong("epgEndTime", endTime);
        // Used to only show the vertical current time indication in the first fragment
        bundle.putBoolean("showTimeIndication", showTimeIndication);
        // Required to know how many pixels of the display are remaining
        bundle.putInt("displayWidth", displayWidth);
        // Required so that the correct width of each program guide entry
        // can be calculated. Also used to determine the exact position of
        // the vertical current time indication
        bundle.putFloat("pixelsPerMinute", pixelsPerMinute);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.program_guide_viewpager_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() called with: savedInstanceState = [" + savedInstanceState + "]");
        activity = getActivity();
        menuUtils = new MenuUtils(getActivity());

        constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment.isAdded() && fragment instanceof ProgramGuideScrollInterface) {
            programGuideScrollInterface = (ProgramGuideScrollInterface) fragment;
        }

        // Set the date and the time slot hours in the titleTextView of the fragment
        Bundle bundle = getArguments();
        if (bundle != null) {
            startTime = bundle.getLong("epgStartTime", 0);
            endTime = bundle.getLong("epgEndTime", 0);
            displayWidth = bundle.getInt("displayWidth");
            pixelsPerMinute = bundle.getFloat("pixelsPerMinute");
            showTimeIndication = bundle.getBoolean("showTimeIndication", false);
        }

        titleDate.setText(UIUtils.getDate(activity, startTime));

        String time = UIUtils.getTimeText(activity, startTime) + " - " + UIUtils.getTimeText(activity, endTime);
        titleHours.setText(time);

        adapter = new ProgramGuideViewPagerContentListAdapter(activity, this, new ArrayList<>(), startTime, endTime, displayWidth, pixelsPerMinute);
        setListAdapter(adapter);
        getListView().setVerticalScrollBarEnabled(false);
        getListView().setOnScrollListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setItemsCanFocus(true);

        // Create the handler and the timer task that will update the
        // entire view every 30 minutes if the first screen is visible.
        // This prevents the time indication from moving to far to the right
        updateViewHandler = new Handler();
        updateViewTask = new Runnable() {
            public void run() {
                updateViewHandler.postDelayed(this, 1200000);
                if (showTimeIndication) {
                    adapter.notifyDataSetChanged();
                }
            }
        };

        // Create the handler and the timer task that will update the current
        // time indication every minute.
        updateTimeIndicationHandler = new Handler();
        updateTimeIndicationTask = new Runnable() {
            public void run() {
                updateTimeIndicationHandler.postDelayed(this, 60000);
                setCurrentTimeIndication();
            }
        };

        updateTimeIndicationHandler.post(updateTimeIndicationTask);
        updateViewHandler.post(updateViewTask);

        Log.d(TAG, "onActivityCreated() returned: ");
    }

    /**
     * Fills the list with the available program guide data from the available
     * channels. Only the programs of those channels will be added to the
     * adapter that contain the selected channel tag.
     */
    /*
    private void populateList() {
        ChannelTag currentTag = Utils.getChannelTag(activity);
        adapter.clear();

        // Make a copy of the channel list before iterating over it
        adapter.clear();
        for (Channel channel : DataStorage.getInstance().getChannelsFromArray().values()) {
            if (currentTag == null || channel.getTags().contains(currentTag.getTagId())) {
                adapter.add(channel);
            }
        }
        adapter.sort(Utils.getChannelSortOrder(activity));
        adapter.notifyDataSetChanged();
    }
*/
    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
        if (!DataStorage.getInstance().isLoading()) {
            //populateList();
        }
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * This line is only visible in the first screen where the current time is
     * shown. This method is called every minute.
     */
    private void setCurrentTimeIndication() {
        if (currentTimeIndication != null && activity != null) {
            if (showTimeIndication) {
                // Get the difference between the current time and the given
                // start time. Calculate from this value in minutes the width in
                // pixels. This will be horizontal offset for the time
                // indication. If channel icons are shown then we need to add a
                // the icon width to the offset.
                final long currentTime = Calendar.getInstance().getTimeInMillis();
                final long durationTime = (currentTime - startTime) / 1000 / 60;
                final int offset = (int) (durationTime * pixelsPerMinute);

                // Set the left constraint of the time indication so it shows the actual time
                constraintSet.connect(currentTimeIndication.getId(), ConstraintSet.LEFT, constraintLayout.getId(), ConstraintSet.LEFT, offset);
                constraintSet.connect(currentTimeIndication.getId(), ConstraintSet.START, constraintLayout.getId(), ConstraintSet.START, offset);
                constraintSet.applyTo(constraintLayout);

                currentTimeIndication.setVisibility(View.VISIBLE);
            } else {
                currentTimeIndication.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO causes null pointer
        updateViewHandler.removeCallbacks(updateViewTask);
        updateTimeIndicationHandler.removeCallbacks(updateTimeIndicationTask);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (selectedProgram == null) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_search_imdb:
                menuUtils.handleMenuSearchWebSelection(selectedProgram.getTitle());
                return true;
            case R.id.menu_search_epg:
                menuUtils.handleMenuSearchEpgSelection(selectedProgram.getTitle());
                return true;
            case R.id.menu_record_remove:
                Recording rec = DataStorage.getInstance().getRecordingFromArray(selectedProgram.getDvrId());
                if (rec != null) {
                    if (rec.isRecording()) {
                        menuUtils.handleMenuStopRecordingSelection(rec.getId(), rec.getTitle());
                    } else if (rec.isScheduled()) {
                        menuUtils.handleMenuCancelRecordingSelection(rec.getId(), rec.getTitle(), null);
                    } else {
                        menuUtils.handleMenuRemoveRecordingSelection(rec.getId(), rec.getTitle(), null);
                    }
                }
                return true;
            case R.id.menu_record_once:
                menuUtils.handleMenuRecordSelection(selectedProgram.getEventId());
                return true;
            case R.id.menu_record_series:
                menuUtils.handleMenuSeriesRecordSelection(selectedProgram.getTitle());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case "channelAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.add((Channel) obj);
                        adapter.sort(Utils.getChannelSortOrder(activity));
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "channelUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        final Channel ch = (Channel) obj;
                        adapter.update(ch);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "channelDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.remove((Channel) obj);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "eventUpdate":
            case "eventDelete":
            case "dvrEntryAdd":
            case "dvrEntryUpdate":
                // An existing program has been updated
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

    @Override
    public void setSelectedContextItem(Program p) {
        selectedProgram = p;
    }

    @Override
    public void setMenuSelection(MenuItem item) {
        onContextItemSelected(item);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void reloadData() {
        //populateList();
    }

    @Override
    public void setSelection(final int position, final int offset) {
        if (getListView() != null && getListView().getCount() > position && position >= 0) {
            getListView().setSelectionFromTop(position, offset);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
            enableScrolling = true;
        } else if (scrollState == SCROLL_STATE_IDLE && enableScrolling) {
            if (programGuideScrollInterface != null) {
                enableScrolling = false;
                programGuideScrollInterface.onScrollStateChanged();
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (programGuideScrollInterface != null && enableScrolling) {
            int position = view.getFirstVisiblePosition();
            View v = view.getChildAt(0);
            int offset = (v == null) ? 0 : v.getTop();
            programGuideScrollInterface.onScroll(position, offset);
        }
    }
}
