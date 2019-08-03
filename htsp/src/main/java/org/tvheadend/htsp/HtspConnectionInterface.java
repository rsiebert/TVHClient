package org.tvheadend.htsp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface HtspConnectionInterface {
    void addMessageListener(@NonNull HtspMessageListener listener);

    void removeMessageListener(@NonNull HtspMessageListener listener);

    // synchronized, non blocking connect
    void openConnection();

    boolean isNotConnected();

    boolean isAuthenticated();

    // synchronized, blocking auth
    void authenticate();

    void sendMessage(@NonNull HtspMessage message);

    void sendMessage(@NonNull HtspMessage message, @Nullable HtspResponseListener listener);

    void closeConnection();
}
