package com.bleacherreport.adaptergendemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bleacherreport.adaptergen.MultiAdapterDataList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel {

    private val mutableMultiLiveData = MutableLiveData<MultiAdapterDataList>()
    val multiLiveData: LiveData<MultiAdapterDataList> = mutableMultiLiveData

    fun updateData() {
        val dataList = MultiAdapterDataList()
        for (i in 0 until 100) {
            dataList.add(i)
            dataList.add("$i string")
        }
        GlobalScope.launch(Dispatchers.IO) {
            delay(1000)
            mutableMultiLiveData.postValue(dataList)
        }
    }
}
