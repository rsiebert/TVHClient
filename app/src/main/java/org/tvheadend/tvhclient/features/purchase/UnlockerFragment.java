package org.tvheadend.tvhclient.features.purchase;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.anjlab.android.iab.v3.BillingProcessor;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.utils.Constants;
import org.tvheadend.tvhclient.features.shared.tasks.FileLoaderCallback;
import org.tvheadend.tvhclient.features.shared.tasks.HtmlFileLoaderTask;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;

public class UnlockerFragment extends Fragment implements FileLoaderCallback {

    private WebView webView;
    private ProgressBar loadingProgressBar;
    private HtmlFileLoaderTask htmlFileLoaderTask;
    private AppCompatActivity activity;
    private BillingProcessor billingProcessor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.webview_fragment, container, false);
        webView = v.findViewById(R.id.webview);
        loadingProgressBar = v.findViewById(R.id.loading);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setTitle(getString(R.string.pref_unlocker));
            toolbarInterface.setSubtitle(null);
        }
        setHasOptionsMenu(true);
        billingProcessor = MainApplication.getInstance().getBillingProcessor();
        htmlFileLoaderTask = new HtmlFileLoaderTask(activity, "features", "en", this);
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
        inflater.inflate(R.menu.unlocker_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;
            case R.id.menu_purchase:
                // Open the activity where the user can actually make the purchase
                if (billingProcessor.isInitialized()) {
                    billingProcessor.purchase(activity, Constants.UNLOCKER);
                }
                // Check if the user has already made the purchase. We check this
                // here because this activity is not information about any changes
                // via the billing event interface.
                if (billingProcessor.isPurchased(Constants.UNLOCKER)) {
                    if (getView() != null) {
                        Snackbar.make(getView(), getString(R.string.unlocker_already_purchased),
                                Snackbar.LENGTH_SHORT).show();
                    }
                    activity.finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            // The billing activity was not shown or did nothing. Nothing needs to be done
            super.onActivityResult(requestCode, resultCode, data);
        }
        activity.finish();
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
