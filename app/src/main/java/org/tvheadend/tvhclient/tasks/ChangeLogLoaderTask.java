package org.tvheadend.tvhclient.tasks;

import android.content.Context;
import android.os.AsyncTask;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

public class ChangeLogLoaderTask extends AsyncTask<Boolean, Void, String> {

    private final String lastAppVersion;
    private WeakReference<Context> context;
    private final FileLoaderCallback callback;
    private ListMode listMode = ListMode.NONE;
    private StringBuffer stringBuffer = null;

    // modes for HTML-Lists (bullet, numbered)
    private enum ListMode {
        NONE, ORDERED, UNORDERED,
    }

    public ChangeLogLoaderTask(Context context, String lastAppVersion, FileLoaderCallback callback) {
        this.context = new WeakReference<>(context);
        this.lastAppVersion = lastAppVersion;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Boolean... showFullChangeLog) {
        Context ctx = context.get();
        if (ctx != null) {
            return getChangeLogFromFile(ctx, showFullChangeLog[0]);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String content) {
        callback.notify(content);
    }

    private String getChangeLogFromFile(Context context, boolean full) {
        // Add the style sheet depending on the used theme
        stringBuffer = new StringBuffer();
        stringBuffer.append("<html><head>");
        if (MiscUtils.getThemeId(context) == R.style.CustomTheme_Light) {
            stringBuffer.append("<link href=\"html/styles_light.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        } else {
            stringBuffer.append("<link href=\"html/styles_dark.css\" type=\"text/css\" rel=\"stylesheet\"/>");
        }
        stringBuffer.append("</head><body>");

        // read changelog.txt file
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.changelog);
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
