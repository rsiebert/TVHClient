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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
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

public class ProgramGuideListFragment extends Fragment implements HTSListener, ProgramContextMenuInterface {

    private final static String TAG = ProgramGuideListFragment.class.getSimpleName();

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
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
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
        try {
            programGuideInterface = (ProgramGuideInterface) activity;
        } catch (ClassCastException e) {
            Log.e(TAG, "Error casting activity, " + e.getMessage().toString());
        }
        try {
            actionBarInterface = (ActionBarInterface) getActivity();
        } catch (Exception e) {
            Log.e(TAG, "Error casting activity, " + e.getMessage().toString());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        adapter = new ProgramGuideListAdapter(getActivity(), this, new ArrayList<Channel>(), bundle);
        listView.setAdapter(adapter);

        // When the user has scrolled the program guide data list, inform the
        // activity about it so it can also scroll the channel list 
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (programGuideInterface != null) {
                        programGuideInterface.onScrollStateIdle();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
            }
        });

        listView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    int index = listView.getFirstVisiblePosition();
                    View v = listView.getChildAt(0);
                    int position = (v == null) ? 0 : v.getTop();
                    if (programGuideInterface != null) {
                        programGuideInterface.onScrollPositionChanged(index, position);
                    }
                }
                return false;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.menu_tags);
        tagAdapter = new ArrayAdapter<ChannelTag>(getActivity(), 
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<ChannelTag>());
        builder.setAdapter(tagAdapter, new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int pos) {
                Utils.setChannelTagId(pos);
                populateList();
                ChannelListFragment channelFrag = (ChannelListFragment) getActivity().getSupportFragmentManager().findFragmentByTag("channel_icon_list");
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
                        try {
                            setCurrentTimeIndication();
                        } catch (Exception e) {
                            Log.e(TAG, "Could not set the current time indication (vertical line), " + e.toString());
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 60000);
    }

    public void scrollListViewTo(int index) {
        if (listView != null) {
            listView.setSelection(index);
        }
    }

    public void scrollListViewToPosition(int index, int pos) {
        if (listView != null) {
            listView.setSelectionFromTop(index, pos);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hide the genre color menu item of not required
        MenuItem genreItem = menu.findItem(R.id.menu_genre_color_info);
        if (genreItem != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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

        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
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
            listView.setSelection(programGuideInterface.getScrollingSelectionIndex());
        } else {
            listView.setSelection(0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            getActivity().onSearchRequested();
            return true;

        case R.id.menu_tags:
            tagDialog.show();
            return true;

        case R.id.menu_genre_color_info:
            Utils.showGenreColorDialog(getActivity());
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        app.addListener(this);
        setLoading(app.isLoading());
    }

    /**
     * Shows a vertical line in the program guide to indicate the current time.
     * This line is only visible in the first screen where the current time is
     * shown.
     */
    private void setCurrentTimeIndication() {
        if (bundle != null && currentTimeIndication != null) {
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
                final float pixelsPerMinute = Utils.getPixelsPerMinute(getActivity(), 0, hoursToShow);
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
        TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
        app.removeListener(this);
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
                TVHClientApplication app = (TVHClientApplication) getActivity().getApplication();
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
            getActivity().startActivity(new SearchIMDbIntent(getActivity(), selectedProgram.title));
            return true;

        case R.id.menu_search_epg:
            getActivity().startActivity(new SearchEPGIntent(getActivity(), selectedProgram.title));
            return true;

        case R.id.menu_record_remove:
            Utils.removeProgram(getActivity(), selectedProgram.recording);
            return true;

        case R.id.menu_record_cancel:
            Utils.cancelProgram(getActivity(), selectedProgram.recording.id);
            return true;

        case R.id.menu_record:
            Utils.recordProgram(getActivity(), selectedProgram.id, selectedProgram.channel.id);
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
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_CHANNEL_ADD)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    adapter.add((Channel) obj);
                    adapter.notifyDataSetChanged();
                    adapter.sort();
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_CHANNEL_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    adapter.remove((Channel) obj);
                    adapter.notifyDataSetChanged();
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_CHANNEL_UPDATE)) {
            getActivity().runOnUiThread(new Runnable() {
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
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ChannelTag tag = (ChannelTag) obj;
                    tagAdapter.add(tag);
                }
            });
        }
        else if (action.equals(TVHClientApplication.ACTION_TAG_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
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
}
