package org.tvheadend.tvhclient.features.purchase;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.crashlytics.android.answers.PurchaseEvent;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.information.WebViewFragment;
import org.tvheadend.tvhclient.features.logging.AnswersWrapper;
import org.tvheadend.tvhclient.features.shared.tasks.FileLoaderCallback;
import org.tvheadend.tvhclient.features.startup.SplashActivity;
import org.tvheadend.tvhclient.utils.Constants;

import timber.log.Timber;

public class UnlockerFragment extends WebViewFragment implements FileLoaderCallback {

    private BillingProcessor billingProcessor;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbarInterface.setTitle(getString(R.string.pref_unlocker));
        toolbarInterface.setSubtitle(null);
        billingProcessor = MainApplication.getInstance().getBillingProcessor();
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
                if (!billingProcessor.isPurchased(Constants.UNLOCKER)) {
                    AnswersWrapper.getInstance().logPurchase(new PurchaseEvent()
                            .putItemName("Unlocker")
                            .putSuccess(true));

                    if (billingProcessor.purchase(activity, Constants.UNLOCKER)) {
                        showPurchaseSuccessfulDialg();
                    } else {
                        showPurchaseNotSuccessfulDialog();
                    }
                } else {
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

    private void showPurchaseSuccessfulDialg() {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            // The billing activity was not shown or did nothing. Nothing needs to be done
            super.onActivityResult(requestCode, resultCode, data);
        }
        activity.finish();
    }
}
