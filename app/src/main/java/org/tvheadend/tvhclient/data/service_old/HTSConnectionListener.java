package org.tvheadend.tvhclient.data.service_old;

import org.tvheadend.tvhclient.data.service.htsp.HtspConnection;
import org.tvheadend.tvhclient.data.service.htsp.tasks.Authenticator;

import androidx.annotation.NonNull;

public interface HTSConnectionListener {

    /**
     * This method is invoked by the HTSConnection class. Whenever the server
     * sends a new message the listeners will be informed.
     *
     * @param response Response from the server
     */
    void onMessage(HTSMessage response);

    void onAuthenticationStateChange(@NonNull Authenticator.State state);

    void onConnectionStateChange(@NonNull HtspConnection.State state);
}
