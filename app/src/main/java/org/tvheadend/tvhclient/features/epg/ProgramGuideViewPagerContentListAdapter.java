package org.tvheadend.tvhclient.features.epg;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.utils.Constants;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Program;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class ProgramGuideViewPagerContentListAdapter extends ArrayAdapter<Channel> {

    @SuppressWarnings("unused")
    private final static String TAG = ProgramGuideViewPagerContentListAdapter.class.getSimpleName();

    private final Activity activity;
    private final List<Channel> list;
    private final long startTime;
    private final long endTime;
    private final int displayWidth;
    private final float pixelsPerMinute;
    private ViewHolder holder = null;
    final private LayoutInflater inflater;

    private final Fragment fragment;

    ProgramGuideViewPagerContentListAdapter(Activity activity, Fragment fragment, List<Channel> list, long startTime, long endTime, int displayWidth, float pixelsPerMinute) {
        super(activity, R.layout.program_guide_viewpager_list_adapter, list);
        this.activity = activity;
        this.fragment = fragment;
        this.list = list;
        this.inflater = activity.getLayoutInflater();
        this.startTime = startTime;
        this.endTime = endTime;
        this.displayWidth = displayWidth;
        this.pixelsPerMinute = pixelsPerMinute;
    }

    public void sort(final int type) {
        switch (type) {
        case Constants.CHANNEL_SORT_DEFAULT:
            sort(new Comparator<Channel>() {
                public int compare(Channel x, Channel y) {
                    // return x.compareTo(y);
                    return x.getName().toLowerCase(Locale.US).compareTo(y.getName().toLowerCase(Locale.US));
                }
            });
            break;
        case Constants.CHANNEL_SORT_BY_NAME:
            sort(new Comparator<Channel>() {
                public int compare(Channel x, Channel y) {
                    return x.getName().toLowerCase(Locale.US).compareTo(y.getName().toLowerCase(Locale.US));
                }
            });
            break;
        case Constants.CHANNEL_SORT_BY_NUMBER:
            sort(new Comparator<Channel>() {
                public int compare(Channel x, Channel y) {
                    if (x.getNumber() > y.getNumber()) {
                        return 1;
                    } else if (x.getNumber() < y.getNumber()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
            break;
        }
    }

    public static class ViewHolder {
        public ImageView icon;
        public LinearLayout timeline;
        public ProgramGuideViewPagerContentListAdapterContentsView item;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Timber.d("getView() called with: position = [" + position + "], convertView = [" + convertView + "], parent = [" + parent + "]");
        View view = convertView;

        if (view == null) {
            view = inflater.inflate(R.layout.program_guide_viewpager_list_adapter, parent, false);
            holder = new ViewHolder();
            //holder.stub = view.findViewById(R.id.stub);
            //holder.stub.setLayoutResource(R.layout.recording_details_contents);
            //stub.inflate();

            holder.timeline = view.findViewById(R.id.timeline);
            holder.item = new ProgramGuideViewPagerContentListAdapterContentsView(activity, fragment, holder.timeline, startTime, endTime, displayWidth, pixelsPerMinute);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Adds the channel and shows the programs. Channel is
        // required to have access to the EPG data.
        // Go through all programs and add them to the view
        //Timber.d("getView: loadRecordingById programs for view");
        Channel channel = getItem(position);
        int nextId = 0;
        List<Program> programList = new ArrayList<>();
/*
        Map<Integer, Program> map = DataStorage.getInstance().getProgramsFromArray();
        Iterator mapIt = map.values().iterator();
        Program p;
        while (mapIt.hasNext()) {
            p = (Program) mapIt.next();
            if (p.getId() == channel.getId()) {
                if (p.getStart() <= startTime && p.getStop() > startTime) {
                    programList.add(p);
                    nextId = p.getNextEventId();
                    break;
                }
            }
        }

        while (nextId != 0) {
            p = DataStorage.getInstance().getProgramFromArray(nextId);
            if (p != null && p.getNextEventId() > 0) {
                programList.add(p);
                nextId = p.getNextEventId();
            } else {
                nextId = 0;
            }
        }
        */
        //Timber.d("getView: loadRecordingById programs for view done");

        //Timber.d("getView: start adding programs to view");
        holder.item.addPrograms(parent, programList, channel);
        //Timber.d("getView: start adding programs to view done");

        Timber.d("getView() returned: ");
        return view;
    }
    
    public void update(Channel c) {
        int length = list.size();
        // Go through the list of programs and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).getId() == c.getId()) {
                list.set(i, c);
                break;
            }
        }
    }

    public List<Channel> getList() {
        return list;
    }
}
