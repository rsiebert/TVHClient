package org.tvheadend.tvhclient.ui.epg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.DataStorage;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.ui.programs.ProgramDetailsActivity;
import org.tvheadend.tvhclient.utils.UIUtils;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ProgramGuideViewPagerContentListAdapterContentsView extends LinearLayout {

    private final static String TAG = ProgramGuideViewPagerContentListAdapterContentsView.class.getSimpleName();
    private SharedPreferences sharedPreferences;
    private LinearLayout layout;
    private Activity activity;
    private Channel channel;
    private int displayWidth;

    // Specifies the usable width for the layout of a single program guide item
    // to the entire display width. After each adding of a program item the
    // width will be reduced.
    private int displayWidthRemaining;

    // Only show details like the duration when 
    // the program width in is this wide in pixels
    private final static int MIN_DISPLAY_WIDTH_FOR_DETAILS = 70;

    // The ratio how many minutes a pixel represents on the screen.
    private float pixelsPerMinute;

    private ProgramContextMenuInterface fragmentInterface;

    // Status variables that define where the program is located within the given time.
    private final static int PROGRAM_TIMESLOT_ERROR = 0;
    private final static int PROGRAM_MOVES_INTO_TIMESLOT = 1;
    private final static int PROGRAM_IS_WITHIN_TIMESLOT = 2;
    private final static int PROGRAM_OVERLAPS_TIMESLOT = 3;
    private final static int PROGRAM_MOVES_OUT_OF_TIMESLOT = 4;
    private final static int PROGRAM_BEFORE_TIMESLOT = 5;
    private final static int PROGRAM_AFTER_TIMESLOT = 6;
    private final static int PROGRAM_UNKNOWN_TIMESLOT = 7;
    private long startTime;
    private long endTime;

    public ProgramGuideViewPagerContentListAdapterContentsView(Context context) {
        super(context);
    }

    public ProgramGuideViewPagerContentListAdapterContentsView(Activity activity, Fragment fragment, final LinearLayout layout, long startTime, long endTime, int displayWidth, float pixelsPerMinute) {
        super(activity);
        this.activity = activity;
        this.layout = layout;
        this.fragmentInterface = (ProgramContextMenuInterface) fragment;
        this.displayWidthRemaining = displayWidth;
        this.startTime = startTime;
        this.endTime = endTime;
        this.displayWidth = displayWidth;
        this.pixelsPerMinute = pixelsPerMinute;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    /**
     * Adds all programs from the program guide to the current view.
     * Only those programs that are within the defined time slot are added. If
     * the last program was reached, a call to load more programs is made.
     *
     * @param parent ViewGroup parent required for the layout
     * @param ch     Channel with the available EPG data
     */
    public void addPrograms(ViewGroup parent, List<Program> programList, Channel ch) {
        Log.d(TAG, "addPrograms() called with: parent = [" + parent + "], programList = [" + programList + "], ch = [" + ch + "]");
        channel = ch;
        // Clear all previously shown programs
        layout.removeAllViews();

        // Show that no programs are available
        if (channel == null) {
            addEmptyProgramToView();
            return;
        }

        // Reset the usable width for the layout of a single program guide item
        // to the entire display width. After each adding of a program item the
        // width will be reduced.
        displayWidthRemaining = displayWidth;

        // Indicates that at least one program has been added
        boolean programAdded = false;

        // Indicated that the last program in the list has 
        // been found. More program need to be loaded.
        boolean lastProgramFound = false;

        // Defaults
        int programType = PROGRAM_TIMESLOT_ERROR;
        int programsAddedCounter = 0;

        try {
            Program p;
            Iterator<Program> it = programList.iterator();
            while (it.hasNext()) {
                p = it.next();

                // Get the type of the program and add it to the view
                programType = getProgramType(p);

                // Add the program only when it is somehow within the timeslot.
                if (programType == PROGRAM_MOVES_INTO_TIMESLOT ||
                        programType == PROGRAM_IS_WITHIN_TIMESLOT ||
                        programType == PROGRAM_MOVES_OUT_OF_TIMESLOT ||
                        programType == PROGRAM_OVERLAPS_TIMESLOT) {

                    addCurrentProgram(p, programType, programsAddedCounter, parent);

                    // Increase the counter which is required to fill in placeholder
                    // programs in case the first program in the guide data is
                    // already within the time slot and not one that moves into one.
                    programsAddedCounter++;
                    programAdded = true;
                }

                // Check if there is more guide data available
                lastProgramFound = !it.hasNext();

                // Stop adding more programs if the last program is within the
                // time slot and no more data is available or the program added
                // is the last one that fits into or overlaps the time slot.
                if ((programType == PROGRAM_IS_WITHIN_TIMESLOT && lastProgramFound)
                        || programType == PROGRAM_MOVES_OUT_OF_TIMESLOT
                        || programType == PROGRAM_OVERLAPS_TIMESLOT) {
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            Log.e(TAG, "The selected channel contains no programs.");
        }

        // Add the loading indication only when the channel is not blocked and
        // the program is the last one and overlaps the timeslot somehow. 
        // Otherwise show that no program data is available.
        if (lastProgramFound
                && (programType == PROGRAM_MOVES_INTO_TIMESLOT || programType == PROGRAM_IS_WITHIN_TIMESLOT)) {
            addLoadingIndication();
        } else {
            addEmptyProgramToView();
        }

        // If the program that was last added was added in the view and it was
        // the last program in the guide then try to load more programs.
        // Also load programs when no program at all was added.
        if (!programAdded || lastProgramFound) {
            // TODO load more data for the channel
        }
    }

    /**
     * Returns the type of the program with respect to its starting and end
     * times and the given time slot. The program can either be outside of the
     * time, overlap it partly or be within the time.
     *
     * @param p Program
     * @return Type of program
     */
    private int getProgramType(final Program p) {
        final long programStartTime = p.getStart();
        final long programEndTime = p.getStop();

        if (programStartTime < startTime && programEndTime > startTime && programEndTime < endTime) {
            // The program starts on the previous day and goes over midnight
            // into the current one. The end time must be within this time slot.
            return PROGRAM_MOVES_INTO_TIMESLOT;
        } else if (programStartTime >= startTime && programEndTime <= endTime) {
            // The program is with the current day. The start or end time is
            // within the time slot.
            return PROGRAM_IS_WITHIN_TIMESLOT;
        } else if (programStartTime > startTime && programStartTime < endTime
                && programEndTime > endTime) {
            // The program starts on the current day and moves over midnight
            // into the next one. The start time must be within this time slot
            return PROGRAM_MOVES_OUT_OF_TIMESLOT;
        } else if (programStartTime < startTime && programEndTime > endTime) {
            // The program starts before and ends after the time slot times
            return PROGRAM_OVERLAPS_TIMESLOT;
        } else if (programStartTime < startTime && programEndTime < startTime) {
            // The program starts and ends before the timeslot and is therefore
            // outside of it
            return PROGRAM_BEFORE_TIMESLOT;
        } else if (programStartTime > endTime && programEndTime > programStartTime) {
            // The program start and ends after the timeslot and is therefore
            // outside of it
            return PROGRAM_AFTER_TIMESLOT;
        } else {
            // This should never happen
            return PROGRAM_UNKNOWN_TIMESLOT;
        }
    }

    /**
     * Depending on the given program type a call to the method to loadRecording the
     * required width of the program within the view is made. Then the method to
     * add the program is made and the remaining width for the other programs is
     * reduced.
     *
     * @param program              Program
     * @param programType          Type of program
     * @param programsAddedCounter Number of programs that were added to the view
     */
    private void addCurrentProgram(final Program program, final int programType, int programsAddedCounter, ViewGroup parent) {

        // Calculate the width of the program layout in the view.
        int width = getProgramLayoutWidth(program, programType);

        switch (programType) {
            case PROGRAM_MOVES_INTO_TIMESLOT:
                addCurrentProgramToView(program, width, parent);
                displayWidthRemaining -= width;
                break;

            case PROGRAM_IS_WITHIN_TIMESLOT:
                // If this program is the first in the guide data and is already
                // within the time slot it would start somewhere in the middle of
                // the view. So we need to fill in a placeholder program.
                if (programsAddedCounter == 0) {
                    final double durationTime = ((program.getStart() - startTime) / 1000 / 60);
                    final int w = (int) (durationTime * pixelsPerMinute);
                    addCurrentProgramToView(null, w, parent);
                }
                addCurrentProgramToView(program, width, parent);
                displayWidthRemaining -= width;
                break;

            case PROGRAM_MOVES_OUT_OF_TIMESLOT:
                // If this program is the first in the guide data and is already
                // within the time slot it would start somewhere in the middle of
                // the view. So we need to fill in a placeholder program.
                if (programsAddedCounter == 0) {
                    final double durationTime = ((program.getStart() - startTime) / 1000 / 60);
                    final int w = (int) (durationTime * pixelsPerMinute);
                    addCurrentProgramToView(null, w, parent);
                }
                // Set the width to the remaining width to indicate for the next
                // program (by the program logic no additional program will be
                // added) there is not space left. The boolean flag will let the
                // layout use the full space instead of the given width.
                if (width >= displayWidthRemaining) {
                    width = displayWidthRemaining;
                }
                addCurrentProgramToView(program, width, parent);
                displayWidthRemaining -= width;
                break;

            case PROGRAM_OVERLAPS_TIMESLOT:
                // Set the width to the remaining width to indicate for the next
                // program (by the program logic no additional program will be
                // added) there is not space left. The boolean flag will let the
                // layout use the full space instead of the given width.
                if (width >= displayWidthRemaining) {
                    width = displayWidthRemaining;
                }
                addCurrentProgramToView(program, width, parent);
                displayWidthRemaining -= width;
                break;

            default:
                break;
        }
    }

    /**
     * Calculates from the length of the program the required width in pixels.
     * The factor pixels per minute is also considered which depends on the
     * setting how many hours the current time slot shall show.
     *
     * @param p           Program
     * @param programType Type of program
     * @return Widht in pixels that a program shall take up in the EPG view
     */
    private int getProgramLayoutWidth(final Program p, final int programType) {
        final long programStartTime = p.getStart();
        final long programEndTime = p.getStop();
        final double durationTime = ((p.getStop() - p.getStart()) / 1000 / 60);
        int offset;
        int width = 0;

        switch (programType) {
            case PROGRAM_MOVES_INTO_TIMESLOT:
                offset = (int) (durationTime - ((startTime - programStartTime) / 1000 / 60));
                width = (int) (offset * pixelsPerMinute);
                break;

            case PROGRAM_IS_WITHIN_TIMESLOT:
                width = (int) (durationTime * pixelsPerMinute);
                break;

            case PROGRAM_MOVES_OUT_OF_TIMESLOT:
                offset = (int) (durationTime - ((programEndTime - endTime)) / 1000 / 60);
                width = (int) (offset * pixelsPerMinute);
                break;

            case PROGRAM_OVERLAPS_TIMESLOT:
                offset = (int) (durationTime - ((programEndTime - endTime)) / 1000 / 60);
                width = (int) (offset * pixelsPerMinute);
                break;

            default:
                break;
        }
        return width;
    }

    /**
     * Creates the views and layout for the given program and adds all required
     * information to it.
     *
     * @param p           Program
     * @param layoutWidth Width in pixels of the layout
     */
    private void addCurrentProgramToView(final Program p, final int layoutWidth, ViewGroup parent) {

        View v = activity.getLayoutInflater().inflate(R.layout.program_guide_program_contents, parent, false);
        final LinearLayout itemLayout = v.findViewById(R.id.timeline_item);
        final TextView title = v.findViewById(R.id.title);
        final ImageView state = v.findViewById(R.id.state);
        final TextView duration = v.findViewById(R.id.time);

        // Set the layout width
        if (layoutWidth > 0) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(layoutWidth, LayoutParams.MATCH_PARENT);
            itemLayout.setLayoutParams(params);
        }

        boolean showGenreColors = sharedPreferences.getBoolean("showGenreColorsGuidePref", false);

        // Show the placeholder if there is no program
        if (p == null) {
            title.setText(R.string.unknown);
            state.setVisibility(View.GONE);
            duration.setVisibility(View.GONE);
        }

        if (p != null) {

            if (showGenreColors) {
                // Offset that reduces the visibility of the program guide colors a little
                int color = UIUtils.getGenreColor(activity, p.getContentType(), 50);
                LayerDrawable layers = (LayerDrawable) itemLayout.getBackground();
                GradientDrawable shape = (GradientDrawable) (layers.findDrawableByLayerId(R.id.timeline_item_genre));
                shape.setColor(color);
            }

            itemLayout.setTag(p.getEventId());
            title.setText(p.getTitle());

            Drawable drawable = UIUtils.getRecordingState(activity, p.getDvrId());
            state.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
            state.setImageDrawable(drawable);

            // Only show the duration if the layout is wide enough
            if (layoutWidth >= MIN_DISPLAY_WIDTH_FOR_DETAILS) {
                String durationTime = activity.getString(R.string.minutes, (int) ((p.getStop() - p.getStart()) / 1000 / 60));
                duration.setText(durationTime);
            } else {
                duration.setVisibility(View.GONE);
            }
            // Create the context menu so that the user can
            // record or do other stuff with the selected program
            itemLayout.setOnCreateContextMenuListener((new OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                    activity.getMenuInflater().inflate(R.menu.channel_list_program_popup_menu, menu);
                    // Set the title of the context menu and show or hide
                    // the menu items depending on the program state
                    fragmentInterface.setSelectedContextItem(p);
                    menu.setHeaderTitle(p.getTitle());
                    //Utils.setProgramMenu(app, menu, p);

                    // Add a listener to each menu item. When the menu item is
                    // called the context handler method from the fragment will
                    // be called. Without this the context menu handler from the
                    // channel list fragment was called (not clear why) which
                    // resulted in a null pointer exception.
                    int size = menu.size();
                    for (int i = 0; i < size; ++i) {
                        menu.getItem(i).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                fragmentInterface.setMenuSelection(item);
                                return true;
                            }
                        });
                    }
                }
            }));

            // Add the listener to the layout so that a
            // click will show the program details activity
            itemLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (channel == null) {
                        return;
                    }
                    // We only have saved the id of the program, so go through
                    // all program in the current channel until we find the one.
                    int id = (int) v.getTag();
                    Program program = DataStorage.getInstance().getProgramFromArray(id);
                    Intent intent = new Intent(activity, ProgramDetailsActivity.class);
                    intent.putExtra("eventId", program.getEventId());
                    intent.putExtra("type", "program");
                    activity.startActivity(intent);
                }
            });
        }
        layout.addView(v);
    }

    private void addEmptyProgramToView() {
        View v = inflate(getContext(), R.layout.program_guide_program_empty, null);
        final LinearLayout itemLayout = v.findViewById(R.id.timeline_item);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(displayWidth, LayoutParams.MATCH_PARENT);
        itemLayout.setLayoutParams(parms);
        layout.addView(v);
    }

    private void addLoadingIndication() {
        View v = inflate(getContext(), R.layout.program_guide_program_loading, null);
        layout.addView(v);
    }

    public interface ProgramContextMenuInterface {
        void setSelectedContextItem(Program p);

        void setMenuSelection(MenuItem item);
    }
}
