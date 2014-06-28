package org.tvheadend.tvhclient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.tvheadend.tvhclient.ProgramGuideItemView.ProgramContextMenuInterface;
import org.tvheadend.tvhclient.adapter.ProgramGuideListAdapter;
import org.tvheadend.tvhclient.adapter.ProgramGuideListAdapter.ViewHolder;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.intent.SearchEPGIntent;
import org.tvheadend.tvhclient.intent.SearchIMDbIntent;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.ProgramGuideInterface;
import org.tvheadend.tvhclient.interfaces.ProgramGuideScrollingInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Program;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ProgramGuideListFragment extends Fragment implements HTSListener, ProgramContextMenuInterface, ProgramGuideScrollingInterface {

    private final static String TAG = ProgramGuideListFragment.class.getSimpleName();

    private FragmentActivity activity;
    private ProgramGuideInterface programGuideInterface;
    private ActionBarInterface actionBarInterface;
    private ProgramGuideListAdapter adapter;
    private ArrayAdapter<ChannelTag> tagAdapter;
    private AlertDialog tagDialog;
    private ListView listView;
    private LinearLayout titleLayout;
    private TextView titleDateText;
    private TextView titleDate;
    private TextView titleHours;
    private ImageView currentTimeIndication;
    private Bundle bundle;
    private Program selectedProgram = null;

    private boolean enableScrolling = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.program_guide_data_list, container, false);
        listView = (ListView) v.findViewById(R.id.item_list);
        titleLayout = (LinearLayout) v.findViewById(R.id.pager_title);
        titleDateText = (TextView) v.findViewById(R.id.pager_title_date_text);
        titleDate = (TextView) v.findViewById(R.id.pager_title_date);
        titleHours = (TextView) v.findViewById(R.id.pager_title_hours);
        currentTimeIndication = (ImageView) v.findViewById(R.id.current_time);

        // Set the date and the time slot hours in the title of the fragment
        bundle = getArguments();
        if (bundle != null) {

            final long startTime = bundle.getLong("startTime", 0);
            final Date startDate = new Date(startTime);
            final long endTime = bundle.getLong("endTime", 0);
            final Date endDate = new Date(endTime);

            // Set the current date and the date as text in the title
            Utils.setDate(titleDateText, startDate);
            final SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
            titleDate.setText("(" + sdf2.format(startDate) + ")");

            // Hide the date text if it shows the date time or the display is too narrow
            DisplayMetrics displaymetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            if (titleDateText.getText().equals(titleDate.getText()) || 
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

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (activity instanceof ProgramGuideInterface) {
            programGuideInterface = (ProgramGuideInterface) activity;
        }

        adapter = new ProgramGuideListAdapter(activity, this, new ArrayList<Channel>(), bundle);
        listView.setAdapter(adapter);

        // Create a scroll listener to inform the parent activity about
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Enables scrolling when the user has touch the screen and
                // starts scrolling. When the user is done, scrolling will be
                // disabled to prevent unwanted calls to the interface.
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    enableScrolling = true;
                } else if (scrollState == SCROLL_STATE_IDLE && enableScrolling) {
                    if (programGuideInterface != null) {
                        enableScrolling = false;
                        programGuideInterface.onScrollStateIdle(TAG);
                    }
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (programGuideInterface != null && enableScrolling) {
                    int index = view.getFirstVisiblePosition();
                    View v = view.getChildAt(0);
                    int position = (v == null) ? 0 : v.getTop();
                    programGuideInterface.onScrollingChanged(index, position, TAG);
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

        // Create the dialog where the user can select the different tags
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_tags);
        tagAdapter = new ArrayAdapter<ChannelTag>(activity, 
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<ChannelTag>());
        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                Utils.setChannelTagId(pos);
                populateList();
                ChannelListFragment channelFrag = (ChannelListFragment) activity.getSupportFragmentManager().findFragmentByTag("channel_icon_list");
                if (channelFrag != null) {
                    channelFrag.populateList();
                }
            }
        });
        tagDialog = builder.create();

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

        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu item of not required
        MenuItem genreItem = menu.findItem(R.id.menu_genre_color_info);
        if (genreItem != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            genreItem.setVisible(prefs.getBoolean("showGenreColorsGuidePref", false));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.epg_menu, menu);
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
        adapter.sort();
        adapter.notifyDataSetChanged();
        
        // Set the scroll position of the list view
        if (programGuideInterface != null) {
            listView.setSelectionFromTop(
                    programGuideInterface.getScrollingSelectionIndex(), 
                    programGuideInterface.getScrollingSelectionPosition());
        } else {
            listView.setSelection(0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            activity.onSearchRequested();
            return true;

        case R.id.menu_tags:
            tagDialog.show();
            return true;

        case R.id.menu_genre_color_info:
            Utils.showGenreColorDialog(activity);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        setLoading(app.isLoading());
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * This line is only visible in the first screen where the current time is
     * shown.
     */
    private void setCurrentTimeIndication() {
        if (bundle != null && currentTimeIndication != null && activity != null) {
            int tabIndex = bundle.getInt("tabIndex", -1);
            if (tabIndex == 0) {
                // Get the difference between the current time and the given
                // start time. Calculate from this value in minutes the width in
                // pixels. This will be horizontal offset for the time
                // indication. If channel icons are shown then we need to add a
                // the icon width to the offset.
                final long startTime = bundle.getLong("startTime", 0);
                final int hoursToShow = bundle.getInt("hoursToShow", 4);
                final long currentTime = Calendar.getInstance().getTimeInMillis();
                final long durationTime = (currentTime - startTime) / 1000 / 60;

                // The pixels per minute are smaller if icons are shown. Add the
                // icon width to start from the correct position
                final float pixelsPerMinute = Utils.getPixelsPerMinute(activity, 0, hoursToShow);
                final int offset = (int) (durationTime * pixelsPerMinute);

                // Get the height of the pager title layout
                Rect titleLayoutRect = new Rect();
                titleLayout.getLocalVisibleRect(titleLayoutRect);

                // Set the left and top margins of the time indication
                RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(3, LayoutParams.MATCH_PARENT);
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
        programGuideInterface = null;
        actionBarInterface = null;
        super.onDetach();
    }

    /**
     * Show that either no connection (and no data) is available, the data is
     * loaded or calls the method to display it.
     * 
     * @param loading
     */
    public void setLoading(boolean loading) {

        if (DatabaseHelper.getInstance() != null && 
                DatabaseHelper.getInstance().getSelectedConnection() == null) {
            adapter.clear();
            adapter.notifyDataSetChanged();
            if (actionBarInterface != null) {
                actionBarInterface.setActionBarSubtitle(getString(R.string.no_connections), TAG);
            }
        } else {
            if (loading) {
                adapter.clear();
                adapter.notifyDataSetChanged();
                if (actionBarInterface != null) {
                    actionBarInterface.setActionBarSubtitle(getString(R.string.loading), TAG);
                }
            } else {
                // Fill the tag adapter with the available channel tags
                TVHClientApplication app = (TVHClientApplication) activity.getApplication();
                tagAdapter.clear();
                for (ChannelTag t : app.getChannelTags()) {
                    tagAdapter.add(t);
                }
                // Update the list with the new guide data
                populateList();
            }
        }
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
            Utils.removeProgram(activity, selectedProgram.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(activity, selectedProgram.recording);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(activity, selectedProgram.id, selectedProgram.channel.id);
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
        if (action.equals(TVHClientApplication.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_CHANNEL_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Channel) obj);
                    adapter.notifyDataSetChanged();
                    adapter.sort();
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_CHANNEL_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHClientApplication.ACTION_PROGRAMME_UPDATE)
                || action.equals(TVHClientApplication.ACTION_PROGRAMME_DELETE)
                || action.equals(TVHClientApplication.ACTION_DVR_ADD)
                || action.equals(TVHClientApplication.ACTION_DVR_UPDATE)) {
            // An existing program has been updated
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_CHANNEL_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    adapter.update((Channel) obj);

                    // Only update the view if no more programs are being loaded
                    // from the server.
                    if (programGuideInterface != null) {
                        if (programGuideInterface.isChannelLoadingListEmpty()) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_TAG_ADD)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.add(tag);
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_TAG_DELETE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.remove(tag);
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_TAG_UPDATE)) {
            // NOP
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
    public void scrollListViewTo(int index) {
        if (listView != null) {
            listView.setSelection(index);
        }
    }

    @Override
    public void scrollListViewToPosition(int index, int pos) {
        if (listView != null) {
            listView.setSelectionFromTop(index, pos);
        }
    }
}
