package org.tvheadend.tvhclient.features.information;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.tasks.FileLoaderCallback;
import org.tvheadend.tvhclient.features.shared.tasks.HtmlFileLoaderTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class HelpFragment extends BaseFragment implements FileLoaderCallback {

    @BindView(R.id.webview)
    protected WebView webView;
    @BindView(R.id.loading)
    protected ProgressBar progressBar;
    private HtmlFileLoaderTask htmlFileLoaderTask;
    private Unbinder unbinder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.webview_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        forceSingleScreenLayout();

        toolbarInterface.setTitle(getString(R.string.help));
        toolbarInterface.setSubtitle(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        htmlFileLoaderTask = new HtmlFileLoaderTask(activity, "help_and_support", "en", this);
        htmlFileLoaderTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (htmlFileLoaderTask != null) {
            htmlFileLoaderTask.cancel(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void notify(String content) {
        if (!TextUtils.isEmpty(content)) {
            webView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }
}
