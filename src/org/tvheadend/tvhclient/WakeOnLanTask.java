package org.tvheadend.tvhclient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tvheadend.tvhclient.model.Connection;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class WakeOnLanTask extends AsyncTask<String, Void, Integer> {

    private final static String TAG = WakeOnLanTask.class.getSimpleName();

    private final static int WOL_SEND = 0;
    private final static int WOL_INVALID_MAC = 1;
    private final static int WOL_ERROR = 2;
    
    private Connection conn;
    private Context context;
    private Exception exception;

    public WakeOnLanTask(Context context, Connection conn) {
        this.context = context;
        this.conn = conn;
    }

    @Override
    protected Integer doInBackground(String... params) {
        // Exit if the MAC address is not ok, this should never happen because
        // it is already validated in the settings
        if (!validateMacAddress(conn.wol_address)) {
            return WOL_INVALID_MAC;
        }
        // Get the MAC address parts from the string
        byte[] macBytes = getMacBytes(conn.wol_address);

        // Assemble the byte array that the WOL consists of
        byte[] bytes = new byte[6 + 16 * macBytes.length];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) 0xff;
        }
        // Copy the elements from macBytes to i
        for (int i = 6; i < bytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }

        try {
            InetAddress address = InetAddress.getByName(conn.address);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, conn.wol_port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
            Log.d(TAG, "Datagram packet send");
            return WOL_SEND;
            
        } catch (Exception e) {
            this.exception = e;
            Log.d(TAG, "Exception for address " + conn.address + ", Exception " + e.getLocalizedMessage());
            return WOL_ERROR;
        }
    }

    /**
     * Checks if the given MAC address is correct.
     * 
     * @param macAddress
     * @return True if the MAC address is correct, false otherwise
     */
    private boolean validateMacAddress(final String macAddress) {
        if (macAddress == null) {
            return false;
        }
        // Check if the MAC address is valid
        Pattern pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}");
        Matcher matcher = pattern.matcher(macAddress);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }

    /**
     * Splits the given MAC address into it's parts and saves it in the bytes
     * array
     * 
     * @param macAddress
     * @return
     */
    private byte[] getMacBytes(final String macAddress) {
        byte[] macBytes = new byte[6];

        // Parse the MAC address elements into the array.
        String[] hex = macAddress.split("(\\:|\\-)");
        for (int i = 0; i < 6; i++) {
            macBytes[i] = (byte) Integer.parseInt(hex[i], 16);
        }
        return macBytes;
    }

    /**
     * Depending on the wake on LAN status the toast with the success or error
     * message is shown to the user
     */
    @Override
    protected void onPostExecute(Integer result) {
        if (result == WOL_SEND) {
            Toast.makeText(context, context.getString(R.string.wol_send, conn.address), Toast.LENGTH_SHORT).show();
        } else if (result == WOL_INVALID_MAC) {
            Toast.makeText(context, context.getString(R.string.wol_address_invalid), Toast.LENGTH_SHORT).show();
        } else {
            final String msg = exception.getLocalizedMessage();
            Toast.makeText(context, context.getString(R.string.wol_error, conn.address, msg), Toast.LENGTH_SHORT).show();
        }
    }
}