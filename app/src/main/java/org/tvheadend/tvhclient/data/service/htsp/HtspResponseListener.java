package org.tvheadend.tvhclient.data.service.htsp;

public interface HtspResponseListener {

    void handleResponse(HtspMessage response);
}