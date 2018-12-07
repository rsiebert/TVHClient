package org.tvheadend.tvhclient.data.source;

class BaseData {
    static final int INSERT = 1;
    static final int UPDATE = 2;
    static final int DELETE = 3;
    static final int INSERT_ALL = 4;
    static final int DELETE_ALL = 5;
    static final int DELETE_BY_TIME = 6;
    static final int DELETE_BY_ID = 10;
    static final int LOAD_LAST_IN_CHANNEL = 7;
    static final int LOAD_BY_ID = 8;
    static final int LOAD_BY_EVENT_ID = 9;
    static final int DELETE_ALL_AND_INSERT = 10;
}
