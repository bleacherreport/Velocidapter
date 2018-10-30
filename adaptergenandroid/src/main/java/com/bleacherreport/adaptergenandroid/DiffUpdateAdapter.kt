package com.bleacherreport.adaptergenandroid

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * A RecyclerView.Adapter class designed to allow logical injection view Lambda functions
 */
class FunctionalAdapter<T : ScopedDataList>(
        val onCreateViewHolder: (ViewGroup, Int) -> RecyclerView.ViewHolder,
        val onBindViewHolder: (RecyclerView.ViewHolder, Int, List<Any>) -> Unit,
        val getItemViewType: (Int, List<Any>) -> Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), AdapterDataTarget<T> {

    private var currentDataset: List<Any> = ArrayList()
    override var isDiffComparable: Boolean? = null

    override fun getItemCount(): Int {
        return currentDataset.size
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): RecyclerView.ViewHolder {
        return onCreateViewHolder.invoke(viewGroup, type)
    }

    override fun onBindViewHolder(viewholder: RecyclerView.ViewHolder, postion: Int) {
        onBindViewHolder.invoke(viewholder, postion, currentDataset)
    }

    override fun getItemViewType(position: Int): Int {
        return getItemViewType.invoke(position, currentDataset)
    }

    override fun updateDataset(newDataset: T) {
        if (currentDataset.isEmpty() || (isDiffComparable != true)) {
            resetData(newDataset)
        } else {
            val diffResult = generateDiffResult(
                    currentDataset as List<DiffComparable>,
                    newDataset as List<DiffComparable>
            )
            currentDataset = newDataset
            diffResult.dispatchUpdatesTo(this)
        }
    }

    override fun resetData(newDataset: T) {
        checkDiffComparable(newDataset)
        currentDataset = newDataset.list
        notifyDataSetChanged()
    }

    override fun setEmpty() {
        currentDataset = mutableListOf()
    }

    fun checkDiffComparable(dataSet: T) : Boolean {
        if(isDiffComparable == null) {
            isDiffComparable = dataSet.isDiffComparable()
        }
        return isDiffComparable!!
    }
}