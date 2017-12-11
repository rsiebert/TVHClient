package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.adapter.ProgramGuideChannelListAdapter;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.interfaces.ToolbarInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.utils.MiscUtils;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;

public class ProgramGuideChannelListFragment extends Fragment implements HTSListener, FragmentControlInterface {

    private final static String TAG = ProgramGuideChannelListFragment.class.getSimpleName();

    private Activity activity;
    private FragmentStatusInterface fragmentStatusInterface;
    private FragmentScrollInterface fragmentScrollInterface;
    private ToolbarInterface toolbarInterface;

    //private ArrayList<ChannelTag> tagList = new ArrayList<>();
    private ProgramGuideChannelListAdapter adapter;
    private ListView listView;

    // Enables scrolling when the user has touch the screen and starts
    // scrolling. When the user is done, scrolling will be disabled to prevent
    // unwanted calls to the interface. 
    private boolean enableScrolling = false;
    private TVHClientApplication app = null;
    private DataStorage dataStorage;
    private SharedPreferences sharedPreferences;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.program_guide_channel_list_layout, container, false);
        listView = v.findViewById(R.id.item_list);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        app = TVHClientApplication.getInstance();
        dataStorage = DataStorage.getInstance();

        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }
        if (activity instanceof FragmentScrollInterface) {
            fragmentScrollInterface = (FragmentScrollInterface) activity;
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        adapter = new ProgramGuideChannelListAdapter(activity, new ArrayList<>());
        listView.setAdapter(adapter);

        // Inform the activity when the channel list is scrolling or has
        // finished scrolling. This is only valid in the program guide
        // where only the channels are shown.
        listView.setFastScrollEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
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
    }

    /**
     * Fills the adapter with the available channel data. Only those channels
     * will be added to the adapter that contain the selected channel tag.
     * Populates the channel tag adapter so the user can select a new one.
     * Additionally some status information will be shown in the action bar.
     */
    private void populateList() {
        // Get the currently selected channel tag
        ChannelTag currentTag = Utils.getChannelTag(activity);
        //Log.d(TAG, "populateList: tag " + currentTag != null ? String.valueOf(currentTag.tagId) : "none");
        // Add only those channels that contain the selected channel tag
        adapter.clear();
        for (Channel channel : DataStorage.getInstance().getChannelsFromArray().values()) {
            if (currentTag == null || channel.tags.contains(currentTag.tagId)) {
                adapter.add(channel);
            }
        }

        adapter.sort(Utils.getChannelSortOrder(activity));
        adapter.notifyDataSetChanged();

        // Show the name of the selected channel tag and the number of channels
        // in the action bar. If enabled show also the channel tag icon.
        if (toolbarInterface != null) {
            toolbarInterface.setActionBarTitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.tagName);
            String items = getResources().getQuantityString(R.plurals.items, adapter.getCount(), adapter.getCount());
            toolbarInterface.setActionBarSubtitle(items);

            if (sharedPreferences.getBoolean("showIconPref", true)
                    && sharedPreferences.getBoolean("showTagIconPref", false)
                    && currentTag != null
                    && currentTag.tagId != 0) {
                Bitmap iconBitmap = MiscUtils.getCachedIcon(activity, currentTag.tagIcon);
                toolbarInterface.setActionBarIcon(iconBitmap);
            } else {
                toolbarInterface.setActionBarIcon(R.mipmap.ic_launcher);
            }
        }

        // Inform the activity that the channel list has been populated. The
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

    @Override
    public void onPause() {
        super.onPause();
        app.removeListener(this);
    }

    @Override
    public void onDestroy() {
        fragmentStatusInterface = null;
        fragmentScrollInterface = null;
        toolbarInterface = null;
        super.onDestroy();
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the specified action will be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;
                        if (loading) {
                            adapter.clear();
                            adapter.notifyDataSetChanged();
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
            case "channelUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.update((Channel) obj);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "eventUpdate":
            case "eventDelete":
            case "dvrEntryAdd":
            case "dvrEntryUpdate":
            case "dvrEntryDelete":
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

        if (adapter != null && adapter.getCount() > position) {
            adapter.setPosition(position);

            if (fragmentStatusInterface != null) {
                final Channel ch = adapter.getItem(position);
                fragmentStatusInterface.onListItemSelected(position, ch, TAG);
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return adapter.getSelectedItem();
    }

    @Override
    public int getItemCount() {
        return adapter.getCount();
    }
}
