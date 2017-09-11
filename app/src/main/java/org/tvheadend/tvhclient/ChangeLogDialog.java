/*
 * Copyright (C) 2013-2014, Robert Siebert
 * Copyright (C) 2011-2013, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 *
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 */
package org.tvheadend.tvhclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.webkit.WebView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.tvheadend.tvhclient.interfaces.ChangeLogDialogInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ChangeLogDialog {

    private final Context context;
    private final String lastVersion;
    private String thisVersion;
    private ChangeLogDialogInterface di;
  
    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";

    private static final String NO_VERSION = "";

    /**
     * Constructor
     * 
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     * 
     * @param context Context
     */
    public ChangeLogDialog(Context context) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
        if (context instanceof ChangeLogDialogInterface) {
        	di = (ChangeLogDialogInterface) context; 
        } else {
        	di = null;
        }
    }

    /**
     * Constructor
     * 
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     * 
     * @param context Context
     * @param sp the shared preferences to store the last version name into
     */
    private ChangeLogDialog(Context context, SharedPreferences sp) {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, NO_VERSION);
        try {
            this.thisVersion = context.getPackageManager()
            		.getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            this.thisVersion = NO_VERSION;
            e.printStackTrace();
        }
    }

    /**
     * @return <code>true</code> if this version of your app is started the
     *         first time
     */
    boolean firstRun() {
        return !this.lastVersion.equals(this.thisVersion);
    }

    /**
     * @return <code>true</code> if your app including ChangeLog is started the
     *         first time ever. Also <code>true</code> if your app was
     *         deinstalled and installed again.
     */
    private boolean firstRunEver() {
        return NO_VERSION.equals(this.lastVersion);
    }

    /**
     * @return An AlertDialog displaying the changes since the previous
     *         installed version of your app (what's new). But when this is the
     *         first run of your app including ChangeLog then the full log
     *         dialog is show.
     */
    MaterialDialog getLogDialog() {
        return this.getDialog(this.firstRunEver());
    }

    /**
     * @return an AlertDialog with a full change log displayed
     */
    public MaterialDialog getFullLogDialog() {
        return this.getDialog(true);
    }

    /**
     * 
     * @param full Show the full changelog or only the latest changes
     * @return MaterialDialog object
     */
    private MaterialDialog getDialog(final boolean full) {

        WebView wv = new WebView(context);
        wv.loadDataWithBaseURL("file:///android_asset/", getLog(full), "text/html", "utf-8", null);

        if (Utils.getThemeId(context) == R.style.CustomTheme_Light) {
            wv.setBackgroundColor(Color.WHITE);
        } else {
            wv.setBackgroundColor(Color.BLACK);
        }

        return new MaterialDialog.Builder(context)
                .title(R.string.pref_changelog)
                .customView(wv, false)
                .positiveText(context.getString(android.R.string.ok))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        updateVersionInPreferences();
                        if (di != null) {
                            di.changeLogDialogDismissed();
                        }
                    }
                })
                .build();
    }

    private void updateVersionInPreferences() {
        // save new version number to preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VERSION_KEY, thisVersion);
        editor.apply();
    }

    /**
     * @return HTML displaying the changes since the previous installed version
     *         of your app (what's new)
     */
    public String getLog() {
        return this.getLog(false);
    }

    /** modes for HTML-Lists (bullet, numbered) */
    private enum Listmode {
        NONE, ORDERED, UNORDERED,
    }

    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;
    private static final String EOCL = "END_OF_CHANGE_LOG";

    private String getLog(boolean full) {
        // Add the style sheet depending on the used theme
        sb = new StringBuffer();
        sb.append("<html><head>");
        if (Utils.getThemeId(context) == R.style.CustomTheme_Light) {
            sb.append("<link href=\"html/styles_light.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        } else {
            sb.append("<link href=\"html/styles_dark.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        }
        sb.append("</head><body>");

        // read changelog.txt file
        try {
            InputStream ins = context.getResources().openRawResource(R.raw.changelog);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line;
            
            // if true: ignore further version sections
            boolean advanceToEOVS = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$') {
                    // begin of a version section
                    this.closeList();
                    String version = line.substring(1).trim();
                    // stop output?
                    if (!full) {
                        if (this.lastVersion.equals(version)) {
                            advanceToEOVS = true;
                        } else if (version.equals(EOCL)) {
                            advanceToEOVS = false;
                        }
                    }
                } else if (!advanceToEOVS) {
                    switch (marker) {
                    case '%':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class=\"title\">");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                        break;
                    case '_':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class=\"subtitle\">");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                        break;
                    case '!':
                        // line contains free text
                        this.closeList();
                        sb.append("<div class=\"content\">");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                        break;
                    case '#':
                        // line contains numbered list item
                        this.openList(Listmode.ORDERED);
                        sb.append("<li class=\"list_content\">");
                        sb.append(line.substring(1).trim());
                        sb.append("</li>\n");
                        break;
                    case '*':
                        // line contains bullet list item
                        this.openList(Listmode.UNORDERED);
                        sb.append("<li class=\"list_content\">");
                        sb.append(line.substring(1).trim());
                        sb.append("</li>\n");
                        break;
                    default:
                        // no special character: just use line as is
                        this.closeList();
                        sb.append(line);
                        sb.append("\n");
                    }
                }
            }
            this.closeList();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sb.append("</body></html>");

        return sb.toString();
    }

    private void openList(Listmode listMode) {
        if (this.listMode != listMode) {
            closeList();
            if (listMode == Listmode.ORDERED) {
                sb.append("<ol>\n");
            } else if (listMode == Listmode.UNORDERED) {
                sb.append("<ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList() {
        if (this.listMode == Listmode.ORDERED) {
            sb.append("</ol>\n");
        } else if (this.listMode == Listmode.UNORDERED) {
            sb.append("</ul>\n");
        }
        this.listMode = Listmode.NONE;
    }
}
