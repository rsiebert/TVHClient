package org.tvheadend.tvhclient.ui.base.tasks;

import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.domain.entity.Connection;
import org.tvheadend.tvhclient.ui.base.utils.SnackbarUtils;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class WakeOnLanTask extends AsyncTask<String, Void, Integer> {

    private final static int WOL_SEND = 0;
    private final static int WOL_SEND_BROADCAST = 1;
    private final static int WOL_INVALID_MAC = 2;
    private final static int WOL_ERROR = 3;

    private final WeakReference<Context> context;
    private final Connection connection;
    private Exception exception;

    public WakeOnLanTask(@NonNull Context context, @NonNull Connection connection) {
        this.context = new WeakReference<>(context);
        this.connection = connection;
    }

    @Override
    protected Integer doInBackground(String... params) {
        // Exit if the MAC address is not ok, this should never happen because
        // it is already validated in the settings
        if (!validateMacAddress(connection.getWolMacAddress())) {
            return WOL_INVALID_MAC;
        }
        // Get the MAC address parts from the string
        byte[] macBytes = getMacBytes(connection.getWolMacAddress());

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
            if (!connection.isWolUseBroadcast()) {
                address = InetAddress.getByName(connection.getHostname());
                Timber.d("Sending WOL packet to " + address);
            } else {
                // Replace the last number by 255 to send the packet as a broadcast
                byte[] ipAddress = InetAddress.getByName(connection.getHostname()).getAddress();
                ipAddress[3] = (byte) 255;
                address = InetAddress.getByAddress(ipAddress);
                Timber.d("Sending WOL packet as broadcast to " + address.toString());
            }
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, connection.getWolPort());
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            if (!connection.isWolUseBroadcast()) {
                return WOL_SEND;
            } else {
                return WOL_SEND_BROADCAST;
            }
        } catch (Exception e) {
            this.exception = e;
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
        String[] hex = macAddress.split("([:\\-])");
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
        Context ctx = context.get();
        if (ctx != null) {
            switch (result) {
                case WOL_SEND:
                    Timber.d("Successfully sent WOL packet to " + connection.getHostname());
                    message = ctx.getString(R.string.wol_send, connection.getHostname());
                    break;
                case WOL_SEND_BROADCAST:
                    Timber.d("Successfully sent WOL packet as a broadcast to " + connection.getHostname());
                    message = ctx.getString(R.string.wol_send_broadcast, connection.getHostname());
                    break;
                case WOL_INVALID_MAC:
                    Timber.d("Can't send WOL packet, the MAC-address is not valid");
                    message = ctx.getString(R.string.wol_address_invalid);
                    break;
                default:
                    Timber.d("Error sending WOL packet to " + connection.getHostname());
                    message = ctx.getString(R.string.wol_error, connection.getHostname(), exception.getLocalizedMessage());
                    break;
            }
        }
        SnackbarUtils.sendSnackbarMessage(ctx, message);
    }
}