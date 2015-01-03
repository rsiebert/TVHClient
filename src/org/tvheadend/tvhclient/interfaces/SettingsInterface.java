package org.tvheadend.tvhclient.interfaces;

public interface SettingsInterface {

    /**
     * 
     */
    public void reconnect();

    /**
     * 
     */
    public void restart();

    /**
     * 
     */
    public void restartActivity();

    /**
     * 
     */
    public void manageConnections();

    /**
     * 
     * @param pref
     */
    public void showPreference(int pref);
}
