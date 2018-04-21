package org.tvheadend.tvhclient.features.shared.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;


public class ImageDownloadTask extends AsyncTask<String, Integer, Drawable> {
    private final ImageDownloadTaskCallback callback;

    public ImageDownloadTask(ImageDownloadTaskCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Drawable doInBackground(String... strings) {
        return downloadImage(strings[0]);
    }

    protected void onPostExecute(Drawable image) {
        if (callback != null) {
            callback.notify(image);
        }
    }

    private Drawable downloadImage(String path) {
        if (path == null) {
            return null;
        }

        URL url;
        InputStream in;
        BufferedInputStream buf;

        try {
            // Open the stream and read it
            url = new URL(path);
            in = url.openStream();
            buf = new BufferedInputStream(in);

            // Convert the BufferedInputStream to a Bitmap
            Bitmap bMap = BitmapFactory.decodeStream(buf);
            in.close();
            buf.close();
            return new BitmapDrawable(bMap);

        } catch (Exception e) {
            // NOP
        }
        return null;
    }
}