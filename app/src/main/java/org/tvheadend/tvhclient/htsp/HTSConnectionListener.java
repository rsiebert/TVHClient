package org.tvheadend.tvhclient.htsp;

import org.tvheadend.tvhclient.htsp.HTSMessage;

public interface HTSConnectionListener {

    /**
     * This method is invoked by the HTSConnection class. Whenever the server
     * sends a new message the listeners will be informed.
     * 
     * @param response Response from the server
     */
    void onMessage(HTSMessage response);

    /**
     * This method is invoked by the HTSConnection class. Whenever the server
     * encounters an error or sends one the listeners will be informed.
     * 
     * @param error Error message
     */
    void onError(String error);
}
