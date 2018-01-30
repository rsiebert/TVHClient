package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "connections")
public class Connection {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private int id = 0;
    private String name = "";
    private String hostname = "";
    private int port = 9982;
    private String username = "";
    private String password = "";
    private boolean active = false;
    @ColumnInfo(name = "streaming_port")
    private int streamingPort = 9981;
    @ColumnInfo(name = "wol_hostname")
    private String wolMacAddress = "";
    @ColumnInfo(name = "wol_port")
    private int wolPort = 9;
    @ColumnInfo(name = "wol_use_broadcast")
    private boolean wolUseBroadcast = false;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getStreamingPort() {
        return streamingPort;
    }

    public void setStreamingPort(int streamingPort) {
        this.streamingPort = streamingPort;
    }

    public String getWolMacAddress() {
        return wolMacAddress;
    }

    public void setWolMacAddress(String wolMacAddress) {
        this.wolMacAddress = wolMacAddress;
    }

    public int getWolPort() {
        return wolPort;
    }

    public void setWolPort(int wolPort) {
        this.wolPort = wolPort;
    }

    public boolean isWolUseBroadcast() {
        return wolUseBroadcast;
    }

    public void setWolUseBroadcast(boolean wolUseBroadcast) {
        this.wolUseBroadcast = wolUseBroadcast;
    }
}
