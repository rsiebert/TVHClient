package org.tvheadend.tvhclient.features.streaming.external;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerProfile;
import org.tvheadend.tvhclient.data.service.EpgSyncService;

import java.io.File;

import timber.log.Timber;

public class PlayRecordingActivity extends BasePlaybackActivity {

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
        serverProfile = appRepository.getServerProfileData()
                .getItemById(serverStatus.getRecordingServerProfileId());
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
        Recording recording = appRepository.getRecordingData().getItemById(dvrId);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra("itemTitle", recording.getTitle());
        intent.putExtra("title", recording.getTitle());

        // Check if the recording exists in the download folder, if not stream it from the server
        String downloadDirectory = sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadDirectory, recording.getTitle() + ".mkv");

        if (file.exists() && MainApplication.getInstance().isUnlocked()) {
            Timber.d("Playing recording from local file: " + file.getAbsolutePath());
            intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "video/*");
        } else {
            String url = "http://" + baseUrl + path + "?ticket=" + ticket; //+ "&mux=matroska";
            ServerProfile serverProfile = appRepository.getServerProfileData().getItemById(serverStatus.getPlaybackServerProfileId());
            if (serverProfile != null) {
                url += "&profile=" + serverProfile.getName();
            }
            Timber.d("Playing recording from server with url: " + url);
            intent.setDataAndType(Uri.parse(url), "video/*");
        }
        startExternalPlayer(intent);
    }
}
