package org.tvheadend.tvhclient.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.tvheadend.tvhclient.BuildConfig;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.activities.SettingsToolbarInterface;
import org.tvheadend.tvhclient.interfaces.BackPressedInterface;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ChangeLogFragment extends android.app.Fragment implements BackPressedInterface {
    private static final String TAG = ChangeLogFragment.class.getSimpleName();

    private WebView webView;
    private ListMode listMode = ListMode.NONE;
    private StringBuffer stringBuffer = null;
    private String lastAppVersion;
    private boolean showFullChangeLog = false;

    // modes for HTML-Lists (bullet, numbered)
    private enum ListMode {
        NONE, ORDERED, UNORDERED,
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.webview_layout, null);
        webView = view.findViewById(R.id.webview);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof SettingsToolbarInterface) {
            SettingsToolbarInterface toolbarInterface = (SettingsToolbarInterface) getActivity();
            // TODO change to string
            toolbarInterface.setTitle(getString(R.string.pref_changelog));
        }

        setHasOptionsMenu(true);

        // Get the build version where the changelog was last shown
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        lastAppVersion = sharedPreferences.getString("app_version_name_for_changelog", "");

        // Show the full changelog if the changelog was never shown before (app version
        // name is empty) or if it was already shown and the version name is the same as
        // the one in the preferences. Otherwise show the changelog of the newest app version.
        showFullChangeLog = (lastAppVersion.isEmpty() || lastAppVersion.equals(BuildConfig.VERSION_NAME));
        showChangelog(showFullChangeLog);
    }

    private void showChangelog(boolean showFullChangeLog) {
        // TODO put the loading stuff into a separate task
        String changes = getChangeLogFromFile(showFullChangeLog);
        webView.loadDataWithBaseURL("file:///android_asset/", changes, "text/html", "utf-8", null);
        // TODO assign a style to the webview
        if (MiscUtils.getThemeId(getActivity()) == R.style.CustomTheme_Light) {
            webView.setBackgroundColor(Color.WHITE);
        } else {
            webView.setBackgroundColor(Color.BLACK);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        (menu.findItem(R.id.menu_full_changelog)).setVisible(!showFullChangeLog);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_changelog, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_full_changelog:
                showChangelog(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() called");
        // Save the information that the changelog was shown
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("app_version_name_for_changelog", BuildConfig.VERSION_NAME);
        editor.apply();
    }

    private String getChangeLogFromFile(boolean full) {
        // Add the style sheet depending on the used theme
        stringBuffer = new StringBuffer();
        stringBuffer.append("<html><head>");
        if (MiscUtils.getThemeId(getActivity()) == R.style.CustomTheme_Light) {
            stringBuffer.append("<link href=\"html/styles_light.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        } else {
            stringBuffer.append("<link href=\"html/styles_dark.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        }
        stringBuffer.append("</head><body>");

        // read changelog.txt file
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.changelog);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            // if true: ignore further version sections
            boolean advanceToEOVS = false;

            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$') {
                    // begin of a version section
                    closeList();
                    String version = line.substring(1).trim();
                    // stop output?
                    if (!full) {
                        if (lastAppVersion.equals(version)) {
                            advanceToEOVS = true;
                        } else if (version.equals("END_OF_CHANGE_LOG")) {
                            advanceToEOVS = false;
                        }
                    }
                } else if (!advanceToEOVS) {
                    switch (marker) {
                        case '%':
                            // line contains version title
                            closeList();
                            stringBuffer.append("<div class=\"title\">");
                            stringBuffer.append(line.substring(1).trim());
                            stringBuffer.append("</div>\n");
                            break;
                        case '_':
                            // line contains version title
                            closeList();
                            stringBuffer.append("<div class=\"subtitle\">");
                            stringBuffer.append(line.substring(1).trim());
                            stringBuffer.append("</div>\n");
                            break;
                        case '!':
                            // line contains free text
                            closeList();
                            stringBuffer.append("<div class=\"content\">");
                            stringBuffer.append(line.substring(1).trim());
                            stringBuffer.append("</div>\n");
                            break;
                        case '#':
                            // line contains numbered list item
                            openList(ListMode.ORDERED);
                            stringBuffer.append("<li class=\"list_content\">");
                            stringBuffer.append(line.substring(1).trim());
                            stringBuffer.append("</li>\n");
                            break;
                        case '*':
                            // line contains bullet list item
                            openList(ListMode.UNORDERED);
                            stringBuffer.append("<li class=\"list_content\">");
                            stringBuffer.append(line.substring(1).trim());
                            stringBuffer.append("</li>\n");
                            break;
                        default:
                            // no special character: just use line as is
                            closeList();
                            stringBuffer.append(line);
                            stringBuffer.append("\n");
                    }
                }
            }
            closeList();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stringBuffer.append("</body></html>");
        return stringBuffer.toString();
    }

    private void openList(ListMode listMode) {
        if (this.listMode != listMode) {
            closeList();
            if (listMode == ListMode.ORDERED) {
                stringBuffer.append("<ol>\n");
            } else if (listMode == ListMode.UNORDERED) {
                stringBuffer.append("<ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList() {
        if (listMode == ListMode.ORDERED) {
            stringBuffer.append("</ol>\n");
        } else if (listMode == ListMode.UNORDERED) {
            stringBuffer.append("</ul>\n");
        }
        listMode = ListMode.NONE;
    }
}
