package org.tvheadend.tvhclient;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Program;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProgramGuideItemView extends LinearLayout {

    private final static String TAG = ProgramGuideItemView.class.getSimpleName();

    private LinearLayout layout;
    private Activity activity;
    private int tabIndex;
    private Channel channel;
    private int hoursToShow;
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

    private FragmentStatusInterface fragmentStatusInterface;
    private ProgramContextMenuInterface fragmentInterface;

    // Status variables that define where the program is located within the given time.
    private final static int PROGRAM_TIMESLOT_ERROR = 0;
    private final static int PROGRAM_MOVES_INTO_TIMESLOT = 1;
    private final static int PROGRAM_IS_WITHIN_TIMESLOT = 2;
    private final static int PROGRAM_OVERLAPS_TIMESLOT = 3;
    private final static int PROGRAM_MOVES_OUT_OF_TIMESLOT = 4;
    private final static int PROGRAM_OUTSIDE_OF_TIMESLOT = 5;

    private long startTime;
    private long endTime;

    public ProgramGuideItemView(Context context) {
        super(context);
    }

    public ProgramGuideItemView(Activity activity, Fragment fragment, final LinearLayout layout, Bundle bundle) {
        super(activity);
        this.activity = activity;
        this.layout = layout;

        // Create the interface so we can talk to the fragment
        if (activity instanceof FragmentStatusInterface) {
            fragmentStatusInterface = (FragmentStatusInterface) activity;
        }

        fragmentInterface = (ProgramContextMenuInterface) fragment;

        if (bundle != null) {
            hoursToShow = bundle.getInt("hoursToShow", 4);
            tabIndex = bundle.getInt("tabIndex", 0);
            startTime = bundle.getLong("startTime", 0);
            endTime = bundle.getLong("endTime", 0);
        }
        
        pixelsPerMinute = Utils.getPixelsPerMinute(activity, tabIndex, hoursToShow);
        displayWidthRemaining = displayWidth;
    }

    public void addChannel(final Channel ch) {
        channel = ch;
    }

    /**
     * Adds all programs from the program guide to the current view.
     * Only those programs that are within the defined time slot are added. If
     * the last program was reached, a call to load more programs is made.
     */
    public void addPrograms() {

        // Clear all previously shown programs
        layout.removeAllViews();

        // Show that no programs are available
        if (channel == null || channel.epg.isEmpty()) {
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
            // Go through all programs and add them to the view
            synchronized(channel.epg) {
                Iterator<Program> it = channel.epg.iterator();
                Program p = null;
                while (it.hasNext()) {
                    p = it.next();
    
                    // Get the type of the program and add it to the view
                    programType = getProgramType(p);
                    addCurrentProgram(p, programType, programsAddedCounter);
    
    				// Check if the program has been added. Required further down to
    				// check if it was the last one added 
                    programAdded = (programType != PROGRAM_OUTSIDE_OF_TIMESLOT);
                    
    				// Increase the counter which is required to fill in placeholder
    				// programs in case the first program in the guide data is
    				// already within the time slot and not one that moves into one.
                    if (programAdded) {
                    	programsAddedCounter += 1;
                    }
                    // Check if there is more guide data available
                    lastProgramFound = !it.hasNext();
                    
    				// Stop adding more programs if the last program is within the 
    				// time slot and no more data is available or the program added 
    				// is the last one that fits into or overlaps the time slot.
                    if ((programType == PROGRAM_IS_WITHIN_TIMESLOT && lastProgramFound) ||
    	                programType == PROGRAM_MOVES_OUT_OF_TIMESLOT || 
                        programType == PROGRAM_OVERLAPS_TIMESLOT) {
                        break;
                    }
                }
            }
        }
        catch (NoSuchElementException e) {
            Log.e(TAG, "The selected channel contains no programs.");
        }

        // Add the loading indication
        if (lastProgramFound && 
                (programType == PROGRAM_MOVES_INTO_TIMESLOT ||
                 programType == PROGRAM_IS_WITHIN_TIMESLOT || 
                 programType == PROGRAM_OUTSIDE_OF_TIMESLOT)) {
            addLoadingIndication();
        }

        // If the program that was last added was added in the view and it was
        // the last program in the guide then try to load more programs.
        // Also load programs when no program at all was added.
        if ((programAdded && lastProgramFound) || !programAdded) {
            if (fragmentStatusInterface != null) {
                fragmentStatusInterface.moreDataRequired(channel, TAG);
            }
        }
    }

    /**
     * Returns the type of the program with respect to its starting and end
     * times and the given time slot. The program can either be outside of the
     * time, overlap it partly or be within the time.
     * 
     * @param p
     * @return
     */
    private int getProgramType(final Program p) {
        final long programStartTime = p.start.getTime();
        final long programEndTime = p.stop.getTime();

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
        } else {
            // The program start and ends times are not part of the time slot
            return PROGRAM_OUTSIDE_OF_TIMESLOT;
        }
    }

    /**
     * Depending on the given program type a call to the method to get the
     * required width of the program within the view is made. Then the method to
     * add the program is made and the remaining width for the other programs is
     * reduced.
     * 
     * @param program
     * @param programType
     * @param programsAddedCounter
     */
    private void addCurrentProgram(final Program program, final int programType, int programsAddedCounter) {

    	// Calculate the width of the program layout in the view. 
        int width = getProgramLayoutWidth(program, programType);

        switch (programType) {
        case PROGRAM_MOVES_INTO_TIMESLOT:
        	addCurrentProgramToView(program, width, false);
            displayWidthRemaining -= width;
        	break;
        	
        case PROGRAM_IS_WITHIN_TIMESLOT:
			// If this program is the first in the guide data and is already
			// within the time slot it would start somewhere in the middle of
			// the view. So we need to fill in a placeholder program. 
			if (programsAddedCounter == 0) {
				final double durationTime = ((program.start.getTime() - startTime) / 1000 / 60);
				final int w = (int) (durationTime * pixelsPerMinute);
				addCurrentProgramToView(null, w, false);
			}
        	addCurrentProgramToView(program, width, false);
            displayWidthRemaining -= width;
        	break;
        	
        case PROGRAM_MOVES_OUT_OF_TIMESLOT:
			// If this program is the first in the guide data and is already
			// within the time slot it would start somewhere in the middle of
			// the view. So we need to fill in a placeholder program. 
        	if (programsAddedCounter == 0) {
        		final double durationTime = ((program.start.getTime() - startTime) / 1000 / 60);
        		final int w = (int) (durationTime * pixelsPerMinute);
        		addCurrentProgramToView(null, w, false);
        	}
        	// Set the width to the remaining width to indicate for the next
			// program (by the program logic no additional program will be
			// added) there is not space left. The boolean flag will let the
			// layout use the full space instead of the given width.
            if (width >= displayWidthRemaining) {
                width = displayWidthRemaining;
            }
            addCurrentProgramToView(program, width, true);
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
            addCurrentProgramToView(program, width, true);
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
     * @param p
     * @param programType
     * @return
     */
    private int getProgramLayoutWidth(final Program p, final int programType) {
    	final long programStartTime = p.start.getTime();
        final long programEndTime = p.stop.getTime();
        final double durationTime = ((p.stop.getTime() - p.start.getTime()) / 1000 / 60);
        int offset = 0;
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
     * @param p
     * @param layoutWidth
     * @param expandLayout
     */
    private void addCurrentProgramToView(final Program p, final int layoutWidth, final boolean expandLayout) {

    	View v = activity.getLayoutInflater().inflate(R.layout.program_guide_data_item, null);
        final LinearLayout itemLayout = (LinearLayout) v.findViewById(R.id.timeline_item);
        final TextView title = (TextView) v.findViewById(R.id.title);
        final ImageView state = (ImageView) v.findViewById(R.id.state);
        final TextView duration = (TextView) v.findViewById(R.id.time);

        // Set the layout width
        if (layoutWidth > 0) {
            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(layoutWidth, LayoutParams.MATCH_PARENT);
            itemLayout.setLayoutParams(parms);
        }

        // Show the placeholder if there is no program
        if (p == null) {
        	title.setText(R.string.unknown);
        	state.setVisibility(View.GONE);
        	duration.setVisibility(View.GONE);
        }

        if (p != null) {
            Utils.setGenreColor(activity, itemLayout, p.contentType, TAG);
	        itemLayout.setTag(p.id);
	        title.setText(p.title);
	        Utils.setState(state, p.recording);

	        // Only show the duration if the layout is wide enough
	        if (layoutWidth >= MIN_DISPLAY_WIDTH_FOR_DETAILS) {
	            Utils.setDuration(duration, p.start, p.stop);
	        } else {
	            duration.setVisibility(View.GONE);
	        }
	        // Create the context menu so that the user can
	        // record or do other stuff with the selected program
	        itemLayout.setOnCreateContextMenuListener((new OnCreateContextMenuListener() {
	            @Override
	            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	                activity.getMenuInflater().inflate(R.menu.program_context_menu, menu);
	                // Set the title of the context menu and show or hide
	                // the menu items depending on the program state
	                fragmentInterface.setSelectedContextItem(p);
	                menu.setHeaderTitle(p.title);
	                Utils.setProgramMenu(menu, p);
	                
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
                    Program program = null;
                    long id = (Long) v.getTag();
                    for (Program p : channel.epg) {
                        if (p.id == id) {
                            program = p;
                            break;
                        }
                    }

                    if (fragmentStatusInterface != null && program != null) {
                        fragmentStatusInterface.onListItemSelected(0, program, TAG);
                    }

	            }
	        });
        }
        layout.addView(v);
    }

    private void addEmptyProgramToView() {
        View v = inflate(getContext(), R.layout.program_guide_data_item_empty, null);
        layout.addView(v);
    }

    private void addLoadingIndication() {
        View v = inflate(getContext(), R.layout.program_guide_data_item_loading, null);
        layout.addView(v);
    }
    
    public interface ProgramContextMenuInterface {
        public void setSelectedContextItem(Program p);

        public void setMenuSelection(MenuItem item);
    }
}
