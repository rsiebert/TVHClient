package org.tvheadend.tvhclient.features.information;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.tasks.HtmlFileLoaderTask;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class WebViewFragment extends BaseFragment implements HtmlFileLoaderTask.Listener {

    @BindView(R.id.webview)
    protected WebView webView;
    @BindView(R.id.loading)
    protected ProgressBar progressBar;
    private HtmlFileLoaderTask htmlFileLoaderTask;
    private Unbinder unbinder;
    private String website;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        if (savedInstanceState != null) {
            website = savedInstanceState.getString("website", "");
        } else {
            website = "";
            Bundle bundle = getArguments();
            if (bundle != null) {
                website = bundle.getString("website", "");
            }
        }

        switch (website) {
            case "information":
                toolbarInterface.setTitle(getString(R.string.pref_information));
                break;
            case "help_and_support":
                toolbarInterface.setTitle(getString(R.string.help_and_support));
                break;
            case "privacy_policy":
                toolbarInterface.setTitle(getString(R.string.pref_privacy_policy));
                break;
        }

        toolbarInterface.setSubtitle("");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("website", website);
    }

    @Override
    public void onResume() {
        super.onResume();
        htmlFileLoaderTask = new HtmlFileLoaderTask(activity, website, "en", this);
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
    public void onFileContentsLoaded(String content) {
        if (!TextUtils.isEmpty(content)) {

            if (content.contains("styles_light.css")) {
                if (MiscUtils.getThemeId(activity) == R.style.CustomTheme_Light) {
                    content = content.replace("styles_light.css", "html/styles_light.css");
                } else {
                    content = content.replace("styles_light.css", "html/styles_dark.css");
                }
            }
            if (content.contains("APP_VERSION")) {
                // Replace the placeholder in the html file with the real version
                String version = BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_VERSION + ")";
                content = Pattern.compile("APP_VERSION").matcher(content).replaceAll(version);
            }

            webView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "utf-8", null);
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }
}
