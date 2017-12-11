package org.tvheadend.tvhclient.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ProgramGuideChannelListAdapter extends ArrayAdapter<Channel> {

    private final static String TAG = ProgramGuideChannelListAdapter.class.getSimpleName();

    private final Activity context;
    private final List<Channel> list;
    private int selectedPosition = 0;
    private final SharedPreferences prefs;

    public ProgramGuideChannelListAdapter(Activity context, List<Channel> list) {
        super(context, 0);
        this.context = context;
        this.list = list;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void sort(final int type) {
        switch (type) {
            case Constants.CHANNEL_SORT_DEFAULT:
                sort(new Comparator<Channel>() {
                    public int compare(Channel x, Channel y) {
                        if (x != null && y != null) {
                            return x.channelName.compareTo(y.channelName);
                        }
                        return 0;
                    }
                });
                break;
            case Constants.CHANNEL_SORT_BY_NAME:
                sort(new Comparator<Channel>() {
                    public int compare(Channel x, Channel y) {
                        if (x != null && y != null) {
                            return x.channelName.toLowerCase(Locale.US).compareTo(y.channelName.toLowerCase(Locale.US));
                        }
                        return 0;
                    }
                });
                break;
            case Constants.CHANNEL_SORT_BY_NUMBER:
                sort(new Comparator<Channel>() {
                    public int compare(Channel x, Channel y) {
                        if (x != null && y != null) {

                            if (x.channelNumber > y.channelNumber) {
                                return 1;
                            } else if (x.channelNumber < y.channelNumber) {
                                return -1;
                            } else {
                                return 0;
                            }
                        }
                        return 0;
                    }
                });
                break;
        }
    }

    public void setPosition(int pos) {
        selectedPosition = pos;
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView icon_text;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            view = context.getLayoutInflater().inflate(R.layout.program_guide_channel_item, parent, false);
            holder = new ViewHolder();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean bigIcon = prefs.getBoolean("showBigIconPref", false);
            holder.icon = view.findViewById(bigIcon ? R.id.icon_large : R.id.icon);
            holder.icon_text = view.findViewById(bigIcon ? R.id.icon_text_large : R.id.icon_text);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Get the program and assign all the values
        final Channel c = getItem(position);
        if (c != null) {
            // Show the channel icon if available and set in the preferences.
            // If not chosen, hide the imageView and show the channel name.
            final boolean showChannelIcons = prefs.getBoolean("showIconPref", true);
            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, c.channelIcon);
            // Show the icon or a blank one if it does not exist
            holder.icon.setImageBitmap(iconBitmap);
            holder.icon_text.setText(c.channelName);
            // Show the channels icon if set in the preferences.
            // If not then hide the icon and show the channel name as a placeholder
            holder.icon.setVisibility(showChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.icon_text.setVisibility(showChannelIcons ? ImageView.GONE : ImageView.VISIBLE);

            // If activated in the settings allow playing
            // the program by selecting the channel icon
            holder.icon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context instanceof FragmentStatusInterface) {
                        ((FragmentStatusInterface) context).onListItemSelected(position, c, Constants.TAG_CHANNEL_ICON);
                    }
                }
            });

            // If activated in the settings allow playing
            // the program by selecting the channel icon
            holder.icon_text.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context instanceof FragmentStatusInterface) {
                        ((FragmentStatusInterface) context).onListItemSelected(position, c, Constants.TAG_CHANNEL_ICON);
                    }
                }
            });
        }
        return view;
    }

    public void update(Channel c) {
        int length = list.size();
        // Go through the list of channels and find the
        // one with the same id. If its been found, replace it.
        for (int i = 0; i < length; ++i) {
            if (list.get(i).channelId == c.channelId) {
                list.set(i, c);
                break;
            }
        }
    }

    public Channel getSelectedItem() {
        if (list.size() > 0 && list.size() > selectedPosition) {
            return list.get(selectedPosition);
        }
        return null;
    }
}
