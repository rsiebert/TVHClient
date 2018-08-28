package org.tvheadend.tvhclient.features.streaming.external;

import android.app.Activity;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

import timber.log.Timber;

public class CastSessionManagerListener implements SessionManagerListener<CastSession> {

    private final Activity activity;
    private CastSession castSession;

    public CastSessionManagerListener(Activity activity, CastSession castSession) {
        this.activity = activity;
        this.castSession = castSession;
    }

    @Override
    public void onSessionEnded(CastSession session, int error) {
        Timber.d("Cast session ended with error " + error);
        if (session == castSession) {
            castSession = null;
        }
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onSessionResumed(CastSession session, boolean wasSuspended) {
        Timber.d("Cast session resumed, was suspended " + wasSuspended);
        castSession = session;
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onSessionStarted(CastSession session, String sessionId) {
        Timber.d("Cast session started with id " + sessionId);
        castSession = session;
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onSessionStarting(CastSession session) {
        Timber.d("Cast session is starting");
    }

    @Override
    public void onSessionStartFailed(CastSession session, int error) {
        Timber.d("Cast session failed with error " + error);
    }

    @Override
    public void onSessionEnding(CastSession session) {
        Timber.d("Cast session is ending");
    }

    @Override
    public void onSessionResuming(CastSession session, String sessionId) {
        Timber.d("Cast session is resuming with id " + sessionId);
    }

    @Override
    public void onSessionResumeFailed(CastSession session, int error) {
        Timber.d("Cast session resume failed with error " + error);
    }

    @Override
    public void onSessionSuspended(CastSession session, int reason) {
        Timber.d("Cast session suspended with reason " + reason);
    }
}
