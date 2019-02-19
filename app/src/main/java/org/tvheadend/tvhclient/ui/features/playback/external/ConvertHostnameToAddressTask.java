package org.tvheadend.tvhclient.ui.features.playback.external;

import android.os.AsyncTask;
import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.domain.entity.Connection;

import java.net.InetAddress;
import java.net.UnknownHostException;

import timber.log.Timber;

class ConvertHostnameToAddressTask extends AsyncTask<Void, Void, String> {

    private final Connection connection;

    ConvertHostnameToAddressTask(@NonNull Connection connection) {
        this.connection = connection;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            return  InetAddress.getByName(connection.getHostname()).getHostAddress();
        } catch (UnknownHostException e) {
            Timber.d("Could not get ip address from " + connection.getHostname() + ", using hostname as fallback", e);
            return connection.getHostname();
        }
    }
}
