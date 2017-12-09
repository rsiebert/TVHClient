package org.tvheadend.tvhclient.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.tasks.HtmlFileLoaderCallback;
import org.tvheadend.tvhclient.tasks.HtmlFileLoaderTask;

public class UnlockerFragment extends Fragment implements HtmlFileLoaderCallback {

    private WebView webView;
    private ProgressBar loadingProgressBar;
    private HtmlFileLoaderTask htmlFileLoaderTask;

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
        htmlFileLoaderTask = new HtmlFileLoaderTask(getActivity(), "features", "en", this);
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
        inflater.inflate(R.menu.purchase_cancel_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.menu_purchase:
                // Open the activity where the user can actually make the purchase
                if (TVHClientApplication.getInstance().getBillingProcessor().isInitialized()) {
                    TVHClientApplication.getInstance().getBillingProcessor().purchase(getActivity(), Constants.UNLOCKER);
                }
                // Check if the user has already made the purchase. We check this
                // here because this activity is not information about any changes
                // via the billing event interface.
                if (TVHClientApplication.getInstance().getBillingProcessor().isPurchased(Constants.UNLOCKER)) {
                    if (getView() != null) {
                        Snackbar.make(getView(), getString(R.string.unlocker_already_purchased),
                                Snackbar.LENGTH_SHORT).show();
                    }
                    getActivity().finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!TVHClientApplication.getInstance().getBillingProcessor().handleActivityResult(requestCode, resultCode, data)) {
            // The billing activity was not shown or did nothing. Nothing needs to be done
            super.onActivityResult(requestCode, resultCode, data);
        }
        getActivity().finish();
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
