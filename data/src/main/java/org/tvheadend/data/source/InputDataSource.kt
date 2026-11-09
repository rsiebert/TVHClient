package org.tvheadend.data.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.entity.Input

class InputDataSource(@Suppress("unused") private val db: AppRoomDatabase) : DataSourceInterface<Input> {

    private var inputs: MutableList<Input> = ArrayList()

    override fun getLiveDataItemCount(): LiveData<Int> {
        val inputLiveDataCount = MutableLiveData<Int>()
        inputLiveDataCount.value = inputs.size
        return inputLiveDataCount
    }

    override fun getLiveDataItems(): LiveData<List<Input>> {
        val inputLiveData = MutableLiveData<List<Input>>()
        inputLiveData.value = inputs
        return inputLiveData
    }

    override suspend fun getItems(): List<Input> {
        return inputs
    }

    override suspend fun getItemById(id: Any): Input {
        return inputs[id as Int]
    }

    override fun addItem(item: Input) {
        inputs.add(item)
    }

    override fun updateItem(item: Input) {
        // Replace in place. This used to append unconditionally, so updating an entry left the old
        // one behind and added a second copy.
        //
        // Input has no identity field - no id, and uuid is not guaranteed to be populated - and
        // this class has always treated the list position as the key. Equality is therefore the
        // only safe match: replacing on a field would overwrite an unrelated entry whenever that
        // field happens to be blank.
        val index = inputs.indexOf(item)
        if (index >= 0) {
            inputs[index] = item
        } else {
            inputs.add(item)
        }
    }

    override fun removeItem(item: Input) {
        inputs.remove(item)
    }

    fun removeItems() {
        inputs.clear()
    }
}