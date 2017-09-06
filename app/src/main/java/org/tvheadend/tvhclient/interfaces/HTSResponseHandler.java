package org.tvheadend.tvhclient.interfaces;

import org.tvheadend.tvhclient.htsp.HTSMessage;

public interface HTSResponseHandler {

    void handleResponse(HTSMessage response);
}
