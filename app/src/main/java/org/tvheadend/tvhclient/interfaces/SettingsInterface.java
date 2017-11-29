package org.tvheadend.tvhclient.interfaces;

public interface SettingsInterface {

    void restart();
    void restartNow();
    void reconnect();
    
    void showConnections();
    void showAddConnection();
    void showEditConnection(long id);
    void showProfiles();
    void showTranscodingSettings();
    void showNotifications();
    void showCasting();
    void showUserInterface();
    void showAdvanced();
}
