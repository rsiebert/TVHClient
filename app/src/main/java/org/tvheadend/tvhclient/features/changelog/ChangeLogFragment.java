package org.tvheadend.tvhclient.features.changelog;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.tasks.ChangeLogLoaderTask;
import org.tvheadend.tvhclient.features.shared.tasks.FileLoaderCallback;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.BackPressedInterface;

public class ChangeLogFragment extends Fragment implements BackPressedInterface, FileLoaderCallback {

    private WebView webView;
    private boolean showFullChangeLog = false;
    private ChangeLogLoaderTask changeLogLoaderTask;
    private ProgressBar loadingProgressBar;
    private AppCompatActivity activity;
    private String versionName = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.webview_fragment, null);
        webView = view.findViewById(R.id.webview);
        loadingProgressBar = view.findViewById(R.id.loading);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (AppCompatActivity) getActivity();
        if (activity instanceof ToolbarInterface) {
            ToolbarInterface toolbarInterface = (ToolbarInterface) activity;
            toolbarInterface.setTitle(getString(R.string.pref_changelog));
        }

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            showFullChangeLog = savedInstanceState.getBoolean("showFullChangelog", true);
            versionName = savedInstanceState.getString("versionNameForChangelog");
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                showFullChangeLog = bundle.getBoolean("showFullChangelog", true);
                versionName = bundle.getString("versionNameForChangelog", BuildConfig.VERSION_NAME);
            }
        }

        showChangelog(showFullChangeLog);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showFullChangelog", showFullChangeLog);
        outState.putString("versionNameForChangelog", versionName);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (changeLogLoaderTask != null) {
            changeLogLoaderTask.cancel(true);
        }
    }

    private void showChangelog(boolean showFullChangeLog) {
        webView.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.VISIBLE);
        changeLogLoaderTask = new ChangeLogLoaderTask(activity, versionName, this);
        changeLogLoaderTask.execute(showFullChangeLog);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        (menu.findItem(R.id.menu_full_changelog)).setVisible(!showFullChangeLog);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.changelog_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_full_changelog:
                changeLogLoaderTask = new ChangeLogLoaderTask(activity, versionName, this);
                changeLogLoaderTask.execute(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Save the information that the changelog was shown
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("versionNameForChangelog", BuildConfig.VERSION_NAME);
        editor.apply();

        activity.setResult(Activity.RESULT_OK, null);
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
