package org.tvheadend.tvhclient.interfaces;

public interface SettingsInterface {

    public void restart();
    public void restartNow();
    public void reconnect();
    
    public void manageConnections();
    public void addConnection();
    public void editConnection(long id);
    public void mainSettingsGenreColors();
    public void mainSettingsProgramGuide();
    public void mainSettingsMenuVisibility();
    public void mainSettingsPlaybackPrograms();
    public void mainSettingsPlaybackRecordings();
}
