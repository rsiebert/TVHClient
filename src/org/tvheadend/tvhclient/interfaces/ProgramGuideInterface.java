package org.tvheadend.tvhclient.interfaces;

public interface ProgramGuideInterface {
    public void onScrollingChanged(final int index, final int pos, final String tag);

    public void onScrollStateIdle(final String tag);

    public int getScrollingSelectionIndex();

    public int getScrollingSelectionPosition();

    boolean isChannelLoadingListEmpty();
}
