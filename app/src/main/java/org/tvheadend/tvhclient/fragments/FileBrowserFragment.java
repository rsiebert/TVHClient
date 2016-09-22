package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.adapter.FileBrowserListAdapter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("deprecation")
public class FileBrowserFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private final static String TAG = FileBrowserFragment.class.getSimpleName();

    private Activity activity;
    private TextView currentPathView;
    private Toolbar toolbar;
    private View toolbarShadow;

    private FileBrowserListAdapter fileListAdapter;
    private RecyclerView fileListView;
    private File basePath = Environment.getExternalStorageDirectory();
    private File selectedPath = Environment.getExternalStorageDirectory();

    private TVHClientApplication app;

    public static FileBrowserFragment newInstance(Bundle args) {
        FileBrowserFragment f = new FileBrowserFragment();
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
            setStyle(DialogFragment.STYLE_NO_TITLE, Utils.getThemeId(activity));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        app = (TVHClientApplication) activity.getApplication();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            String path = savedInstanceState.getString(Constants.BUNDLE_DOWNLOAD_DIR, null);
            if (path != null) {
                selectedPath = new File(path);
            }
        }

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.filebrowser_layout, container, false);
        currentPathView = (TextView) v.findViewById(R.id.path);
        fileListView = (RecyclerView) v.findViewById(R.id.file_list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        fileListView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(activity);
        fileListView.setLayoutManager(layoutManager);
        // TODO add animation upon selection

        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbarShadow = v.findViewById(R.id.toolbar_shadow);
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Constants.BUNDLE_DOWNLOAD_DIR, selectedPath.getAbsolutePath());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toolbar.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);
        toolbarShadow.setVisibility(getDialog() != null ? View.VISIBLE : View.GONE);

        if (currentPathView != null) {
            currentPathView.setText(selectedPath.getAbsolutePath());
        }

        // Setup the menu in the toolbar and the listener to save the selected path
        toolbar.inflateMenu(R.menu.filebrowser_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_select) {
                    saveSelectedDirectory(selectedPath);
                }
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
                return true;
            }
        });

        // Create the adapter that will show the contents of the selected path
        fileListAdapter = new FileBrowserListAdapter(activity, basePath);
        fileListView.setAdapter(fileListAdapter);

        // Only pass on the parent file if the selected path is not the base path.
        // This is the case when the device orientation has changed
        if (basePath.getAbsolutePath().equals(selectedPath.getAbsolutePath())) {
            new FileListLoader().execute(basePath);
        } else {
            new FileListLoader().execute(selectedPath.getParentFile());
        }

        // Setup the listeners so that the user can navigate through the
        // directory structure and also choose a directory by long pressing it
        fileListAdapter.setCallback(new FileBrowserListAdapter.Callback() {
            @Override
            public void onItemClicked(int which) {
                selectedPath = fileListAdapter.getItem(which);

                // Get the contents of the newly selected path and update the adapter
                new FileListLoader().execute(selectedPath);
                if (currentPathView != null) {
                    currentPathView.setText(selectedPath.getAbsolutePath());
                }
            }

            @Override
            public void onItemLongClicked(int which) {
                // Save the selected folder in the preferences
                selectedPath = fileListAdapter.getItem(which);
                saveSelectedDirectory(selectedPath);
            }
        });
    }

    /**
     * Saves the selected directory in the preferences. Additionally remove the
     * external storage path like /storage/emulated/0 from the selected path.
     * It will automatically be prepended by the download manager.
     *
     * @param path The selected path that shall be saved
     */
    private void saveSelectedDirectory(File path) {
        String strippedPath = path.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        app.log(TAG, "Saving the selected path " + strippedPath);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("pref_download_directory", strippedPath).apply();
    }

    /**
     * Creates a list of file objects that are available in the given directory.
     * The found files are only added to the list if they match the filter.
     */
    class FileListLoader extends AsyncTask<File, Void, Void> {

        private final String TAG = FileListLoader.class.getSimpleName();
        private List<File> fl = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            toolbar.setTitle(R.string.loading);
        }

        @Override
        protected Void doInBackground(File... paths) {
            File path = paths[0];

            if (path.exists()) {
                app.log(TAG, "Loading directories from " + path.getAbsolutePath());

                // Create the filter that checks if the file or directory shall be added to the list
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        File f = new File(dir, filename);
                        return ((f.isDirectory() && !f.isHidden()));
                    }
                };

                // Save the contents from the file array into a list
                File[] files = path.listFiles(filter);
                if (files != null) {
                    for (File file : files) {
                        fl.add(file);
                    }
                }

                // Sort the found files alphabetically
                Collections.sort(fl, new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        return file1.getName().compareTo(file2.getName());
                    }
                });

                // Add the parent directory so the user can navigate backwards.
                final File parent = path.getParentFile();
                if (parent != null && !path.getPath().equals(basePath.getAbsolutePath())) {
                    fl.add(0, parent);
                }

                app.log(TAG, "Loaded " + fl.size() + " directories");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            app.log(TAG, "Updating file list adapter");

            toolbar.setTitle("");
            fileListAdapter.setFileList(fl, selectedPath.getParentFile());
            fileListAdapter.notifyDataSetChanged();
        }
    }
}
