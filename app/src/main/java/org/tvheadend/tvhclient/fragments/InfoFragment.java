package org.tvheadend.tvhclient.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.tasks.FileLoaderCallback;
import org.tvheadend.tvhclient.tasks.HtmlFileLoaderTask;

public class InfoFragment extends Fragment implements FileLoaderCallback {

    private WebView webView;
    private HtmlFileLoaderTask htmlFileLoaderTask;
    private ProgressBar loadingProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.webview_layout, container, false);
        webView = v.findViewById(R.id.webview);
        loadingProgressBar = v.findViewById(R.id.loading);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        htmlFileLoaderTask = new HtmlFileLoaderTask(getActivity(), "info_help", "en", this);
        htmlFileLoaderTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        htmlFileLoaderTask.cancel(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.info_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_changelog:
                final ChangeLogDialog cld = new ChangeLogDialog(getActivity());
                cld.getFullLogDialog().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void notify(String content) {
        if (content != null) {
            webView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);
            loadingProgressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }
}
