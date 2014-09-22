package org.tvheadend.tvhclient.fragments;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Recording;

import android.os.AsyncTask;
import android.util.Log;
import android.view.Menu;

public class SeriesRecordingListFragment extends RecordingListFragment {

    /**
     * Sets the correct tag. This is required for logging and especially for the
     * main activity so it knows what action shall be executed depending on the
     * recording fragment type.
     */
    public SeriesRecordingListFragment() {
        TAG = SeriesRecordingListFragment.class.getSimpleName();
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
        if (!app.isLoading()) {
            populateList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Only show the cancel all recordings menu if the correct tab is
        // selected and recordings are available that can be canceled.
        (menu.findItem(R.id.menu_record_remove_all)).setVisible(false);
        // Playing a scheduled recording is not possible 
        (menu.findItem(R.id.menu_play)).setVisible(false);
    }

    /**
     * Fills the list with the available recordings. Only the recordings that
     * are scheduled are added to the list.
     */
    private void populateList() {
        // Clear the list and add the recordings
        adapter.clear();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        for (Recording rec : app.getRecordings(Constants.RECORDING_TYPE_SERIES)) {
            adapter.add(rec);
        }
        adapter.sort();
        adapter.notifyDataSetChanged();
        
        // Shows the currently visible number of recordings of the type  
        if (actionBarInterface != null) {
            actionBarInterface.setActionBarTitle(getString(R.string.recordings), TAG);
            actionBarInterface.setActionBarSubtitle(adapter.getCount() + " " + getString(R.string.upcoming), TAG);
            actionBarInterface.setActionBarIcon(R.drawable.ic_launcher, TAG);
        }
        // Inform the listeners that the channel list is populated.
        // They could then define the preselected list item.
        if (fragmentStatusInterface != null) {
            fragmentStatusInterface.onListPopulated(TAG);
        }
        
        // just testing http stuff here
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                String user = null;
                char[] pass = null;
                Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
                if (conn != null) {
                    Log.d(TAG, "Got credentials for the password authenticator");
                    user = conn.username;
                    pass = conn.password.toCharArray();
                }
                Log.d(TAG, "Returning password authenticator");
                return new PasswordAuthentication(user, pass);
            }
        });
        
        URL url = null;
        try {
            Log.d(TAG, "Creating url");
            Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
            if (conn != null) {
                String s = "http://" + conn.address + ":" + conn.streaming_port;
                Log.d(TAG, "Url is " + s);
                url = new URL(s);
                new MyTask().execute(url);
            }
        } catch (MalformedURLException e) {
            Log.d(TAG, "Exception creating url");
            e.printStackTrace();
        }
        
    }

    private class MyTask extends AsyncTask<URL, Void, Void> {
        
        @Override
        protected Void doInBackground(URL... urls) {
            Log.d(TAG, "doInBackground");
            
            HttpURLConnection urlConnection = null;
            try {
                Log.d(TAG, "Opening url");
                urlConnection = (HttpURLConnection) urls[0].openConnection();
            } catch (IOException e) {
                Log.d(TAG, "Exception opening url");
                e.printStackTrace();
            }
            urlConnection.setRequestProperty("Authorization", "Basic");

            BufferedInputStream content = null;
            try {
                Log.d(TAG, "Getting input from server");
                content = new BufferedInputStream(urlConnection.getInputStream());
            } catch (IOException e) {
                Log.d(TAG, "Exception getting input from server");
                e.printStackTrace();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            String line;

            try {
                Log.d(TAG, "Reading contents");
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, line);
                }
            } catch (IOException e) {
                Log.d(TAG, "Exception reading contents");
                e.printStackTrace();
            }
            
            return null;
        }
    }
    
    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, final Object obj) {
        if (action.equals(Constants.ACTION_LOADING)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean loading = (Boolean) obj;
                    if (loading) {
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                    } else {
                        populateList();
                    }
                }
            });
        }
    }
}
