package org.tvheadend.tvhclient.fragments;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.Logger;
import org.tvheadend.tvhclient.ProgramGuideItemView.ProgramContextMenuInterface;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.ProgramGuideListAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Program;
import org.tvheadend.tvhclient.model.Recording;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ProgramGuideListFragment extends Fragment implements HTSListener, FragmentControlInterface, ProgramContextMenuInterface {

    private final static String TAG = ProgramGuideListFragment.class.getSimpleName();

    private FragmentActivity activity;
    private FragmentStatusInterface fragmentStatusInterface;
    private FragmentScrollInterface fragmentScrollInterface;
    private ProgramGuideListAdapter adapter;
    private ListView listView;
    private LinearLayout titleLayout;
    private TextView titleDateText;
    private TextView titleDate;
    private TextView titleHours;
    private ImageView currentTimeIndication;
    private Bundle bundle;
    private Program selectedProgram = null;
    private int tabIndex;

    private Handler updateEpgHandler;
    private Runnable updateEpgTask;
    private Handler updateViewHandler;
    private Runnable updateViewTask;
    private Handler updateTimeIndicationHandler;
    private Runnable updateTimeIndicationTask;

    // Enables scrolling when the user has touch the screen and starts
    // scrolling. When the user is done, scrolling will be disabled to prevent
    // unwanted calls to the interface. 
    private boolean enableScrolling = false;

    private TVHClientApplication app;
    private Logger logger;
    private DataStorage dataStorage;
    private MenuUtils menuUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.program_guide_pager_list, container, false);
        listView = v.findViewById(R.id.item_list);
        titleLayout = v.findViewById(R.id.pager_title);
        titleDateText = v.findViewById(R.id.pager_title_date_text);
        titleDate = v.findViewById(R.id.pager_title_date);
        titleHours = v.findViewById(R.id.pager_title_hours);
        currentTimeIndication = v.findViewById(R.id.current_time);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        app = TVHClientApplication.getInstance();
        dataStorage = DataStorage.getInstance();
        logger = Logger.getInstance();

        menuUtils = new MenuUtils(getActivity());

        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }
        if (activity instanceof FragmentScrollInterface) {
            fragmentScrollInterface = (FragmentScrollInterface) activity;
        }

        // Set the date and the time slot hours in the title of the fragment
        bundle = getArguments();
        if (bundle != null) {

            // Get the current visible tab index
            tabIndex = bundle.getInt(Constants.BUNDLE_EPG_INDEX, -1);

            final Date startDate = new Date(bundle.getLong(Constants.BUNDLE_EPG_START_TIME, 0));
            final Date endDate = new Date(bundle.getLong(Constants.BUNDLE_EPG_END_TIME, 0));

            // Set the current date and the date as text in the title
            Utils.setDate(titleDateText, startDate.getTime());
            Utils.setDate(titleDate, startDate.getTime());

            // Hide the date text if it shows the date time or the display is too narrow.
            // It is considered too narrow if the width falls below 400 pixels.
            DisplayMetrics displaymetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            if (titleDateText.getText().equals(titleDate.getText()) ||
                    (displaymetrics.widthPixels < 400)) {
                titleDate.setVisibility(View.GONE);
            }

            Utils.setTime(titleHours, startDate.getTime(), endDate.getTime());
        }

        adapter = new ProgramGuideListAdapter(activity, this, new ArrayList<Channel>(), bundle);
        listView.setAdapter(adapter);

        // Inform the activity when the program guide list is scrolling or has
        // finished scrolling. Required to synchronize the scrolling of the
        // program guide list with the channel list.
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    enableScrolling = true;
                } else if (scrollState == SCROLL_STATE_IDLE && enableScrolling) {
                    if (fragmentScrollInterface != null) {
                        enableScrolling = false;
                        fragmentScrollInterface.onScrollStateIdle(TAG);
                    }
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (fragmentScrollInterface != null && enableScrolling) {
                    int position = view.getFirstVisiblePosition();
                    View v = view.getChildAt(0);
                    int offset = (v == null) ? 0 : v.getTop();
                    fragmentScrollInterface.onScrollingChanged(position, offset, TAG);
                }
            }
        });

        // Allow the selection of the items within the list
        listView.setItemsCanFocus(true);

        // Create the handler and the timer task that will update the
        // entire view every 30 minutes if the first screen is visible.
        // This prevents the time indication from moving to far to the right
        updateViewHandler = new Handler();
        updateViewTask = new Runnable() {
            public void run() {
                updateViewHandler.postDelayed(this, 1200000);
                if (tabIndex == 0) {
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

        // This task will be called when the timer to delay the adapter update
        // has expired. It triggers the update of the program guide view. The
        // timer is started in the startDelayedAdapterUpdate method.
        updateEpgHandler = new Handler();
        updateEpgTask = new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
            }
        };

        updateTimeIndicationHandler.post(updateTimeIndicationTask);
        updateViewHandler.post(updateViewTask);
    }

    /**
     * Starts a timer that will update the program guide view when expired. If
     * this method is called while the timer is running, the timer will be
     * restarted. This will prevent calls adapter.notifyDataSetChanged() until
     * all data has been loaded and nothing has happened for 2s.
     */
    private void startDelayedAdapterUpdate() {
        updateEpgHandler.removeCallbacks(updateEpgTask);
        updateEpgHandler.postDelayed(updateEpgTask, 2000);
    }

    /**
     * Fills the list with the available program guide data from the available
     * channels. Only the programs of those channels will be added to the
     * adapter that contain the selected channel tag.
     */
    private void populateList() {
        ChannelTag currentTag = Utils.getChannelTag(activity);
        adapter.clear();

        // Make a copy of the channel list before iterating over it
        adapter.clear();
        for (Channel channel : DataStorage.getInstance().getChannelsFromArray().values()) {
            if (currentTag == null || channel.tags.contains(currentTag.tagId)) {
                adapter.add(channel);
            }
        }
        adapter.sort(Utils.getChannelSortOrder(activity));
        startDelayedAdapterUpdate();

        // Inform the activity that the program list has been populated. The
        // activity will then inform the fragment to select the first item in
        // the list or scroll to the previously selected one in case the
        // orientation has changed
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        app.addListener(this);
        if (!dataStorage.isLoading()) {
            populateList();
        }
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * This line is only visible in the first screen where the current time is
     * shown. This method is called every minute.
     */
    @SuppressLint("InlinedApi")
    private void setCurrentTimeIndication() {
        if (bundle != null && currentTimeIndication != null && activity != null) {
            if (tabIndex == 0) {
                // Get the difference between the current time and the given
                // start time. Calculate from this value in minutes the width in
                // pixels. This will be horizontal offset for the time
                // indication. If channel icons are shown then we need to add a
                // the icon width to the offset.
                final long startTime = bundle.getLong(Constants.BUNDLE_EPG_START_TIME, 0);
                final long currentTime = Calendar.getInstance().getTimeInMillis();
                final long durationTime = (currentTime - startTime) / 1000 / 60;
                final float pixelsPerMinute = bundle.getFloat(Constants.BUNDLE_EPG_PIXELS_PER_MINUTE, 0);
                final int offset = (int) (durationTime * pixelsPerMinute);

                // Get the height of the title layout
                Rect titleLayoutRect = new Rect();
                titleLayout.getLocalVisibleRect(titleLayoutRect);

                // Set the left and top margins of the time indication so it
                // starts in the actual program guide and not in the channel
                // list or time line.
                final int layout = LayoutParams.MATCH_PARENT;
                RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(3, layout);
                parms.setMargins(offset, titleLayoutRect.height(), 0, 0);
                currentTimeIndication.setLayoutParams(parms);
            } else {
                currentTimeIndication.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public void onDestroy() {
        updateEpgHandler.removeCallbacks(updateEpgTask);
        updateViewHandler.removeCallbacks(updateViewTask);
        updateTimeIndicationHandler.removeCallbacks(updateTimeIndicationTask);

        fragmentStatusInterface = null;
        fragmentScrollInterface = null;
        super.onDestroy();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (selectedProgram == null) {
            return true;
        }

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            menuUtils.handleMenuSearchWebSelection(selectedProgram.title);
            return true;

        case R.id.menu_search_epg:
            menuUtils.handleMenuSearchEpgSelection(selectedProgram.title);
            return true;

        case R.id.menu_record_remove:
            Recording rec = dataStorage.getRecordingFromArray(selectedProgram.dvrId);
            if (rec != null) {
                if (rec.isRecording()) {
                    menuUtils.handleMenuStopRecordingSelection(rec.id, rec.title);
                } else if (rec.isScheduled()) {
                    menuUtils.handleMenuCancelRecordingSelection(rec.id, rec.title);
                } else {
                    menuUtils.handleMenuRemoveRecordingSelection(rec.id, rec.title);
                }
            }
            return true;

        case R.id.menu_record_once:
            menuUtils.handleMenuRecordSelection(selectedProgram.eventId);
            return true;

        case R.id.menu_record_series:
            menuUtils.handleMenuSeriesRecordSelection(selectedProgram.title);
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
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;
                        if (loading) {
                            adapter.clear();
                            startDelayedAdapterUpdate();
                        } else {
                            populateList();
                        }
                    }
                });
                break;
            case "channelAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.add((Channel) obj);
                        adapter.sort(Utils.getChannelSortOrder(activity));
                        startDelayedAdapterUpdate();
                    }
                });
                break;
            case "channelDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.remove((Channel) obj);
                        startDelayedAdapterUpdate();
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
                        startDelayedAdapterUpdate();
                    }
                });
                break;
            case "channelUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        final Channel ch = (Channel) obj;
                        adapter.update(ch);
                        startDelayedAdapterUpdate();
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
        populateList();
    }

    @Override
    public void setSelection(final int position, final int offset) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, offset);
        }
    }

    @Override
    public void setInitialSelection(final int position) {
        setSelection(position, 0);
    }

    @Override
    public Object getSelectedItem() {
        return null;
    }

    @Override
    public int getItemCount() {
        return adapter.getCount();
    }
}
