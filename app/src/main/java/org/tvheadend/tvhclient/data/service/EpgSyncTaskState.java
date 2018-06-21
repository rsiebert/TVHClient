package org.tvheadend.tvhclient.data.service;

public class EpgSyncTaskState {

    private final EpgSyncStatusReceiver.State state;
    private final String message;
    private final String details;

    EpgSyncTaskState(EpgSyncTaskStateBuilder builder) {
        this.state = builder.state;
        this.message = builder.message;
        this.details = builder.details;
    }

    public EpgSyncStatusReceiver.State getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public static class EpgSyncTaskStateBuilder {
        private EpgSyncStatusReceiver.State state = EpgSyncStatusReceiver.State.IDLE;
        private String message = "";
        private String details = "";

        public EpgSyncTaskStateBuilder state(EpgSyncStatusReceiver.State state) {
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
