package org.tvheadend.tvhclient.ui.epg;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.DatabaseHelper;
import org.tvheadend.tvhclient.data.model.Channel;
import org.tvheadend.tvhclient.data.model.ChannelTag;
import org.tvheadend.tvhclient.data.model.Connection;
import org.tvheadend.tvhclient.service.HTSListener;
import org.tvheadend.tvhclient.ui.base.ToolbarInterface;
import org.tvheadend.tvhclient.utils.Utils;

import java.util.ArrayList;

public class ProgramGuideChannelListFragment extends ListFragment implements HTSListener, OnScrollListener, ProgramGuideControlInterface {

    private Activity activity;
    private ProgramGuideScrollInterface programGuideScrollInterface;
    private ProgramGuideChannelListAdapter adapter;

    // Enables scrolling when the user has touch the screen and starts
    // scrolling. When the user is done, scrolling will be disabled to prevent
    // unwanted calls to the interface. 
    private boolean enableScrolling = false;
    private ToolbarInterface toolbarInterface;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.program_guide_channel_listfragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        if (activity instanceof ToolbarInterface) {
            toolbarInterface = (ToolbarInterface) activity;
        }

        adapter = new ProgramGuideChannelListAdapter(activity, new ArrayList<>());
        setListAdapter(adapter);
        getListView().setOnScrollListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment.isAdded() && fragment instanceof ProgramGuideScrollInterface) {
            programGuideScrollInterface = (ProgramGuideScrollInterface) fragment;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication.getInstance().addListener(this);

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
            if (currentTag == null || channel.getTags().contains(currentTag.getTagId())) {
                adapter.add(channel);
            }
        }
        adapter.sort(Utils.getChannelSortOrder(activity));
        adapter.notifyDataSetChanged();

        // Show the name of the selected channel tag and the number of channels
        // in the action bar. If enabled show also the channel tag icon.
        toolbarInterface.setTitle(getString(R.string.pref_program_guide));
        toolbarInterface.setSubtitle((currentTag == null) ? getString(R.string.all_channels) : currentTag.getTagName());
    }

    @Override
    public void onMessage(String action, final Object obj) {
        switch (action) {
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
