package org.tvheadend.tvhclient.features.playback;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;
import org.tvheadend.tvhclient.data.remote.EpgSyncService;

import java.io.File;

import timber.log.Timber;

public class PlayRecordingActivity extends BasePlayActivity {

    private Recording recording;
    private int dvrId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            dvrId = getIntent().getIntExtra("dvrId", -1);
        } else {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                dvrId = bundle.getInt("dvrId", -1);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("dvrId", dvrId);
    }

    @Override
    protected void getHttpTicket() {
        Intent intent = new Intent(this, EpgSyncService.class);
        intent.setAction("getTicket");
        intent.putExtra("dvrId", dvrId);
        startService(intent);
    }

    @Override
    protected void onHttpTicketReceived(String path, String ticket) {
        Recording recording = new RecordingRepository(this).getRecordingByIdSync(dvrId);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra("itemTitle", recording.getTitle());
        intent.putExtra("title", recording.getTitle());

        // Check if the recording exists in the download folder, if not stream it from the server
        String downloadDirectory = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadDirectory, recording.getTitle() + ".mkv");

        if (file.exists() && MainApplication.getInstance().isUnlocked()) {
            Timber.d("Playing recording from local file: " + file.getAbsolutePath());
            intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "video/x-matroska");
        } else {
            Timber.d("Playing recording from server");
            intent.setDataAndType(Uri.parse(getPlayerUrl(path, ticket)), getPlayerMimeType());
        }
        startExternalPlayer(intent);
    }
}
