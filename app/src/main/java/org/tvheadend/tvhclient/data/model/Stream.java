package org.tvheadend.tvhclient.data.model;

public class Stream {

    public static final String STREAM_TYPE_AC3 = "AC3";
    public static final String STREAM_TYPE_MPEG2AUDIO = "MPEG2AUDIO";
    public static final String STREAM_TYPE_MPEG2VIDEO = "MPEG2VIDEO";
    public static final String STREAM_TYPE_MPEG4VIDEO = "MPEG4VIDEO";
    public static final String STREAM_TYPE_H264 = "H264";
    public static final String STREAM_TYPE_VP8 = "VP8";
    public static final String STREAM_TYPE_AAC = "AAC";
    public int index;
    public String type;
    public String language;
    public int height;
    public int width;
    public int duration;
    public int aspectNum;
    public int aspectDen;
    public int autioType;
    public int channels;
    public int rate;
}
