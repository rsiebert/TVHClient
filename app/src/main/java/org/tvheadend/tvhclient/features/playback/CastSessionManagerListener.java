package org.tvheadend.tvhclient.features.playback;

import android.app.Activity;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

public class CastSessionManagerListener implements SessionManagerListener<CastSession> {

    private Activity activity;
    private CastSession castSession;

    public CastSessionManagerListener(Activity activity, CastSession castSession) {
        this.activity = activity;
        this.castSession = castSession;
    }

    @Override
    public void onSessionEnded(CastSession session, int error) {
        if (session == castSession) {
            castSession = null;
        }
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onSessionResumed(CastSession session, boolean wasSuspended) {
        castSession = session;
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onSessionStarted(CastSession session, String sessionId) {
        castSession = session;
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onSessionStarting(CastSession session) {
    }

    @Override
    public void onSessionStartFailed(CastSession session, int error) {
    }

    @Override
    public void onSessionEnding(CastSession session) {
    }

    @Override
    public void onSessionResuming(CastSession session, String sessionId) {
    }

    @Override
    public void onSessionResumeFailed(CastSession session, int error) {
    }

    @Override
    public void onSessionSuspended(CastSession session, int reason) {
    }
}
