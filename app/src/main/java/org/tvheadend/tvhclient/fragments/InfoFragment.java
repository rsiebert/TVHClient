package org.tvheadend.tvhclient.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.ChangeLogDialog;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.regex.Pattern;

public class InfoFragment extends Fragment {

    private WebView mWebView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.webview_layout, container, false);
        mWebView = v.findViewById(R.id.webview);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        loadContents();
    }

    private void loadContents() {
        // TODO put the loading stuff into a separate task
        String content = MiscUtils.loadHtmlFromFile(getActivity(), "info_help", "en");
        // Replace the placeholder in the html file with the real version
        String version = BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_VERSION + ")";
        content = (Pattern.compile("APP_VERSION").matcher(content).replaceAll(version));

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
}
