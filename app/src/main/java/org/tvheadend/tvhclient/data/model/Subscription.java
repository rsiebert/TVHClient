package org.tvheadend.tvhclient.data.model;

import java.util.ArrayList;
import java.util.List;

public class Subscription {

    public long id;
    public String status;
    public final List<Stream> streams = new ArrayList<>();

    public long packetCount;
    public long queSize;
    public long delay;
    public long droppedBFrames;
    public long droppedIFrames;
    public long droppedPFrames;
    public long graceTimeout;
    public String feStatus;
    public int feSNR;
    public int feSignal;
    public int feBER;
    public int feUNC;
    public SourceInfo sourceInfo;
}
