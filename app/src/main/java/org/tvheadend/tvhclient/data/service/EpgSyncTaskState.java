package org.tvheadend.tvhclient.data.service;

import org.tvheadend.tvhclient.features.shared.receivers.ServiceStatusReceiver;

public class EpgSyncTaskState {

    private final ServiceStatusReceiver.State state;
    private final String message;
    private final String details;

    EpgSyncTaskState(EpgSyncTaskStateBuilder builder) {
        this.state = builder.state;
        this.message = builder.message;
        this.details = builder.details;
    }

    public ServiceStatusReceiver.State getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public static class EpgSyncTaskStateBuilder {
        private ServiceStatusReceiver.State state = ServiceStatusReceiver.State.CLOSED;
        private String message = "";
        private String details = "";

        public EpgSyncTaskStateBuilder state(ServiceStatusReceiver.State state) {
            this.state = state;
            return this;
        }

        public EpgSyncTaskStateBuilder message(String message) {
            this.message = message;
            return this;
        }

        public EpgSyncTaskStateBuilder details(String details) {
            this.details = details;
            return this;
        }

        public EpgSyncTaskState build() {
            return new EpgSyncTaskState(this);
        }
    }
}
