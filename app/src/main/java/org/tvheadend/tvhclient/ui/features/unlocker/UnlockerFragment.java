package org.tvheadend.tvhclient.ui.features.unlocker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.ui.base.billing.BillingHandler;
import org.tvheadend.tvhclient.ui.base.billing.BillingManager;
import org.tvheadend.tvhclient.ui.base.billing.BillingUpdatesListener;
import org.tvheadend.tvhclient.ui.features.information.WebViewFragment;
import org.tvheadend.tvhclient.ui.base.tasks.HtmlFileLoaderTask;
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity;

import java.util.List;

import timber.log.Timber;

import static org.tvheadend.tvhclient.utils.Constants.UNLOCKER;

public class UnlockerFragment extends WebViewFragment implements HtmlFileLoaderTask.Listener, BillingUpdatesListener {

    private BillingManager billingManager;
    private BillingHandler billingHandler;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.pref_unlocker));
        toolbarInterface.setSubtitle("");
        billingManager = MainApplication.getInstance().getBillingManager();
        billingHandler = MainApplication.getInstance().getBillingHandler();
    }

    @Override
    public void onResume() {
        super.onResume();
        billingHandler.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        billingHandler.removeListener(this);
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
                if (!MainApplication.getInstance().isUnlocked()) {
                    Timber.d("Unlocker not purchased");
                    billingManager.initiatePurchaseFlow(activity, UNLOCKER, null, BillingClient.SkuType.INAPP);
                } else {
                    Timber.d("Unlocker already purchased");
                    showPurchasedAlreadyMadeDialog();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showPurchaseNotSuccessfulDialog() {
        Timber.d("Unlocker purchase not successful");
        new MaterialDialog.Builder(activity)
                .title(R.string.dialog_title_purchase_not_successful)
                .content(R.string.dialog_content_purchase_not_successful)
                .canceledOnTouchOutside(false)
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showPurchaseSuccessfulDialog() {
        Timber.d("Unlocker purchase successful");
        new MaterialDialog.Builder(activity)
                .title(R.string.dialog_title_purchase_successful)
                .content(R.string.dialog_content_purchase_successful)
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

    private void showPurchasedAlreadyMadeDialog() {
        Timber.d("Unlocker already purchased");
        new MaterialDialog.Builder(activity)
                .title(R.string.dialog_title_purchase_already_made)
                .content(R.string.dialog_content_purchase_already_made)
                .canceledOnTouchOutside(false)
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onBillingClientSetupFinished() {

    }

    @Override
    public void onConsumeFinished(String token, int result) {

    }

    @Override
    public void onPurchaseSuccessful(List<Purchase> purchases) {
        Timber.d("Purchase was successful");
        showPurchaseSuccessfulDialog();
    }

    @Override
    public void onPurchaseCancelled() {
        Timber.d("Purchase was cancelled");
    }

    @Override
    public void onPurchaseError(int errorCode) {
        Timber.d("Purchase was not successful");
        showPurchaseNotSuccessfulDialog();
    }
}
