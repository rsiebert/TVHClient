package org.tvheadend.tvhclient.fragments;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.ProgramGuideItemView.ProgramContextMenuInterface;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.ProgramGuideListAdapter;
import org.tvheadend.tvhclient.adapter.ProgramGuideListAdapter.ViewHolder;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Program;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

    private Runnable updateEpgTask;
    private Handler updateEpgHandler = new Handler();

    // Enables scrolling when the user has touch the screen and starts
    // scrolling. When the user is done, scrolling will be disabled to prevent
    // unwanted calls to the interface. 
    private boolean enableScrolling = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Return if frame for this fragment doesn't exist because the fragment
        // will not be shown.
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.program_guide_pager_list, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);
        titleLayout = (LinearLayout) v.findViewById(R.id.pager_title);
        titleDateText = (TextView) v.findViewById(R.id.pager_title_date_text);
        titleDate = (TextView) v.findViewById(R.id.pager_title_date);
        titleHours = (TextView) v.findViewById(R.id.pager_title_hours);
        currentTimeIndication = (ImageView) v.findViewById(R.id.current_time);

        // Set the date and the time slot hours in the title of the fragment
        bundle = getArguments();
        if (bundle != null) {

            final long startTime = bundle.getLong(Constants.BUNDLE_EPG_START_TIME, 0);
            final Date startDate = new Date(startTime);
            final long endTime = bundle.getLong(Constants.BUNDLE_EPG_END_TIME, 0);
            final Date endDate = new Date(endTime);

            // Set the current date and the date as text in the title
            Utils.setDate(titleDateText, startDate);
            final SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            titleDate.setText("(" + sdf2.format(startDate) + ")");

            // Hide the date text if it shows the date time or the display is too narrow
            DisplayMetrics displaymetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            if (titleDateText.getText().equals(titleDate.getText()) ||
                    // TODO make 400 adjustable or detect automatically if it would wrap
                ((int) displaymetrics.widthPixels < 400)) { 
                titleDate.setVisibility(View.GONE);
            }

            final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            final String start = sdf.format(startDate);
            final String end = sdf.format(endDate);
            titleHours.setText(start + " - " + end);
        }
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (FragmentActivity) activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }
        if (activity instanceof FragmentScrollInterface) {
            fragmentScrollInterface = (FragmentScrollInterface) activity;
        }

        adapter = new ProgramGuideListAdapter(activity, this, new ArrayList<Channel>(), bundle);
        listView.setAdapter(adapter);

        // Create a scroll listener to inform the parent activity about
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
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unused")
                ViewHolder holder = (ViewHolder) view.getTag();
            }
        });

        // Create the handler and the timer task that will update the current
        // time indication every minute.
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        setCurrentTimeIndication();
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 60000);

        // Create the runnable that will initiate the update of the adapter that
        // will trigger the update of the program guide view
        updateEpgTask = new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
            }
        }; 
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
     * channels. Only the channels that are part of the selected tag are shown.
     */
    private void populateList() {
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        ChannelTag currentTag = Utils.getChannelTag(app);
        adapter.clear();
        for (Channel ch : app.getChannels()) {
            if (currentTag == null || ch.hasTag(currentTag.id)) {
                adapter.add(ch);
            }
        }
        adapter.sort(Utils.getChannelSortOrder(activity));
        startDelayedAdapterUpdate();

        // Inform the listeners that the channel list is populated.
        // They could then define the preselected list item.
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * This line is only visible in the first screen where the current time is
     * shown.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("InlinedApi")
    private void setCurrentTimeIndication() {
        if (bundle != null && currentTimeIndication != null && activity != null) {
            int tabIndex = bundle.getInt(Constants.BUNDLE_EPG_INDEX, -1);
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

                // Get the height of the pager title layout
                Rect titleLayoutRect = new Rect();
                titleLayout.getLocalVisibleRect(titleLayoutRect);

                // Set the left and top margins of the time indication
                final int layout = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) ? LayoutParams.MATCH_PARENT : LayoutParams.FILL_PARENT;
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
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onDetach() {
        fragmentStatusInterface = null;
        fragmentScrollInterface = null;
        super.onDetach();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (selectedProgram == null) {
            return true;
        }

        switch (item.getItemId()) {
        case R.id.menu_search_imdb:
            activity.startActivity(new SearchIMDbIntent(activity, selectedProgram.title));
            return true;

        case R.id.menu_search_epg:
            activity.startActivity(new SearchEPGIntent(activity, selectedProgram.title));
            return true;

        case R.id.menu_record_remove:
            Utils.confirmRemoveRecording(activity, selectedProgram.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.confirmCancelRecording(activity, selectedProgram.recording);
            return true;

        case R.id.menu_record_once:
            Utils.recordProgram(activity, selectedProgram, false);
            return true;

        case R.id.menu_record_series:
            Utils.recordProgram(activity, selectedProgram, true);
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
        if (action.equals(Constants.ACTION_LOADING)) {
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
        } else if (action.equals(Constants.ACTION_CHANNEL_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Channel) obj);
                    adapter.sort(Utils.getChannelSortOrder(activity));
                    startDelayedAdapterUpdate();
                }
            });
        } else if (action.equals(Constants.ACTION_CHANNEL_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Channel) obj);
                    startDelayedAdapterUpdate();
                }
            });
        } else if (action.equals(Constants.ACTION_PROGRAM_UPDATE)
                || action.equals(Constants.ACTION_PROGRAM_DELETE)
                || action.equals(Constants.ACTION_DVR_ADD)
                || action.equals(Constants.ACTION_DVR_UPDATE)) {
            // An existing program has been updated
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    startDelayedAdapterUpdate();
                }
            });
        }
        else if (action.equals(Constants.ACTION_CHANNEL_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    final Channel ch = (Channel) obj;
                    adapter.update(ch);

                    // Only update the channel if is not blocked
                    TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                    if (!app.isChannelBlocked(ch)) {
                        startDelayedAdapterUpdate();
                    }
                }
            });
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
    public void setInitialSelection(final int position) {
        setSelection(position, 0);
    }

    @Override
    public void setSelection(final int position, final int offset) {
        if (listView != null && listView.getCount() > position && position >= 0) {
            listView.setSelectionFromTop(position, offset);
        }
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
