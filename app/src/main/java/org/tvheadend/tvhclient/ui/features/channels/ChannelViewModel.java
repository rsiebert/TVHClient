package org.tvheadend.tvhclient.ui.features.channels;

import android.app.Application;

import org.tvheadend.tvhclient.domain.entity.Channel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;
import timber.log.Timber;

public class ChannelViewModel extends BaseChannelViewModel {

    private final LiveData<List<Channel>> channels;
    private final LiveData<Integer> channelCount;

    public ChannelViewModel(Application application) {
        super(application);

        channelCount = appRepository.getChannelData().getLiveDataItemCount();

        ChannelLiveData trigger = new ChannelLiveData(selectedTime, channelSortOrder, selectedChannelTagIds);
        channels = Transformations.switchMap(trigger, value -> {
            Timber.d("Loading channels because one of the three triggers have changed");

            if (value.first == null) {
                Timber.d("Skipping loading of channels because selected time is not set");
                return null;
            }
            if (value.second == null) {
                Timber.d("Skipping loading of channels because channel sort order is not set");
                return null;
            }
            if (value.third == null) {
                Timber.d("Skipping loading of channels because selected channel tag ids are not set");
                return null;
            }

            return appRepository.getChannelData().getAllChannelsByTime(value.first, value.second, value.third);
        });
    }

    public LiveData<List<Channel>> getChannels() {
        return channels;
    }

    public LiveData<Integer> getNumberOfChannels() {
        return channelCount;
    }

    class ChannelLiveData extends MediatorLiveData<Triple<Long, Integer, List<Integer>>> {

        ChannelLiveData(LiveData<Long> selectedTime,
                        LiveData<Integer> selectedChannelSortOrder,
                        LiveData<List<Integer>> selectedChannelTagIds) {

            addSource(selectedTime, time ->
                    setValue(Triple.create(time, selectedChannelSortOrder.getValue(), selectedChannelTagIds.getValue()))
            );
            addSource(selectedChannelSortOrder, order ->
                    setValue(Triple.create(selectedTime.getValue(), order, selectedChannelTagIds.getValue()))
            );
            addSource(selectedChannelTagIds, integers ->
                    setValue(Triple.create(selectedTime.getValue(), selectedChannelSortOrder.getValue(), integers))
            );
        }
    }

    /**
     * Container to ease passing around a tuple of three objects. This object provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained
     * objects.
     */
    static class Triple<F, S, T> {
        final F first;
        final S second;
        final T third;

        /**
         * Constructor for a Pair.
         *
         * @param first the first object in the Pair
         * @param second the second object in the pair
         */
        private Triple(F first, S second, T third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        /**
         * Checks the three objects for equality by delegating to their respective
         * {@link Object#equals(Object)} methods.
         *
         * @param o the {@link Triple} to which this one is to be checked for equality
         * @return true if the underlying objects of the Triple are both considered
         *         equal
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Triple)) {
                return false;
            }
            Triple<?, ?, ?> p = (Triple<?, ?, ?>) o;
            return p.first.equals(first)
                    && p.second.equals(second)
                    && p.third.equals(third);
        }

        /**
         * Compute a hash code using the hash codes of the underlying objects
         *
         * @return a hashcode of the Triple
         */
        @Override
        public int hashCode() {
            return (first == null ? 0 : first.hashCode())
                    ^ (second == null ? 0 : second.hashCode())
                    ^ (third == null ? 0 : third.hashCode());
        }

        @Override
        @NonNull
        public String toString() {
            return "Triple{" + String.valueOf(first)
                    + " " + String.valueOf(second)
                    + " " + String.valueOf(third) + "}";
        }

        /**
         * Convenience method for creating an appropriately typed pair.
         * @param a the first object in the Triple
         * @param b the second object in the pair
         * @return a Triple that is templatized with the types of a and b
         */
        public static <A, B, C> Triple <A, B, C> create(A a, B b, C c) {
            return new Triple<>(a, b, c);
        }
    }
}
