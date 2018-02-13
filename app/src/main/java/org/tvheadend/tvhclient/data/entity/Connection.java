package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.text.TextUtils;
import android.util.Patterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity(tableName = "connections")
public class Connection {

    @PrimaryKey(autoGenerate = true)
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
    @ColumnInfo(name = "wol_enabled")
    private boolean wolEnabled = false;
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

    public boolean isWolEnabled() {
        return wolEnabled;
    }

    public void setWolEnabled(boolean wolEnabled) {
        this.wolEnabled = wolEnabled;
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

    public boolean isWolMacAddressValid(String macAddress) {
        // Check if the MAC address is valid
        Pattern pattern = Pattern.compile("([0-9a-fA-F]{2}(?::|-|$)){6}");
        Matcher matcher = pattern.matcher(macAddress);
        return matcher.matches();
    }

    public boolean isNameValid(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        // Check if the name contains only valid characters.
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z_\\-\\.]*$");
        Matcher matcher = pattern.matcher(name);
        return matcher.matches();
    }

    public boolean isIpAddressValid(String address) {
        // Do not allow an empty address
        if (TextUtils.isEmpty(address)) {
            return false;
        }

        // Check if the name contains only valid characters.
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z_\\-\\.]*$");
        Matcher matcher = pattern.matcher(address);
        if (!matcher.matches()) {
            return false;
        }

        // Check if the address has only numbers and dots in it.
        pattern = Pattern.compile("^[0-9\\.]*$");
        matcher = pattern.matcher(address);

        // Now validate the IP address
        if (matcher.matches()) {
            pattern = Patterns.IP_ADDRESS;
            matcher = pattern.matcher(address);
            if (!matcher.matches()) {
                return false;
            }
        }
        return true;
    }

    public boolean isPortValid(int port) {
        return port > 0 && port <= 65535;
    }
}
