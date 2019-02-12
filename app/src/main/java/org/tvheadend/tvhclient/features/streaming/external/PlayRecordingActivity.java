package org.tvheadend.tvhclient.features.streaming.external;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.service.HtspService;

import java.io.File;

import androidx.annotation.Nullable;
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
    }

    @Override
    protected boolean requireHostnameToAddressConversion() {
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("dvrId", dvrId);
    }

    @Override
    protected void getHttpTicket() {
        if (dvrId > 0) {
            Intent intent = new Intent(this, HtspService.class);
            intent.setAction("getTicket");
            intent.putExtra("dvrId", dvrId);
            startService(intent);
        } else {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText(getString(R.string.error_starting_playback_no_recording));
        }
    }

    @Override
    protected void onHttpTicketReceived() {
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
            Timber.d("Playing recording from server with url: " + serverUrl);
            intent.setDataAndType(Uri.parse(serverUrl), "video/*");
        }
        startExternalPlayer(intent);
    }
}
