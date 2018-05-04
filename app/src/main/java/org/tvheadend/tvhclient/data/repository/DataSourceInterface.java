package org.tvheadend.tvhclient.data.repository;

import android.arch.lifecycle.LiveData;

import java.util.List;

public interface DataSourceInterface<T> {

    void addItem(T item);

    void addItems(List<T> items);

    void updateItem(T item);

    void updateItems(List<T> items);

    void removeItem(T item);

    void removeItems(List<T> items);

    LiveData<Integer> getLiveDataItemCount();

    LiveData<List<T>> getLiveDataItems();

    LiveData<T> getLiveDataItemById(Object id);

    T getItemById(Object id);
}
