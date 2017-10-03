package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DataStorage;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.data.DataContract;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.ActionBarInterface;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.DiscSpace;
import org.tvheadend.tvhclient.model.Recording;

public class StatusFragment extends Fragment implements HTSListener, LoaderManager.LoaderCallbacks<Cursor>, HTSService.Listener {

    @SuppressWarnings("unused")
    private final static String TAG = StatusFragment.class.getSimpleName();

    public HTSService mService;
    boolean mBound = false;
    private Activity activity;
    private ActionBarInterface actionBarInterface;

    private LinearLayout additionalInformationLayout;

    // This information is always available
    private TextView connection;
    private TextView status;
    private TextView channels;
    private TextView currentlyRec;
    private TextView completedRec;
    private TextView upcomingRec;
    private TextView failedRec;
    private TextView removedRec;
    private TextView seriesRec;
    private TextView timerRec;
    private TextView freediscspace;
    private TextView totaldiscspace;
    private TextView serverApiVersion;
    private String connectionStatus = "";

    private TVHClientApplication app;
    private DatabaseHelper dbh;
    private DataStorage ds;

    private static final int LOADER_ID_CHANNELS = 1;
    private static final int LOADER_ID_COMPLETED_RECORDINGS = 2;
    private static final int LOADER_ID_SCHEDULED_RECORDINGS = 3;
    private static final int LOADER_ID_FAILED_RECORDINGS = 4;
    private static final int LOADER_ID_REMOVED_RECORDINGS = 5;
    private static final int LOADER_ID_SERIES_RECORDINGS = 6;
    private static final int LOADER_ID_TIMER_RECORDINGS = 7;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // If the view group does not exist, the fragment would not be shown. So
        // we can return anyway.
        if (container == null) {
            return null;
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            connectionStatus = bundle.getString(Constants.BUNDLE_CONNECTION_STATUS);
        }

        View v = inflater.inflate(R.layout.status_fragment_layout, container, false);
        connection = (TextView) v.findViewById(R.id.connection);
        status = (TextView) v.findViewById(R.id.status);
        additionalInformationLayout = (LinearLayout) v.findViewById(R.id.additional_information_layout);
        channels = (TextView) v.findViewById(R.id.channels);
        currentlyRec = (TextView) v.findViewById(R.id.currently_recording);
        completedRec = (TextView) v.findViewById(R.id.completed_recordings);
        upcomingRec = (TextView) v.findViewById(R.id.upcoming_recordings);
        failedRec = (TextView) v.findViewById(R.id.failed_recordings);
        removedRec = (TextView) v.findViewById(R.id.removed_recordings);
        seriesRec = (TextView) v.findViewById(R.id.series_recordings);
        timerRec = (TextView) v.findViewById(R.id.timer_recordings);
        freediscspace = (TextView) v.findViewById(R.id.free_discspace);
        totaldiscspace = (TextView) v.findViewById(R.id.total_discspace);
        serverApiVersion = (TextView) v.findViewById(R.id.server_api_version);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        app = TVHClientApplication.getInstance();
        dbh = DatabaseHelper.getInstance(activity);
        ds = DataStorage.getInstance();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() called with: savedInstanceState = [" + savedInstanceState + "]");

        if (activity instanceof ActionBarInterface) {
            actionBarInterface = (ActionBarInterface) activity;
        }
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.status));
            actionBarInterface.setActionBarSubtitle("");
        }

        // Bind to LocalService
        Intent intent = new Intent(getActivity(), HTSService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Show how many series recordings are available
        if (ds.getProtocolVersion() < Constants.MIN_API_VERSION_SERIES_RECORDINGS) {
            seriesRec.setVisibility(View.GONE);
        } else {
            seriesRec.setVisibility(View.VISIBLE);
        }

        // Show how many timer recordings are available if the server supports
        // it and the application is unlocked
        if (ds.getProtocolVersion() < Constants.MIN_API_VERSION_TIMER_RECORDINGS || !app.isUnlocked()) {
            timerRec.setVisibility(View.GONE);
        } else {
            timerRec.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
        app.addListener(this);

        if (mBound) {
            Log.d(TAG, "onResume: adding listener");
            mService.addListener(this);
        }

        // Upon resume show the actual status. If stuff is loading hide certain
        // information, otherwise show the connection status and the cause of
        // possible connection problems. 
        additionalInformationLayout.setVisibility(View.GONE);
        if (ds.isLoading()) {
            onMessage(Constants.ACTION_LOADING, true);
        } else {
            onMessage(connectionStatus, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");

        app.removeListener(this);
        if (mBound) {
            Log.d(TAG, "onPause: removing listener");
            mService.removeListener(this);
        }
    }

    @Override
    public void onDestroy() {
        actionBarInterface = null;
        super.onDestroy();

        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onMessage(final String action, final Object obj) {
        switch (action) {
            case Constants.ACTION_CONNECTION_STATE_OK:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionStatus = action;
                        showCompleteStatus();
                    }
                });
                break;
            case Constants.ACTION_CONNECTION_STATE_UNKNOWN:
            case Constants.ACTION_CONNECTION_STATE_SERVER_DOWN:
            case Constants.ACTION_CONNECTION_STATE_LOST:
            case Constants.ACTION_CONNECTION_STATE_TIMEOUT:
            case Constants.ACTION_CONNECTION_STATE_REFUSED:
            case Constants.ACTION_CONNECTION_STATE_AUTH:
            case Constants.ACTION_CONNECTION_STATE_NO_CONNECTION:
            case Constants.ACTION_CONNECTION_STATE_NO_NETWORK:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        connectionStatus = action;

                        if (actionBarInterface != null) {
                            actionBarInterface.setActionBarTitle(getString(R.string.status));
                            actionBarInterface.setActionBarSubtitle("");
                        }

                        // Hide the additional status information because the
                        // connection to the server is not OK
                        additionalInformationLayout.setVisibility(View.GONE);
                        showConnectionName();
                        showConnectionStatus();
                    }
                });
                break;
            case Constants.ACTION_LOADING:
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        boolean loading = (Boolean) obj;
                        if (actionBarInterface != null) {
                            actionBarInterface.setActionBarTitle(getString(R.string.status));
                            actionBarInterface.setActionBarSubtitle((loading ? getString(R.string.updating) : ""));
                        }

                        // Show that data is being loaded from the server and hide
                        // the additional information, because this information is
                        // outdated. If the data has been loaded from the server,
                        // show the additional status layout again and display the
                        // available information.
                        if (loading) {
                            status.setText(R.string.loading);
                            additionalInformationLayout.setVisibility(View.GONE);
                            showConnectionName();

                            freediscspace.setText(R.string.loading);
                            totaldiscspace.setText(R.string.loading);
                        } else {
                            showCompleteStatus();
                        }
                    }
                });
                break;
            case Constants.ACTION_DISC_SPACE:
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded()) {
                            showDiscSpace();
                        }
                    }
                });
                break;
        }
    }

    /**
     * Displays all available status information. This is the case
     * when the loading is done and the connection is fine
     */
    private void showCompleteStatus() {
        // The connection to the server is fine again, therefore
        // show the additional information again
        additionalInformationLayout.setVisibility(View.VISIBLE);
        showConnectionName();
        showConnectionStatus();
        showRecordingStatus();
        showDiscSpace();
    }

    /**
     * Shows the name and address of a connection, otherwise shows an
     * information that no connection is selected or available.
     */
    private void showConnectionName() {
        // Get the currently selected connection
        boolean noConnectionsDefined = false;
        Connection conn = null;
        if (dbh != null) {
            noConnectionsDefined = TVHClientApplication.getInstance().getContentProviderHelper().getConnections().isEmpty();
            conn = TVHClientApplication.getInstance().getContentProviderHelper().getSelectedConnection();
        }

        // Show the details about the current connection or an information that
        // none is selected or available
        if (conn == null) {
            if (noConnectionsDefined) {
                connection.setText(R.string.no_connection_available_advice);
            } else {
                connection.setText(R.string.no_connection_active_advice);
            }
        } else {
            String text = conn.name + " (" + conn.address + ")";
            connection.setText(text);
        }
    }

    /**
     * Shows the current connection status is displayed, this can be
     * authorization, timeouts or other errors.
     */
    private void showConnectionStatus() {
        // Show a textual description about the connection state
        switch (connectionStatus) {
            case Constants.ACTION_CONNECTION_STATE_OK:
                status.setText(R.string.ready);
                break;
            case Constants.ACTION_CONNECTION_STATE_SERVER_DOWN:
                status.setText(R.string.err_connect);
                break;
            case Constants.ACTION_CONNECTION_STATE_LOST:
                status.setText(R.string.err_con_lost);
                break;
            case Constants.ACTION_CONNECTION_STATE_TIMEOUT:
                status.setText(R.string.err_con_timeout);
                break;
            case Constants.ACTION_CONNECTION_STATE_REFUSED:
            case Constants.ACTION_CONNECTION_STATE_AUTH:
                status.setText(R.string.err_auth);
                break;
            case Constants.ACTION_CONNECTION_STATE_NO_NETWORK:
                status.setText(R.string.err_no_network);
                break;
            case Constants.ACTION_CONNECTION_STATE_NO_CONNECTION:
                status.setText(R.string.no_connection_available);
                break;
            default:
                status.setText(R.string.unknown);
                break;
        }
    }

    /**
     * Shows the available and total disc space either in MB or GB to avoid
     * showing large numbers. This depends on the size of the value.
     */
    private void showDiscSpace() {
        DiscSpace discSpace = ds.getDiscSpace();
        if (discSpace == null) {
            freediscspace.setText(R.string.unknown);
            totaldiscspace.setText(R.string.unknown);
            return;
        }

        try {
            // Get the disc space values and convert them to megabytes
            long free = Long.valueOf(discSpace.freediskspace) / 1000000;
            long total = Long.valueOf(discSpace.totaldiskspace) / 1000000;

            String freeDiscSpace;
            String totalDiscSpace;

            // Show the free amount of disc space as GB or MB
            if (free > 1000) {
                freeDiscSpace = (free / 1000) + " GB " + getString(R.string.available);
            } else {
                freeDiscSpace = free + " MB " + getString(R.string.available);
            }
            // Show the total amount of disc space as GB or MB
            if (total > 1000) {
                totalDiscSpace = (total / 1000) + " GB " + getString(R.string.total);
            } else {
                totalDiscSpace = total + " MB " + getString(R.string.total);
            }
            freediscspace.setText(freeDiscSpace);
            totaldiscspace.setText(totalDiscSpace);
        } catch (Exception e) {
            freediscspace.setText(R.string.unknown);
            totaldiscspace.setText(R.string.unknown);
        }
    }

    /**
     * Shows the program that is currently being recorded and the summary about
     * the available, scheduled and failed recordings.
     */
    private void showRecordingStatus() {
        String currentRecText = "";

        // Get the programs that are currently being recorded
        for (Recording rec : ds.getRecordings()) {
            if (rec.isRecording()) {
                currentRecText += getString(R.string.currently_recording) + ": " + rec.title;
                if (rec.channel != null) {
                    currentRecText += " (" + getString(R.string.channel) + " " + rec.channel.name + ")\n";
                }
            }
        }

        // Show which programs are being recorded
        currentlyRec.setText(currentRecText.length() > 0 ? currentRecText
                : getString(R.string.nothing));

        String version = String.valueOf(ds.getProtocolVersion())
                + "   (" + getString(R.string.server) + ": "
                + ds.getServerName() + " " + ds.getServerVersion() + ")";
        serverApiVersion.setText(version);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        final String[] channelProjection = new String[]{
                DataContract.Recordings.ID};

        final String[] recordingProjection = new String[]{
                DataContract.Recordings.ID,
                DataContract.Recordings.ERROR,
                DataContract.Recordings.STATE};

        switch (i) {
            case LOADER_ID_CHANNELS:
                return new CursorLoader(getActivity(), DataContract.Channels.CONTENT_URI,
                        channelProjection, null, null, null);

            case LOADER_ID_COMPLETED_RECORDINGS:
                return new CursorLoader(getActivity(), DataContract.Recordings.CONTENT_URI,
                        recordingProjection,
                        DataContract.Recordings.SELECTION_COMPLETED,
                        DataContract.Recordings.SELECTION_ARGS_COMPLETED, null);

            case LOADER_ID_SCHEDULED_RECORDINGS:
                return new CursorLoader(getActivity(), DataContract.Recordings.CONTENT_URI,
                        recordingProjection,
                        DataContract.Recordings.SELECTION_SCHEDULED,
                        DataContract.Recordings.SELECTION_ARGS_SCHEDULED, null);

            case LOADER_ID_FAILED_RECORDINGS:
                return new CursorLoader(getActivity(), DataContract.Recordings.CONTENT_URI,
                        recordingProjection,
                        DataContract.Recordings.SELECTION_FAILED,
                        DataContract.Recordings.SELECTION_ARGS_FAILED, null);

            case LOADER_ID_REMOVED_RECORDINGS:
                return new CursorLoader(getActivity(), DataContract.Recordings.CONTENT_URI,
                        recordingProjection,
                        DataContract.Recordings.SELECTION_REMOVED,
                        DataContract.Recordings.SELECTION_ARGS_REMOVED, null);

            case LOADER_ID_SERIES_RECORDINGS:
                return new CursorLoader(getActivity(), DataContract.SeriesRecordings.CONTENT_URI,
                        new String[]{DataContract.SeriesRecordings.ID}, null, null, null);

            case LOADER_ID_TIMER_RECORDINGS:
                return new CursorLoader(getActivity(), DataContract.TimerRecordings.CONTENT_URI,
                        new String[]{DataContract.TimerRecordings.ID}, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished() called with: loader = [" + loader.getId() + "]");

        int count = (cursor != null) ? cursor.getCount() : 0;
        switch (loader.getId()) {
            case LOADER_ID_CHANNELS:
                channels.setText(count + " " + getString(R.string.available));
                break;
            case LOADER_ID_COMPLETED_RECORDINGS:
                completedRec.setText(getResources().getQuantityString(R.plurals.completed_recordings, count, count));
                break;
            case LOADER_ID_SCHEDULED_RECORDINGS:
                upcomingRec.setText(getResources().getQuantityString(R.plurals.upcoming_recordings, count, count));
                break;
            case LOADER_ID_FAILED_RECORDINGS:
                failedRec.setText(getResources().getQuantityString(R.plurals.failed_recordings, count, count));
                break;
            case LOADER_ID_REMOVED_RECORDINGS:
                removedRec.setText(getResources().getQuantityString(R.plurals.removed_recordings, count, count));
                break;
            case LOADER_ID_SERIES_RECORDINGS:
                seriesRec.setText(getResources().getQuantityString(R.plurals.series_recordings, count, count));
                break;
            case LOADER_ID_TIMER_RECORDINGS:
                timerRec.setText(getResources().getQuantityString(R.plurals.timer_recordings, count, count));
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_CHANNELS:
                channels.setText(0 + " " + getString(R.string.available));
                break;
            case LOADER_ID_COMPLETED_RECORDINGS:
                completedRec.setText(getResources().getQuantityString(R.plurals.completed_recordings, 0, 0));
                break;
            case LOADER_ID_SCHEDULED_RECORDINGS:
                upcomingRec.setText(getResources().getQuantityString(R.plurals.upcoming_recordings, 0, 0));
                break;
            case LOADER_ID_FAILED_RECORDINGS:
                failedRec.setText(getResources().getQuantityString(R.plurals.failed_recordings, 0, 0));
                break;
            case LOADER_ID_REMOVED_RECORDINGS:
                removedRec.setText(getResources().getQuantityString(R.plurals.removed_recordings, 0, 0));
                break;
            case LOADER_ID_SERIES_RECORDINGS:
                seriesRec.setText(getResources().getQuantityString(R.plurals.series_recordings, 0, 0));
                break;
            case LOADER_ID_TIMER_RECORDINGS:
                timerRec.setText(getResources().getQuantityString(R.plurals.timer_recordings, 0, 0));
                break;
        }
    }

    @Override
    public void onInitialSyncCompleted() {
        Log.d(TAG, "onInitialSyncCompleted() called");
        // Prepare the loaders. Either re-connect with an existing one, or start a new one.
        getLoaderManager().initLoader(LOADER_ID_CHANNELS, null, this);
        getLoaderManager().initLoader(LOADER_ID_COMPLETED_RECORDINGS, null, this);
        getLoaderManager().initLoader(LOADER_ID_SCHEDULED_RECORDINGS, null, this);
        getLoaderManager().initLoader(LOADER_ID_FAILED_RECORDINGS, null, this);
        getLoaderManager().initLoader(LOADER_ID_REMOVED_RECORDINGS, null, this);
        getLoaderManager().initLoader(LOADER_ID_SERIES_RECORDINGS, null, this);
        getLoaderManager().initLoader(LOADER_ID_TIMER_RECORDINGS, null, this);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected() called with: className = [" + className + "], service = [" + service + "]");
            mService = ((HTSService.LocalBinder) service).getService();
            onInitialSyncCompleted();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected() called with: className = [" + className + "]");
            mService = null;
            mBound = false;
        }
    };
}
