package org.tvheadend.tvhclient.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
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

    // TODO use a model instead of a list of files

    private Activity activity;
    private TextView currentPathView;
    private Toolbar toolbar;
    private View toolbarShadow;
    private List<File> fileList = new ArrayList<>();

    private FileBrowserListAdapter fileListAdapter;
    private RecyclerView fileListView;
    private RecyclerView.LayoutManager layoutManager;
    private File basePath = Environment.getExternalStorageDirectory();
    private File selectedPath = Environment.getExternalStorageDirectory();

    public static FileBrowserFragment newInstance(Bundle args) {
        FileBrowserFragment f = new FileBrowserFragment();
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        // TODO min height and width

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
            setStyle(DialogFragment.STYLE_NO_TITLE, Utils.getThemeId(activity));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView");

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
        layoutManager = new LinearLayoutManager(activity);
        fileListView.setLayoutManager(layoutManager);
        // TODO add animation upon selection

        // TODO check if the shadow can be removed
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbarShadow = v.findViewById(R.id.toolbar_shadow);
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
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

        // Fill the adapter with the found files and directories
        createFileList(selectedPath);
        fileListAdapter.setFileList(getFileList(), selectedPath);
        fileListAdapter.notifyDataSetChanged();

        // Setup the listeners so that the user can navigate through the
        // directory structure and also choose a directory by long pressing it
        fileListAdapter.setCallback(new FileBrowserListAdapter.Callback() {
            @Override
            public void onItemClicked(int which) {
                selectedPath = fileList.get(which);

                // Get the contents of the newly selected path and update the adapter
                createFileList(selectedPath);
                fileListAdapter.setFileList(getFileList(), selectedPath.getParentFile());
                fileListAdapter.notifyDataSetChanged();
                if (currentPathView != null) {
                    currentPathView.setText(selectedPath.getAbsolutePath());
                }
            }

            @Override
            public void onItemLongClicked(int which) {
                // Save the selected folder in the preferences
                selectedPath = fileList.get(which);
                saveSelectedDirectory(selectedPath);
            }
        });
    }

    /**
     *
     * @param path
     */
    private void saveSelectedDirectory(File path) {
        // Remove the external storage path like /storage/emulated/0 from the selected path.
        // It will automatically be prepended by the download manager.
        String strippedPath = path.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        Log.d(TAG, "Saving the selected path " + strippedPath);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString("pref_download_directory", strippedPath).apply();
    }

    /**
     * Returns the list of file objects that were found in the given directory
     *
     * @return fileList List of found files
     */
    private List<File> getFileList() {
        return this.fileList;
    }

    /**
     * Creates a list of file objects that are available in the given directory.
     * The found files are only added to the list if they match the filter.
     *
     * @param path The directory that shall be searched
     */
    private void createFileList(File path) {
        this.fileList.clear();

        if (path.exists()) {
            Log.d(TAG, "Filling list from path " + path.getAbsolutePath());

            // Create the filter that checks if the file or directory shall be added to the list
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File f = new File(dir, filename);
                    if (f.isDirectory() && !f.isHidden()) {
                        return true;
                    }
                    return false;
                }
            };

            // Save the contents from the file array into a list
            File[] files = path.listFiles(filter);
            if (files != null) {
                for (File file : files) {
                    fileList.add(file);
                }
            }

            // Sort the found files alphabetically
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    return file1.getName().compareTo(file2.getName());
                }
            });

            // Add the parent directory so the user can navigate backwards.
            final File parent = path.getParentFile();
            if (parent != null && !path.getPath().equals(basePath.getAbsolutePath())) {
                fileList.add(0, parent);
            }
        }
    }

}
