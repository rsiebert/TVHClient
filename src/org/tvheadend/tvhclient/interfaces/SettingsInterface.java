package org.tvheadend.tvhclient.interfaces;

public interface SettingsInterface {

    public void restart();
    public void restartNow();
    public void reconnect();
    
    void manageConnections();
    void addConnection();
    void editConnection(long id);
}
