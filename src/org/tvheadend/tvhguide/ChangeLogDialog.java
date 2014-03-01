/**
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
package org.tvheadend.tvhguide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;

public class ChangeLogDialog {

    private final Context context;
    private String lastVersion, thisVersion;
    private ChangeLogDialogInterface di;

    private String styleLight = 
              "div.title { color:#AAAAAA; font-size:1.2em; margin-left:0.4em; margin-top:0.7em; margin-bottom:0.2em; text-align:left } "
            + "div.subtitle {color:#AAAAAA; margin-left:0.6em; margin-bottom:0.0em; text-align:left }"
            + "div.freetext { color:#333333 }"
            + "div.list     { color:#333333 }";
    
    private String styleDark = 
              "div.title { color:#DDDDDD; font-size:1.2em; margin-left:0.4em; margin-top:0.7em; margin-bottom:0.2em; text-align:left } "
            + "div.subtitle {color:#DDDDDD; margin-left:0.6em; margin-bottom:0.0em; text-align:left }"
            + "div.freetext { color:#AAAAAA }"
            + "div.list     { color:#AAAAAA }";
  
    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";

    private static final String NO_VERSION = "";

    /**
     * Constructor
     * 
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     * 
     * @param context
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
     * @param context
     * @param sp the shared preferences to store the last version name into
     */
    public ChangeLogDialog(Context context, SharedPreferences sp) {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, NO_VERSION);
        Log.d(TAG, "lastVersion: " + lastVersion);
        try {
            this.thisVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e) {
            this.thisVersion = NO_VERSION;
            Log.e(TAG, "could not get version name from manifest!");
            e.printStackTrace();
        }
        Log.d(TAG, "appVersion: " + this.thisVersion);
    }

    /**
     * @return The version name of the last installation of this app (as
     *         described in the former manifest). This will be the same as
     *         returned by <code>getThisVersion()</code> the second time this
     *         version of the app is launched (more precisely: the second time
     *         ChangeLog is instantiated).
     * @see AndroidManifest.xml#android:versionName
     */
    public String getLastVersion() {
        return this.lastVersion;
    }

    /**
     * @return The version name of this app as described in the manifest.
     * @see AndroidManifest.xml#android:versionName
     */
    public String getThisVersion() {
        return this.thisVersion;
    }

    /**
     * @return <code>true</code> if this version of your app is started the
     *         first time
     */
    public boolean firstRun() {
        return !this.lastVersion.equals(this.thisVersion);
    }

    /**
     * @return <code>true</code> if your app including ChangeLog is started the
     *         first time ever. Also <code>true</code> if your app was
     *         deinstalled and installed again.
     */
    public boolean firstRunEver() {
        return NO_VERSION.equals(this.lastVersion);
    }

    /**
     * @return An AlertDialog displaying the changes since the previous
     *         installed version of your app (what's new). But when this is the
     *         first run of your app including ChangeLog then the full log
     *         dialog is show.
     */
    public AlertDialog getLogDialog() {
        return this.getDialog(this.firstRunEver());
    }

    /**
     * @return an AlertDialog with a full change log displayed
     */
    public AlertDialog getFullLogDialog() {
        return this.getDialog(true);
    }

    private AlertDialog getDialog(boolean full) {
        WebView wv = new WebView(this.context);

        if (Utils.getThemeId(context) == R.style.CustomTheme_Light)
            wv.setBackgroundColor(Color.WHITE);
        else
            wv.setBackgroundColor(Color.BLACK);
        
        wv.loadDataWithBaseURL(null, this.getLog(full), "text/html", "UTF-8", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle(context.getResources().getString(R.string.pref_changelog));
        builder.setView(wv);
        builder.setCancelable(false);
        // OK button
        builder.setPositiveButton(context.getResources().getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateVersionInPreferences();
                        if (di != null) {
                        	di.dialogDismissed();
                        }
                    }
                });

        return builder.create();
    }

    private void updateVersionInPreferences() {
        // save new version number to preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VERSION_KEY, thisVersion);
        editor.commit();
    }

    /**
     * @return HTML displaying the changes since the previous installed version
     *         of your app (what's new)
     */
    public String getLog() {
        return this.getLog(false);
    }

    /**
     * @return HTML which displays full change log
     */
    public String getFullLog() {
        return this.getLog(true);
    }

    /** modes for HTML-Lists (bullet, numbered) */
    private enum Listmode {
        NONE, ORDERED, UNORDERED,
    };

    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;
    private static final String EOCL = "END_OF_CHANGE_LOG";

    private String getLog(boolean full) {
        // Add the style sheet depending on the used theme
        sb = new StringBuffer();
        sb.append("<html><head><style type=\'text/css\'>");
        
        if (Utils.getThemeId(context) == R.style.CustomTheme_Light) {
            sb.append(styleLight);
        } else {
            sb.append(styleDark);
        }

        sb.append("</style></head><body>");

        // read changelog.txt file
        try {
            InputStream ins = context.getResources().openRawResource(R.raw.changelog);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));

            String line = null;
            
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
                        }
                        else if (version.equals(EOCL)) {
                            advanceToEOVS = false;
                        }
                    }
                }
                else if (!advanceToEOVS) {
                    switch (marker) {
                    case '%':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='title'>" + line.substring(1).trim() + "</div>\n");
                        break;
                    case '_':
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='subtitle'>" + line.substring(1).trim() + "</div>\n");
                        break;
                    case '!':
                        // line contains free text
                        this.closeList();
                        sb.append("<div class='freetext'>" + line.substring(1).trim() + "</div>\n");
                        break;
                    case '#':
                        // line contains numbered list item
                        this.openList(Listmode.ORDERED);
                        sb.append("<li>" + line.substring(1).trim() + "</li>\n");
                        break;
                    case '*':
                        // line contains bullet list item
                        this.openList(Listmode.UNORDERED);
                        sb.append("<li>" + line.substring(1).trim() + "</li>\n");
                        break;
                    default:
                        // no special character: just use line as is
                        this.closeList();
                        sb.append(line + "\n");
                    }
                }
            }
            this.closeList();
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        sb.append("</body></html>");

        return sb.toString();
    }

    private void openList(Listmode listMode) {
        if (this.listMode != listMode) {
            closeList();
            if (listMode == Listmode.ORDERED) {
                sb.append("<div class='list'><ol>\n");
            }
            else if (listMode == Listmode.UNORDERED) {
                sb.append("<div class='list'><ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList() {
        if (this.listMode == Listmode.ORDERED) {
            sb.append("</ol></div>\n");
        }
        else if (this.listMode == Listmode.UNORDERED) {
            sb.append("</ul></div>\n");
        }
        this.listMode = Listmode.NONE;
    }

    private static final String TAG = "ChangeLog";

    /**
     * manually set the last version name - for testing purposes only
     * 
     * @param lastVersion
     */
    public void dontuseSetLastVersion(String lastVersion) {
        this.lastVersion = lastVersion;
    }
    
    public interface ChangeLogDialogInterface {
        public void dialogDismissed();
    }
}
