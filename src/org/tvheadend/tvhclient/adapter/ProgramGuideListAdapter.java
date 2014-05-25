package org.tvheadend.tvhclient.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhclient.ProgramGuideItemView;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ProgramGuideListAdapter extends ArrayAdapter<Channel> {

    @SuppressWarnings("unused")
    private final static String TAG = ProgramGuideListAdapter.class.getSimpleName();

    private final FragmentActivity activity;
    private final List<Channel> list;
    public ViewHolder holder = null;
    private Bundle bundle;
    final private LayoutInflater inflater;

    private Fragment fragment;
    // private HashMap<Channel, Set<Program>> channelProgramList = new HashMap<Channel, Set<Program>>();
    
    public ProgramGuideListAdapter(FragmentActivity activity, Fragment fragment, List<Channel> list, Bundle bundle) {
        super(activity, R.layout.program_guide_list_item, list);
        this.activity = activity;
        this.fragment = fragment;
        this.list = list;
        this.bundle = bundle;
        this.inflater = activity.getLayoutInflater();
    }

    public void sort() {
        sort(new Comparator<Channel>() {
            public int compare(Channel x, Channel y) {
                return x.compareTo(y);
            }
        });
    }

    public static class ViewHolder {
        public ImageView icon;
        public LinearLayout timeline;
        public ProgramGuideItemView item;
    }

    public ProgramGuideItemView getListItem() {
        return holder.item;
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.program_guide_list_item, null);
            holder = new ViewHolder();
            holder.timeline = (LinearLayout) view.findViewById(R.id.timeline);
            holder.item = new ProgramGuideItemView(activity, fragment, holder.timeline, bundle);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Adds the channel and shows the programs 
        holder.item.addChannel(getItem(position));
        holder.item.addPrograms();

        return view;
    }
    
    public void update(Channel c) {
        int length = list.size();

        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).id == c.id) {
                list.set(i, c);
                break;
            }
        }
    }

    public List<Channel> getList() {
        return list;
    }
}
