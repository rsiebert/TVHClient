package org.tvheadend.tvhguide.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.model.Recording;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class RecordingListAdapter extends ArrayAdapter<Recording> {

    Activity context;
    List<Recording> list;

    public RecordingListAdapter(Activity context, List<Recording> list) {
        super(context, R.layout.recording_list_widget, list);
        this.context = context;
        this.list = list;
    }

    public void sort() {
        sort(new Comparator<Recording>() {

            public int compare(Recording x, Recording y) {
                return x.compareTo(y);
            }
        });
    }

    public void updateView(ListView listView, Recording recording) {
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            int pos = listView.getPositionForView(view);
            Recording rec = (Recording) listView.getItemAtPosition(pos);

            if (view.getTag() == null || rec == null) {
                continue;
            }

            if (recording.id != rec.id) {
                continue;
            }

            RecordingListViewWrapper wrapper = (RecordingListViewWrapper) view.getTag();
            wrapper.repaint(recording);
            break;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        RecordingListViewWrapper wrapper = null;

        Recording rec = list.get(position);

        if (row == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            row = inflater.inflate(R.layout.recording_list_widget, null, false);

            wrapper = new RecordingListViewWrapper(row);
            row.setTag(wrapper);

        } else {
            wrapper = (RecordingListViewWrapper) row.getTag();
        }

        wrapper.repaint(rec);
        return row;
    }
}