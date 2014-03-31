package org.tvheadend.tvhclient.interfaces;

public interface ProgramGuideInterface {
    public void onScrollPositionChanged(final int index, final int pos);

    public void onScrollStateIdle();

    public int getScrollingSelectionIndex();

    public int getScrollingSelectionPosition();

    boolean isChannelLoadingListEmpty();
}
