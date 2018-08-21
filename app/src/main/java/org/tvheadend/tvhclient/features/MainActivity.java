package org.tvheadend.tvhclient.features;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.features.download.DownloadPermissionGrantedInterface;
import org.tvheadend.tvhclient.features.playback.CastSessionManagerListener;
import org.tvheadend.tvhclient.features.shared.BaseActivity;
import org.tvheadend.tvhclient.features.shared.callbacks.ToolbarInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

import javax.inject.Inject;

import timber.log.Timber;

// TODO casting needs rework
// TODO check for gmtoffset
// TODO move the conversion from ms to s or minutes from the intents into the entity getter and setter
// TODO add option in menu to show file missing recordings
// TODO give up after x reconnect retries
// TODO reschedule work when not successful
// TODO epg search in epg
// TODO show recording state in details view
// TODO add option to add recording and directly edit it

public class MainActivity extends BaseActivity implements ToolbarInterface {

    private MenuItem mediaRouteMenuItem;
    @Inject
    protected SharedPreferences sharedPreferences;
    @Inject
    protected AppRepository appRepository;
    protected boolean isUnlocked;
    protected boolean isDualPane;
    protected Toolbar toolbar;

    private IntroductoryOverlay introductoryOverlay;
    private CastSession castSession;
    private CastContext castContext;
    private CastStateListener castStateListener;
    private SessionManagerListener<CastSession> castSessionManagerListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(MiscUtils.getThemeId(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        MiscUtils.setLanguage(this);

        MainApplication.getComponent().inject(this);

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            Timber.d("Google API is available");
            Answers.getInstance().logCustom(new CustomEvent("Startup")
                    .putCustomAttribute("Google API", "Available"));

            castContext = CastContext.getSharedInstance(this);
            castSessionManagerListener = new CastSessionManagerListener(this, castSession);
            castStateListener = new CastStateListener() {
                @Override
                public void onCastStateChanged(int newState) {
                    if (newState != CastState.NO_DEVICES_AVAILABLE) {
                        showIntroductoryOverlay();
                    }
                }
            };
        } else {
            Timber.d("Google API is not available, casting will no be enabled");
            Answers.getInstance().logCustom(new CustomEvent("Startup")
                    .putCustomAttribute("Google API", "Not available"));
        }

        isDualPane = findViewById(R.id.details) != null;

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        isUnlocked = MainApplication.getInstance().isUnlocked();
        boolean showCastingMiniController = isUnlocked && sharedPreferences.getBoolean("casting_minicontroller_enabled", false);
        View miniController = findViewById(R.id.cast_mini_controller);
        miniController.setVisibility(showCastingMiniController ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (castContext != null) {
            return castContext.onDispatchVolumeKeyEventBeforeJellyBean(event) || super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        if (castContext != null) {
            castContext.addCastStateListener(castStateListener);
            castContext.getSessionManager().addSessionManagerListener(castSessionManagerListener, CastSession.class);
            if (castSession == null) {
                castSession = CastContext.getSharedInstance(this).getSessionManager().getCurrentCastSession();
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (castContext != null) {
            castContext.removeCastStateListener(castStateListener);
            castContext.getSessionManager().removeSessionManagerListener(castSessionManagerListener, CastSession.class);
        }
        super.onPause();
    }

    @Override
    public void setTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(subtitle);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mediaRouteMenuItem.setVisible(isUnlocked);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);
        return true;
    }

    private void showIntroductoryOverlay() {
        if (introductoryOverlay != null) {
            introductoryOverlay.remove();
        }
        if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    introductoryOverlay = new IntroductoryOverlay.Builder(
                            MainActivity.this, mediaRouteMenuItem)
                            .setTitleText(getString(R.string.intro_overlay_text))
                            .setOverlayColor(R.color.primary)
                            .setSingleTime()
                            .setOnOverlayDismissedListener(
                                    new IntroductoryOverlay.OnOverlayDismissedListener() {
                                        @Override
                                        public void onOverlayDismissed() {
                                            introductoryOverlay = null;
                                        }
                                    })
                            .build();
                    introductoryOverlay.show();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions[0].equals("android.permission.WRITE_EXTERNAL_STORAGE")) {

            Timber.d("Storage permission granted");
            Fragment fragment = getSupportFragmentManager().findFragmentById(isDualPane ? R.id.details : R.id.main);
            if (fragment != null && fragment instanceof DownloadPermissionGrantedInterface) {
                ((DownloadPermissionGrantedInterface) fragment).downloadRecording();
            }
        }
    }
}
