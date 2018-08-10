package org.tvheadend.tvhclient.features.shared;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkAvailabilityChangedInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusInterface;
import org.tvheadend.tvhclient.features.shared.callbacks.NetworkStatusReceiverCallback;
import org.tvheadend.tvhclient.features.shared.receivers.NetworkStatusReceiver;

public abstract class BaseActivity extends AppCompatActivity implements NetworkStatusReceiverCallback, NetworkStatusInterface {

    private NetworkStatusReceiver networkStatusReceiver;
    protected boolean isNetworkAvailable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkStatusReceiver = new NetworkStatusReceiver(this);
        isNetworkAvailable = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(networkStatusReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(networkStatusReceiver);
    }

    @Override
    public void onNetworkAvailable() {
        isNetworkAvailable = true;
        onNetworkAvailabilityChanged(true);
    }

    @Override
    public void onNetworkNotAvailable() {
        isNetworkAvailable = false;
        onNetworkAvailabilityChanged(false);
    }

    protected void onNetworkAvailabilityChanged(boolean isNetworkAvailable) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main);
        if (fragment != null && fragment instanceof NetworkAvailabilityChangedInterface) {
            ((NetworkAvailabilityChangedInterface) fragment).onNetworkAvailabilityChanged(isNetworkAvailable);
        }
    }

    @Override
    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }
}
