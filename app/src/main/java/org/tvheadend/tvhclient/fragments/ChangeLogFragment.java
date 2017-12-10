package org.tvheadend.tvhclient.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import org.tvheadend.tvhclient.activities.SettingsToolbarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.tasks.ChangeLogLoaderTask;
import org.tvheadend.tvhclient.tasks.FileLoaderCallback;

public class ChangeLogFragment extends android.app.Fragment implements BackPressedInterface, FileLoaderCallback {

    private WebView webView;
    private boolean showFullChangeLog = false;
    private ChangeLogLoaderTask changeLogLoaderTask;
    private ProgressBar loadingProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.webview_layout, null);
        webView = view.findViewById(R.id.webview);
        loadingProgressBar = view.findViewById(R.id.loading);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof SettingsToolbarInterface) {
            SettingsToolbarInterface toolbarInterface = (SettingsToolbarInterface) getActivity();
            toolbarInterface.setTitle(getString(R.string.pref_changelog));
        }

        setHasOptionsMenu(true);

        // Get the build version where the changelog was last shown
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String lastAppVersion = sharedPreferences.getString("app_version_name_for_changelog", "");
        changeLogLoaderTask = new ChangeLogLoaderTask(getActivity(), lastAppVersion, this);

        // Show the full changelog if the changelog was never shown before (app version
        // name is empty) or if it was already shown and the version name is the same as
        // the one in the preferences. Otherwise show the changelog of the newest app version.
        showFullChangeLog = (lastAppVersion.isEmpty() || lastAppVersion.equals(BuildConfig.VERSION_NAME));
        showChangelog(showFullChangeLog);
    }

    @Override
    public void onPause() {
        super.onPause();
        changeLogLoaderTask.cancel(true);
    }

    private void showChangelog(boolean showFullChangeLog) {
        webView.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.VISIBLE);
        changeLogLoaderTask.execute(showFullChangeLog);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        (menu.findItem(R.id.menu_full_changelog)).setVisible(!showFullChangeLog);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_changelog, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_full_changelog:
                showChangelog(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Save the information that the changelog was shown
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("app_version_name_for_changelog", BuildConfig.VERSION_NAME);
        editor.apply();
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
