package org.tvheadend.tvhclient.fragments.epg;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.activities.ToolbarInterface;
import org.tvheadend.tvhclient.adapter.ProgramGuideChannelListAdapter;
import org.tvheadend.tvhclient.htsp.HTSListener;
import org.tvheadend.tvhclient.interfaces.FragmentControlInterface;
import org.tvheadend.tvhclient.interfaces.FragmentScrollInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;

// TODO header placeholder for current time is missing in layout

public class ProgramGuideChannelListFragment extends ListFragment implements HTSListener, OnScrollListener, FragmentControlInterface {

    private final static String TAG = ProgramGuideChannelListFragment.class.getSimpleName();

    private Activity activity;
    private FragmentScrollInterface fragmentScrollInterface;
    private ProgramGuideChannelListAdapter adapter;

    // Enables scrolling when the user has touch the screen and starts
    // scrolling. When the user is done, scrolling will be disabled to prevent
    // unwanted calls to the interface. 
    private boolean enableScrolling = false;
    private ToolbarInterface toolbarInterface;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        adapter = new ProgramGuideChannelListAdapter(activity, new ArrayList<>());
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
        getListView().setVerticalScrollBarEnabled(false);
        getListView().setOnScrollListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (activity instanceof FragmentScrollInterface) {
            fragmentScrollInterface = (FragmentScrollInterface) activity;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);
        setListShown(!DataStorage.getInstance().isLoading());

        if (!DataStorage.getInstance().isLoading()) {
            populateList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication.getInstance().removeListener(this);
    }

    /**
     * Fills the adapter with the available channel data. Only those channels
     * will be added to the adapter that contain the selected channel tag.
     */
    private void populateList() {
        ChannelTag currentTag = null;
        Connection connection = DatabaseHelper.getInstance(getActivity().getApplicationContext()).getSelectedConnection();
        if (connection != null) {
            currentTag = DataStorage.getInstance().getTagFromArray(connection.channelTag);
        }
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
        toolbarInterface.setTitle(getString(R.string.pref_program_guide));
        toolbarInterface.setSubtitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.tagName);
    }

    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;
                        setListShown(!loading);
                        if (!loading) {
                            populateList();
                        }
                    }
                });
                break;
            case "channelAdd":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Channel channel = (Channel) obj;
                        adapter.add(channel);
                        adapter.sort(Utils.getChannelSortOrder(activity));
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "channelDelete":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Channel channel = (Channel) obj;
                        adapter.remove(channel);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
            case "channelUpdate":
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Channel channel = (Channel) obj;
                        adapter.update(channel);
                        adapter.notifyDataSetChanged();
                    }
                });
                break;
        }
    }

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

    @Override
    public void reloadData() {
        populateList();
    }

    @Override
    public void setSelection(final int position, final int offset) {
        if (getListView().getCount() > position && position >= 0) {
            getListView().setSelectionFromTop(position, offset);
        }
    }
}
