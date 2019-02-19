package org.tvheadend.tvhclient.data.service.htsp;

import androidx.annotation.NonNull;

public interface HtspConnectionStateListener {

    void onAuthenticationStateChange(@NonNull HtspConnection.AuthenticationState state);

    void onConnectionStateChange(@NonNull HtspConnection.ConnectionState state);
}
