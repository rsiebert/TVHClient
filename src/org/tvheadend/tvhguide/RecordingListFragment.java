/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide;

import java.util.ArrayList;
import java.util.List;

import org.tvheadend.tvhguide.adapter.RecordingListAdapter;
import org.tvheadend.tvhguide.htsp.HTSListener;
import org.tvheadend.tvhguide.htsp.HTSService;
import org.tvheadend.tvhguide.intent.SearchEPGIntent;
import org.tvheadend.tvhguide.intent.SearchIMDbIntent;
import org.tvheadend.tvhguide.model.Recording;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 *
 * @author john-tornblom
 */
public class RecordingListFragment extends Fragment implements HTSListener {

    private RecordingListAdapter recAdapter;
    private ListView recListView;
    private List<Recording> recList;
    private int tabIndex = 0;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Return if frame for this fragment doesn't
        // exist because the fragment will not be shown.
        if (container == null)
            return null;
        
        View v = inflater.inflate(R.layout.recording_list, container, false);
        recListView = (ListView) v.findViewById(R.id.recording_list);
        
        // Get the passed argument so we know which recording type to display
        Bundle bundle = getArguments();
        if (bundle != null)
            tabIndex = bundle.getInt("tabIndex", 0);
        
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        recList = new ArrayList<Recording>();
        recAdapter = new RecordingListAdapter(getActivity(), recList);
        recAdapter.sort();
        recListView.setAdapter(recAdapter);
        registerForContextMenu(recListView);

        recListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Recording rec = (Recording) recAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), RecordingActivity.class);
                intent.putExtra("id", rec.id);
                startActivity(intent);
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.addListener(this);
        populateList();
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        app.removeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.recording_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_search:
            getActivity().onSearchRequested();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuItem item = null;
        Intent intent = null;

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Recording rec = recAdapter.getItem(info.position);

        menu.setHeaderTitle(rec.title);

        intent = new Intent(getActivity(), HTSService.class);
        intent.putExtra("id", rec.id);

        if (rec.isRecording() || rec.isScheduled()) {
            intent.setAction(HTSService.ACTION_DVR_CANCEL);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_cancel, ContextMenu.NONE, R.string.menu_record_cancel);
            item.setIntent(intent);
        } else {
            intent.setAction(HTSService.ACTION_DVR_DELETE);
            item = menu.add(ContextMenu.NONE, R.string.menu_record_remove, ContextMenu.NONE, R.string.menu_record_remove);
            item.setIntent(intent);

            item = menu.add(ContextMenu.NONE, R.string.ch_play, ContextMenu.NONE, R.string.ch_play);
            intent = new Intent(getActivity(), ExternalPlaybackActivity.class);
            intent.putExtra("dvrId", rec.id);
            item.setIntent(intent);
            item.setIcon(android.R.drawable.ic_menu_view);
        }

        item = menu.add(ContextMenu.NONE, R.string.search_hint, ContextMenu.NONE, R.string.search_hint);
        item.setIntent(new SearchEPGIntent(getActivity(), rec.title));
        item.setIcon(android.R.drawable.ic_menu_search);

        item = menu.add(ContextMenu.NONE, ContextMenu.NONE, ContextMenu.NONE, "IMDb");
        item.setIntent(new SearchIMDbIntent(getActivity(), rec.title));
        item.setIcon(android.R.drawable.ic_menu_info_details);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.string.menu_record:
            case R.string.menu_record_cancel: {
                getActivity().startService(item.getIntent());
                return true;
            }
            case R.string.menu_record_remove: {
                new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.menu_record_remove)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) { 
                            getActivity().startService(item.getIntent());
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) { 
                            //NOP
                        }
                    })
                    .show();

                return true;
            }
            default: {
                return super.onContextItemSelected(item);
            }
        }
    }

    private void setLoading(boolean loading) {
        if (loading) {
            recAdapter.clear();
            recAdapter.notifyDataSetChanged();
            getActivity().getActionBar().setSubtitle(R.string.inf_load);
        } else {
            populateList();
        }
    }

    private void populateList() {
        TVHGuideApplication app = (TVHGuideApplication) getActivity().getApplication();
        recList.clear();
        
        // Show only the recordings that belong to the tab
        for (Recording rec : app.getRecordings()) {
            if (tabIndex == 0 &&
                    rec.error == null &&
                    rec.state.equals("completed")) {
                recList.add(rec);
            }
            else if (tabIndex == 1 &&
                    rec.error == null &&
                    (rec.state.equals("scheduled") || 
                     rec.state.equals("recording") ||
                     rec.state.equals("autorec"))) {
                recList.add(rec);
            }
            else if (tabIndex == 2 &&
                    (rec.error != null ||
                    (rec.state.equals("missed") || rec.state.equals("invalid")))) {
                recList.add(rec);
            }
        }
        recAdapter.notifyDataSetChanged();
        ((RecordingListTabsActivity)getActivity()).updateTitle(tabIndex, recList.size());
    }
    
    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_LOADING)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    setLoading(loading);
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_ADD)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    recAdapter.add((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                    recAdapter.sort();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_DELETE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    recAdapter.remove((Recording) obj);
                    recAdapter.notifyDataSetChanged();
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_DVR_UPDATE)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Recording rec = (Recording) obj;
                    recAdapter.updateView(recListView, rec);
                }
            });
        }
    }

    public int getRecordingCount() {
        if (recList != null)
            return recList.size();
        
        return 0;
    }
}
