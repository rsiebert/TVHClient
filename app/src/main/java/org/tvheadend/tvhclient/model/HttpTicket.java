package org.tvheadend.tvhclient.model;

public class HttpTicket {
    public final String path;
    public final String ticket;
    
    public HttpTicket(String path, String ticket) {
        this.path = path;
        this.ticket = ticket;
    }
}
