package org.tvheadend.tvhclient.features.purchase;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.PurchaseEvent;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.tasks.FileLoaderCallback;
import org.tvheadend.tvhclient.features.shared.tasks.HtmlFileLoaderTask;
import org.tvheadend.tvhclient.features.startup.SplashActivity;
import org.tvheadend.tvhclient.utils.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class UnlockerFragment extends BaseFragment implements FileLoaderCallback {

    @BindView(R.id.webview)
    protected WebView webView;
    @BindView(R.id.loading)
    protected ProgressBar progressBar;
    private HtmlFileLoaderTask htmlFileLoaderTask;
    private BillingProcessor billingProcessor;
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

        toolbarInterface.setTitle(getString(R.string.pref_unlocker));
        toolbarInterface.setSubtitle(null);

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
                // Check if the user has made the purchase and then restart the application.
                // In this case all classes and variables that might use the unlocked
                // information get initialized with the new value.
                if (billingProcessor.isPurchased(Constants.UNLOCKER)) {
                    Answers.getInstance().logPurchase(new PurchaseEvent()
                            .putItemName("Unlocker")
                            .putSuccess(true));

                    new MaterialDialog.Builder(activity)
                            .title(R.string.dialog_title_purchase_ok)
                            .content(R.string.dialog_content_purchase_ok)
                            .canceledOnTouchOutside(false)
                            .positiveText(R.string.dialog_button_restart)
                            .onPositive((dialog, which) -> {
                                // Restart the app so that the unlocker will be activated
                                Intent intent = new Intent(activity, SplashActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                activity.startActivity(intent);
                            })
                            .show();
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
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }
}
