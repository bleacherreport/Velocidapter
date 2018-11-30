package com.bleacherreport.velocidapterandroid

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Implemented by adapters to abstract away non-data logic. If not using LiveData, hold a reference
 * to this class instead of the Adapter
 */
interface AdapterDataTarget<T : ScopedDataList> {
    fun updateDataset(newDataset: T)
    fun resetData(newDataset: T)
    fun setEmpty()
    var shouldRunDiff: Boolean
}

/**
 * Enables DiffUtil updates on target, must be called before first data is set on target
 *
 * Note: All classes contained in the data set must implement DiffComparable, or this function does nothing
 *
 * */
fun <T : ScopedDataList> AdapterDataTarget<T>.enableDiff() : AdapterDataTarget<T> {
    this.shouldRunDiff = true
    return this
}


/**
 * Binds AdapterDataTarget to LiveData with LifecycleOwner
 *
 * @param liveData the live data which will be bound to the AdapterDataTarget
 * @param lifecycleOwner The LifecycleOwner which controls the observer
 *
 */
fun <T : ScopedDataList> AdapterDataTarget<T>.observeLiveData(liveData: LiveData<T>, lifecycleOwner: LifecycleOwner) {
    val adapterDataTarget = this
    liveData.observe(lifecycleOwner, Observer<T> { list: T? ->
        if (list == null || list.isNullOrEmpty()) {
            adapterDataTarget.setEmpty()
        } else {
            adapterDataTarget.updateDataset(list)
        }
    })
}

/**
 * Binds AdapterDataTarget to LiveData indefinitely.
 *
 * @param liveData the live data which will be bound to the AdapterDataTarget
 * @return the Observer that was created from LiveData subscription. Use to cancel Observation as usual
 */
fun <T : ScopedDataList> AdapterDataTarget<T>.observeLiveDataForever(liveData: LiveData<T>): Observer<T> {
    val adapterDataTarget = this
    val observer: Observer<T> = Observer { list: T? ->
        if (list == null || list.isNullOrEmpty()) {
            adapterDataTarget.setEmpty()
        } else {
            adapterDataTarget.updateDataset(list)
        }
    }
    liveData.observeForever(observer)

    return observer
}