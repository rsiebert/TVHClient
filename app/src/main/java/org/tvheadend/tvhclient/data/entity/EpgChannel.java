package org.tvheadend.tvhclient.data.entity;

import android.arch.persistence.room.ColumnInfo;

public class EpgChannel {

    @ColumnInfo(name = "id")
    private int id;
    @ColumnInfo(name = "number")
    private int number;
    @ColumnInfo(name = "number_minor")
    private int numberMinor;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "icon")
    private String icon;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getNumberMinor() {
        return numberMinor;
    }

    public void setNumberMinor(int numberMinor) {
        this.numberMinor = numberMinor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
