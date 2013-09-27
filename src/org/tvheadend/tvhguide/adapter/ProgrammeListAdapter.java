package org.tvheadend.tvhguide.adapter;

import java.util.Comparator;
import java.util.List;

import org.tvheadend.tvhguide.R;
import org.tvheadend.tvhguide.model.Program;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ProgrammeListAdapter extends ArrayAdapter<Program> {

    Activity context;
    List<Program> list;

    public ProgrammeListAdapter(Activity context, List<Program> list) {
        super(context, R.layout.program_list_widget, list);
        this.context = context;
        this.list = list;
    }

    public void sort() {
        sort(new Comparator<Program>() {

            public int compare(Program x, Program y) {
                return x.compareTo(y);
            }
        });
    }

    public void updateView(ListView listView, Program programme) {
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            int pos = listView.getPositionForView(view);
            Program pr = (Program) listView.getItemAtPosition(pos);

            if (view.getTag() == null || pr == null) {
                continue;
            }

            if (programme.id != pr.id) {
                continue;
            }

            ProgramListViewWrapper wrapper = (ProgramListViewWrapper) view.getTag();
            wrapper.repaint(programme);
            break;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ProgramListViewWrapper wrapper = null;

        if (row == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            row = inflater.inflate(R.layout.program_list_widget, null, false);

            wrapper = new ProgramListViewWrapper(context, row);
            row.setTag(wrapper);

        } else {
            wrapper = (ProgramListViewWrapper) row.getTag();
        }

        Program p = getItem(position);
        wrapper.repaint(p);
        return row;
    }

    public List<Program> getList() {
        return list;
    }
}
