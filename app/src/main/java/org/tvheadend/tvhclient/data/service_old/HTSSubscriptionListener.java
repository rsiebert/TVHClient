package org.tvheadend.tvhclient.data.service_old;

import androidx.annotation.NonNull;

public interface HTSSubscriptionListener {

    void onSubscriptionStart(@NonNull HTSMessage message);

    void onSubscriptionStatus(@NonNull HTSMessage message);

    void onSubscriptionStop(@NonNull HTSMessage message);

    void onSubscriptionSkip(@NonNull HTSMessage message);

    void onSubscriptionSpeed(@NonNull HTSMessage message);

    void onQueueStatus(@NonNull HTSMessage message);

    void onSignalStatus(@NonNull HTSMessage message);

    void onTimeshiftStatus(@NonNull HTSMessage message);

    void onMuxPacket(@NonNull HTSMessage message);
}
