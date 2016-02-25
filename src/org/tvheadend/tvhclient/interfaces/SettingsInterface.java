package org.tvheadend.tvhclient.interfaces;

public interface SettingsInterface {

    public void restart();
    public void restartNow();
    public void reconnect();
    public void done(int resultCode);
    
    void showConnections();
    void showAddConnection();
    void showEditConnection(long id);
    void showProfiles();
    void showTranscodingSettings();
    void showNotifications();
}
