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
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.utils.MiscUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProgramGuideChannelListAdapter extends ArrayAdapter<Channel> implements OnClickListener {

    private final static String TAG = ProgramGuideChannelListAdapter.class.getSimpleName();

    private final Activity context;
    private final List<Channel> list;
    private int selectedPosition = 0;
    private final SharedPreferences sharedPreferences;

    public ProgramGuideChannelListAdapter(Activity context, List<Channel> list) {
        super(context, R.layout.program_guide_channel_item, list);
        this.context = context;
        this.list = list;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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
        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.icon_text)
        TextView icon_text;
        @BindView(R.id.icon_large)
        ImageView icon_large;
        @BindView(R.id.icon_text_large)
        TextView icon_text_large;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @NonNull
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = context.getLayoutInflater().inflate(R.layout.channel_list_widget, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        // Get the program and assign all the values
        final Channel c = getItem(position);
        if (c != null) {

            boolean showChannelIcons = sharedPreferences.getBoolean("showIconPref", true);
            boolean showLargeChannelIcons = sharedPreferences.getBoolean("showBigIconPref", false);

            // Show the regular or large channel icons. Otherwise show the channel name only
            // Assign the channel icon image or a null image
            Bitmap iconBitmap = MiscUtils.getCachedIcon(context, c.channelIcon);
            holder.icon.setImageBitmap(iconBitmap);
            holder.icon_large.setImageBitmap(iconBitmap);
            holder.icon_text.setText(c.channelName);
            holder.icon_text_large.setText(c.channelName);

            // Show or hide the regular or large channel icon or name text views
            holder.icon.setVisibility(showChannelIcons && !showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.icon_text.setVisibility(!showChannelIcons && !showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.icon_large.setVisibility(showChannelIcons && showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);
            holder.icon_text_large.setVisibility(!showChannelIcons && showLargeChannelIcons ? ImageView.VISIBLE : ImageView.GONE);

            // If activated in the settings allow playing
            // the program by selecting the channel icon
            holder.icon.setOnClickListener(this);
            holder.icon_large.setOnClickListener(this);
            holder.icon_text.setOnClickListener(this);
            holder.icon_text_large.setOnClickListener(this);
        }
        return view;
    }

    @Override
    public void onClick(View view) {
        if (sharedPreferences.getBoolean("playWhenChannelIconSelectedPref", true)) {
            Channel channel = getSelectedItem();
            new MenuUtils(context).handleMenuPlaySelection(channel.channelId, -1);
        }
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
