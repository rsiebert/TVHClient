package org.tvheadend.tvhclient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.URL;


public class ImageDownloadTask extends AsyncTask<String, Integer, Drawable> {

    private final static String TAG = ImageDownloadTask.class.getSimpleName();
    private ImageView imageView;

    public ImageDownloadTask(ImageView imageView) {
        this.imageView = imageView;
    }

    @Override
    protected Drawable doInBackground(String... strings) {
        return downloadImage(strings[0], strings[1]);
    }

    protected void onPostExecute(Drawable image) {
        if (image != null) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(image);

            // Get the dimensions of the image so the
            // width / height ratio can be determined
            final float w = image.getIntrinsicWidth();
            final float h = image.getIntrinsicHeight();

            if (h > 0) {
                // Scale the image view so it fits the width of the dialog or fragment root view
                final float scale = h / w;
                final float vw = imageView.getRootView().getWidth() - 128;
                final float vh = vw * scale;
                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int)vw, (int)vh);
                layoutParams.gravity = Gravity.CENTER;
                imageView.setLayoutParams(layoutParams);
            }
        }
    }

    private Drawable downloadImage(String path, String name) {
        if (path == null) {
            return null;
        }

        URL url;
        BufferedOutputStream out;
        InputStream in;
        BufferedInputStream buf;

        try {
            // Open the stream and read it
            url = new URL(path);
            in = url.openStream();
            buf = new BufferedInputStream(in);

            // Convert the BufferedInputStream to a Bitmap
            Bitmap bMap = BitmapFactory.decodeStream(buf);
            if (in != null) {
                in.close();
            }
            if (buf != null) {
                buf.close();
            }
            return new BitmapDrawable(bMap);

        } catch (Exception e) {
            // NOP
        }
        return null;
    }
}