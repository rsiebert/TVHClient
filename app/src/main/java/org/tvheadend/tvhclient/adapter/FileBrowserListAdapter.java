package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.Utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class FileBrowserListAdapter extends RecyclerView.Adapter<FileBrowserListAdapter.ViewHolder> {
    private final Activity activity;
    private File basePath;
    private Callback mCallback;
    private List<File> list;

    public FileBrowserListAdapter(Activity activity, File basePath) {
        this.activity = activity;
        this.basePath = basePath;
    }

    public File getItem(int position) {
        return this.list.get(position);
    }

    public interface Callback {
        void onItemClicked(int adapterPosition);
        void onItemLongClicked(int adapterPosition);
    }

    public void setFileList(List<File> list, File basePath) {
        this.list = list;
        this.basePath = basePath;
    }

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.filebrowser_list_item, parent, false);
        return new ViewHolder(view, this);
    }

    @Override
    /**
     * Applies the values to the available layout items
     */
    public void onBindViewHolder(ViewHolder holder, int position) {
        final boolean lightTheme = (Utils.getThemeId(activity) == R.style.CustomTheme_Light);
        final File item = list.get(position);
        if (item != null) {
            if (holder.icon != null) {
                if (item.isFile()) {
                    holder.icon.setImageResource(lightTheme ? R.drawable.ic_file_light : R.drawable.ic_file_dark);
                } else if (item.isDirectory()) {
                    holder.icon.setImageResource(lightTheme ? R.drawable.ic_folder_light : R.drawable.ic_folder_dark);
                }
            }

            if (holder.title != null) {
                // Show the option to navigate up if the first item is the base path.
                if (position == 0 && item.getAbsolutePath().equals(basePath.getAbsolutePath())) {
                    holder.title.setText("..");
                } else {
                    holder.title.setText(item.getName());
                }
                holder.title.setTag(position);
            }

            if (holder.itemCount != null) {
                // Do not show the number of items for the parent directory
                if (position == 0 && item.getAbsolutePath().equals(basePath.getAbsolutePath())) {
                    holder.itemCount.setText(R.string.parent_directory);
                } else {
                    FilenameFilter filter = new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            File f = new File(dir, filename);
                            return (f.isDirectory() && !f.isHidden());
                        }
                    };

                    File[] files = item.listFiles(filter);
                    final int count = (files != null) ? files.length : 0;
                    holder.itemCount.setText(activity.getResources().getQuantityString(R.plurals.items, count, count));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return (list != null ? list.size() : 0);
    }

    /**
     * Provide a reference to the views for each data item
     * Complex data items may need more than one view per item, and
     * you provide access to all the views for a data item in a view holder
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        final ImageView icon;
        final TextView title;
        final TextView itemCount;
        final FileBrowserListAdapter adapter;

        public ViewHolder(View view, FileBrowserListAdapter adapter) {
            super(view);
            this.icon = (ImageView) view.findViewById(R.id.icon);
            this.title = (TextView) view.findViewById(R.id.title);
            this.itemCount = (TextView) view.findViewById(R.id.item_count);

            this.adapter = adapter;
            view.setClickable(true);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (adapter.mCallback == null) {
                return;
            }
            adapter.mCallback.onItemClicked(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            if (adapter.mCallback == null) {
                return false;
            }
            adapter.mCallback.onItemLongClicked(getAdapterPosition());
            return true;
        }
    }
}
