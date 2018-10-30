package com.bleacherreport.adaptergenandroid

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
    var isDiffComparable: Boolean?
}

/**
 * Binds AdapterDataTarget to LiveData with LifecycleOwner
 *
 * @param hardReset specifies whether the underlying update should be done with via a diffing function
 * or by invalidating the entire data set
 *
 * Note: All classes contained in the data set must implement DiffComparable, or hardReset=false will behave like true
 * Additional Note: a minor amount of reflection will be done on the first update of the dataset if hardReset is set to false
 *
 * @param liveData the live data which will be bound to the AdapterDataTarget
 * @param lifecycleOwner The LifecycleOwner which controls the observer
 *
 */
fun <T : ScopedDataList> AdapterDataTarget<T>.observeLiveData(hardReset: Boolean = true, liveData: LiveData<T>, lifecycleOwner: LifecycleOwner) {
    val adapterDataTarget = this
    liveData.observe(lifecycleOwner, Observer<T> { list: T? ->
        adapterDataTarget.isDiffComparable = hardReset
        if (list != null) {
            if (hardReset) {
                adapterDataTarget.isDiffComparable = hardReset
                adapterDataTarget.resetData(list)
            } else {
                adapterDataTarget.updateDataset(list)
            }
        } else {
            adapterDataTarget.setEmpty()
        }
    })
}

/**
 * Binds AdapterDataTarget to LiveData indefinitely.
 *
 * @param hardReset specifies whether the underlying update should be done with via a diffing function
 * or by invalidating the entire data set
 *
 * Note: All classes contained in the data set must implement DiffComparable, or hardReset=false will behave like true
 * Additional Note: a minor amount of reflection will be done on the first update of the dataset if hardReset is set to false
 *
 * @param liveData the live data which will be bound to the AdapterDataTarget
 * @return the Observer that was created from LiveData subscription. Use to cancel Observation as usual
 */
fun <T : ScopedDataList> AdapterDataTarget<T>.observeLiveDataForever(hardReset: Boolean = true, liveData: LiveData<T>): Observer<T> {
    val adapterDataTarget = this
    val observer: Observer<T> = Observer { list: T? ->
        if (list != null) {
            if (hardReset) {
                adapterDataTarget.isDiffComparable = hardReset
                adapterDataTarget.resetData(list)
            } else {
                adapterDataTarget.updateDataset(list)
            }
        } else {
            adapterDataTarget.setEmpty()
        }
    }
    liveData.observeForever(observer)

    return observer
}