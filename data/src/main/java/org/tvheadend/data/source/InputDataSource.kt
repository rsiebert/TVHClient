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
        inputLiveData.value = getItems()
        return inputLiveData
    }

    override fun getLiveDataItemById(id: Any): LiveData<Input> {
        val inputLiveData = MutableLiveData<Input>()
        inputLiveData.value = getItemById(id)
        return inputLiveData
    }

    override fun getItems(): List<Input> {
        return inputs
    }

    override fun getItemById(id: Any): Input {
        return inputs[id as Int]
    }

    override fun addItem(item: Input) {
        inputs.add(item)
    }

    override fun updateItem(item: Input) {
        inputs.add(item)
    }

    override fun removeItem(item: Input) {
        inputs.remove(item)
    }

    fun removeItems() {
        inputs.clear()
    }
}