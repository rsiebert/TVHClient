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

public class SearchResultAdapter extends ArrayAdapter<Program> {

    Activity context;
    List<Program> list;

    public SearchResultAdapter(Activity context, List<Program> list) {
        super(context, R.layout.search_result_widget, list);
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

            SearchResultWrapper wrapper = (SearchResultWrapper) view.getTag();
            wrapper.repaint(programme);
            break;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SearchResultWrapper wrapper = null;

        if (row == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            row = inflater.inflate(R.layout.search_result_widget, null, false);

            wrapper = new SearchResultWrapper(this.getContext(), row);
            row.setTag(wrapper);

        } else {
            wrapper = (SearchResultWrapper) row.getTag();
        }

        Program p = getItem(position);
        wrapper.repaint(p);
        return row;
    }

    public List<Program> getList() {
        return list;
    }
}
