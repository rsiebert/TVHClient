package org.tvheadend.tvhclient.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.utils.MiscUtils;

public class UnlockerFragment extends Fragment {

    private WebView mWebView;
    private TVHClientApplication mMainApplication;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.webview_layout, container, false);
        mWebView = view.findViewById(R.id.webview);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        mMainApplication = (TVHClientApplication) getActivity().getApplication();
        loadContents();
    }

    private void loadContents() {
        // TODO put the loading stuff into a separate task
        String content = MiscUtils.loadHtmlFromFile(getActivity(), "features", "en");
        mWebView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);
        // TODO assign a style to the webview
        if (MiscUtils.getThemeId(getActivity()) == R.style.CustomTheme_Light) {
            mWebView.setBackgroundColor(Color.WHITE);
        } else {
            mWebView.setBackgroundColor(Color.BLACK);
        }
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
                if (mMainApplication.getBillingProcessor().isInitialized()) {
                    mMainApplication.getBillingProcessor().purchase(getActivity(), Constants.UNLOCKER);
                }
                // Check if the user has already made the purchase. We check this
                // here because this activity is not information about any changes
                // via the billing event interface.
                if (mMainApplication.getBillingProcessor().isPurchased(Constants.UNLOCKER)) {
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
        if (!mMainApplication.getBillingProcessor().handleActivityResult(requestCode, resultCode, data)) {
            // The billing activity was not shown or did nothing. Nothing needs to be done
            super.onActivityResult(requestCode, resultCode, data);
        }
        getActivity().finish();
    }
}
