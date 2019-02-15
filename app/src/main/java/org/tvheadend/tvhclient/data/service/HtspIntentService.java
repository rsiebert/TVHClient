package org.tvheadend.tvhclient.data.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import timber.log.Timber;

public class HtspIntentService extends JobIntentService implements HtspConnectionStateListener {

    private ScheduledExecutorService execService;
    private HtspConnection htspConnection;
    private Connection connection;
    private final int htspVersion;

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private final ArrayList<Program> pendingEventOps = new ArrayList<>();

    private Object authenticationLock = new Object();
    private Object responseLock = new Object();

    public HtspIntentService() {
        MainApplication.getComponent().inject(this);
        execService = Executors.newScheduledThreadPool(10);
        connection = appRepository.getConnectionData().getActiveItem();
        htspVersion = appRepository.getServerStatusData().getActiveItem().getHtspVersion();
        htspConnection = new HtspConnection(this, null);
        // Since this is blocking, spawn to a new thread
        execService.execute(() -> {
            htspConnection.openConnection();
            htspConnection.authenticate();
        });
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, HtspIntentService.class, 1, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        String action = intent.getAction();
        if (action == null || action.isEmpty()) {
            return;
        }

        synchronized (authenticationLock) {
            try {
                authenticationLock.wait(5000);
            } catch (InterruptedException e) {
                Timber.d("Timeout waiting while connecting to server");
            }
        }

        if (!htspConnection.isConnected() || !htspConnection.isAuthenticated()) {
            Timber.d("Connection to server failed or authentication failed");
            return;
        }

        Timber.d("Executing command " + action + " for service");
        switch (action) {
            case "getMoreEvents":
                getMoreEvents(intent);
                break;
            case "loadChannelIcons":
                loadAllChannelIcons();
                break;
        }
    }

    @Override
    public void onDestroy() {
        Timber.d("Stopping service");
        execService.shutdown();
        if (htspConnection != null) {
            htspConnection.closeConnection();
            htspConnection = null;
        }
        connection = null;
    }

    @Override
    public void onAuthenticationStateChange(@NonNull HtspConnection.AuthenticationState state) {
        Timber.d("Authentication state changed to " + state);
        synchronized (authenticationLock) {
            authenticationLock.notify();
        }
    }

    @Override
    public void onConnectionStateChange(@NonNull HtspConnection.ConnectionState state) {
        // NOP
    }

    /**
     * Handles the given server message that contains a list of events.
     *
     * @param message The message with the events
     */
    private void onGetEvents(HtspMessage message, Intent intent) {
        final String channelName = intent.getStringExtra("channelName");

        if (message.containsKey("events")) {
            List<Program> programs = new ArrayList<>();
            for (Object obj : message.getList("events")) {
                HtspMessage msg = (HtspMessage) obj;
                Program program = HtspUtils.convertMessageToProgramModel(new Program(), msg);
                program.setConnectionId(connection.getId());

                programs.add(program);
            }
            Timber.d("Added " + programs.size() + " events to the list for channel " + channelName);
            pendingEventOps.addAll(programs);
        }
    }


    /**
     * Tries to download and save all received channel and channel
     * tag logos from the initial sync in the database.
     */
    private void loadAllChannelIcons() {
        Timber.d("Downloading and saving all channel and channel tag icons...");

        List<String> iconUrls = new ArrayList<>();
        for (Channel channel : appRepository.getChannelData().getItems()) {
            if (!TextUtils.isEmpty(channel.getIcon())) {
                iconUrls.add(channel.getIcon());
            }
        }
        for (ChannelTag channelTag : appRepository.getChannelTagData().getItems()) {
            if (!TextUtils.isEmpty(channelTag.getTagIcon())) {
                iconUrls.add(channelTag.getTagIcon());
            }
        }

        int iconUrlCount = 0;
        for (String iconUrl : iconUrls) {
            // Determine if events for the last channel in the list are being loaded.
            // This is required to set and release a lock to get all responses
            // before saving the event data
            final boolean isLastIconUrl  = (++iconUrlCount == iconUrls.size());
            execService.execute(() -> {
                try {
                    Timber.d("Downloading icon url " + iconUrl);
                    downloadIconFromFileUrl(iconUrl);
                    // Release the lock so that all data can be saved
                    if (isLastIconUrl) {
                        synchronized (responseLock) {
                            Timber.d("Got response for last icon, releasing lock");
                            responseLock.notify();
                        }
                    }
                } catch (Exception e) {
                    Timber.d("Could not load icon " + iconUrl);
                }
            });

            // Wait until the last response from the server was received and the lock released
            if (isLastIconUrl) {
                synchronized (responseLock) {
                    try {
                        Timber.d("Loaded icons, waiting for response");
                        responseLock.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Downloads the file from the given url. If the url starts with http then a
     * buffered input stream is used, otherwise the htsp api is used. The file
     * will be saved in the cache directory using a unique hash value as the file name.
     *
     * @param url The url of the file that shall be downloaded
     * @throws IOException Error message if something went wrong
     */
    // Use the icon loading from the original library?
    private void downloadIconFromFileUrl(final String url) throws IOException {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        File file = new File(getCacheDir(), MiscUtils.convertUrlToHashString(url) + ".png");
        if (file.exists()) {
            Timber.d("Icon file " + file.getAbsolutePath() + " exists already");
            return;
        }

        InputStream is;
        if (url.startsWith("http")) {
            is = new BufferedInputStream(new URL(url).openStream());
        } else if (htspVersion > 9) {
            is = new HtspFileInputStream(htspConnection, url);
        } else {
            return;
        }

        OutputStream os = new FileOutputStream(file);

        // Set the options for a bitmap and decode an input stream into a bitmap
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, o);
        is.close();

        if (url.startsWith("http")) {
            is = new BufferedInputStream(new URL(url).openStream());
        } else if (htspVersion > 9) {
            is = new HtspFileInputStream(htspConnection, url);
        }

        float scale = getResources().getDisplayMetrics().density;
        int width = (int) (64 * scale);
        int height = (int) (64 * scale);

        // Set the sample size of the image. This is the number of pixels in
        // either dimension that correspond to a single pixel in the decoded
        // bitmap. For example, inSampleSize == 4 returns an image that is 1/4
        // the width/height of the original, and 1/16 the number of pixels.
        int ratio = Math.max(o.outWidth / width, o.outHeight / height);
        int sampleSize = Integer.highestOneBit((int) Math.floor(ratio));
        o = new BitmapFactory.Options();
        o.inSampleSize = sampleSize;

        // Now decode an input stream into a bitmap and compress it.
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, o);
        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        }

        os.close();
        is.close();
    }

    /**
     * Loads a defined number of events for all channels.
     * This method is called by a worker after the initial sync is done.
     * All loaded events are saved in a temporary list and saved in one
     * batch into the database when all events were loaded for all channels.
     *
     * @param intent The intent with the parameters e.g. to define how many events shall be loaded
     */
    private void getMoreEvents(Intent intent) {

        int numberOfProgramsToLoad = intent.getIntExtra("numFollowing", 0);
        List<Channel> channelList = appRepository.getChannelData().getItems();

        Timber.d("Database currently contains " + appRepository.getProgramData().getItemCount() + " events. ");
        Timber.d("Loading " + numberOfProgramsToLoad + " events for each of the " + channelList.size() + " channels");

        int channelCount = 0;
        for (Channel channel : channelList) {
            // Determine if events for the last channel in the list are being loaded.
            // This is required to set and release a lock to get all responses
            // before saving the event data
            final boolean isLastChannel = (++channelCount == channelList.size());

            Intent msgIntent = new Intent();
            msgIntent.putExtra("numFollowing", numberOfProgramsToLoad);
            msgIntent.putExtra("channelId", channel.getId());
            msgIntent.putExtra("channelName", channel.getName());

            Program lastProgram = appRepository.getProgramData().getLastItemByChannelId(channel.getId());
            if (lastProgram != null) {
                Timber.d("Loading more programs for channel " + channel.getName() +
                        " from last program id " + lastProgram.getEventId());
                msgIntent.putExtra("eventId", lastProgram.getNextEventId());
            } else if (channel.getNextEventId() > 0) {
                Timber.d("Loading more programs for channel " + channel.getName() +
                        " starting from channel next event id " + channel.getNextEventId());
                msgIntent.putExtra("eventId", channel.getNextEventId());
            } else {
                Timber.d("Loading more programs for channel " + channel.getName() +
                        " starting from channel event id " + channel.getEventId());
                msgIntent.putExtra("eventId", channel.getEventId());
            }

            HtspMessage request = HtspUtils.convertIntentToEventMessage(msgIntent);
            htspConnection.sendMessage(request, response -> {
                if (response != null) {
                    onGetEvents(response, msgIntent);
                    // Release the lock so that all data can be saved
                    if (isLastChannel) {
                        synchronized (responseLock) {
                            Timber.d("Got response for last channel, releasing lock");
                            responseLock.notify();
                        }
                    }
                } else {
                    Timber.d("Response is null");
                }
            });

            // Wait until the last response from the server was received and the lock released
            if (isLastChannel) {
                synchronized (responseLock) {
                    try {
                        Timber.d("Loaded more events for last channel, waiting for response");
                        responseLock.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Timber.d("Done loading more events");
        appRepository.getProgramData().addItems(pendingEventOps);
        Timber.d("Saved " + pendingEventOps.size() + " events for all channels. " +
                "Database contains " + appRepository.getProgramData().getItemCount() + " events");
        pendingEventOps.clear();
    }
}
