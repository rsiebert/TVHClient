package org.tvheadend.tvhclient.data.source;

class BaseData {
    protected static final int INSERT = 1;
    protected static final int UPDATE = 2;
    protected static final int DELETE = 3;
    protected static final int INSERT_ALL = 4;
    protected static final int DELETE_ALL = 5;
    protected static final int DELETE_BY_TIME = 6;
    protected static final int DELETE_BY_ID = 10;
    protected static final int LOAD_LAST_IN_CHANNEL = 7;
    protected static final int LOAD_BY_ID = 8;
    protected static final int LOAD_BY_EVENT_ID = 9;
}
