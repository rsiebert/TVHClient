package org.tvheadend.tvhclient;

import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.model.Connection;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WakeOnLanTask extends AsyncTask<String, Void, Integer> {
    private final static String TAG = WakeOnLanTask.class.getSimpleName();

    private final static int WOL_SEND = 0;
    private final static int WOL_SEND_BROADCAST = 1;
    private final static int WOL_INVALID_MAC = 2;
    private final static int WOL_ERROR = 3;

    private WeakReference<Context> mContext;
    private final Connection mConnection;
    private final Logger mLogger;
    private final AsyncTaskCallback mCallback;
    private Exception mException;

    public WakeOnLanTask(Context context, AsyncTaskCallback callback, Connection connection) {
        mContext = new WeakReference<>(context);
        mConnection = connection;
        mCallback = callback;
        mLogger = Logger.getInstance();
    }

    @Override
    protected Integer doInBackground(String... params) {
        // Exit if the MAC address is not ok, this should never happen because
        // it is already validated in the settings
        if (!validateMacAddress(mConnection.wol_mac_address)) {
            return WOL_INVALID_MAC;
        }
        // Get the MAC address parts from the string
        byte[] macBytes = getMacBytes(mConnection.wol_mac_address);

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
            InetAddress address;
            if (!mConnection.wol_broadcast) {
                address = InetAddress.getByName(mConnection.address);
                mLogger.log(TAG, "doInBackground: Sending WOL packet to " + address);
            } else {
                // Replace the last number by 255 to send the packet as a broadcast
                byte[] ipAddress = InetAddress.getByName(mConnection.address).getAddress();
                ipAddress[3] = (byte) 255;
                address = InetAddress.getByAddress(ipAddress);
                mLogger.log(TAG, "doInBackground: Sending WOL packet as broadcast to " + address.toString());
            }
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, mConnection.wol_port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            mLogger.log(TAG, "doInBackground: Datagram packet was sent");
            if (!mConnection.wol_broadcast) {
                return WOL_SEND;
            } else {
                return WOL_SEND_BROADCAST;
            }
        } catch (Exception e) {
            this.mException = e;
            return WOL_ERROR;
        }
    }

    /**
     * Checks if the given MAC address is correct.
     *
     * @param macAddress The MAC address that shall be validated
     * @return True if the MAC address is correct, false otherwise
     */
    private boolean validateMacAddress(final String macAddress) {
        if (macAddress == null) {
            return false;
        }
        // Check if the MAC address is valid
        Pattern pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}");
        Matcher matcher = pattern.matcher(macAddress);
        return matcher.matches();
    }

    /**
     * Splits the given MAC address into it's parts and saves it in the bytes
     * array
     *
     * @param macAddress The MAC address that shall be split
     * @return The byte array that holds the MAC address parts
     */
    private byte[] getMacBytes(final String macAddress) {
        byte[] macBytes = new byte[6];

        // Parse the MAC address elements into the array.
        String[] hex = macAddress.split("(:|-)");
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
        String message = "";
        Context context = mContext.get();
        if (context != null) {
            if (result == WOL_SEND) {
                message = context.getString(R.string.wol_send, mConnection.address);
            } else if (result == WOL_SEND_BROADCAST) {
                message = context.getString(R.string.wol_send_broadcast, mConnection.address);
            } else if (result == WOL_INVALID_MAC) {
                message = context.getString(R.string.wol_address_invalid);
            } else {
                message = context.getString(R.string.wol_error, mConnection.address, mException.getLocalizedMessage());
            }
        }
        mCallback.notify(message);
    }
}