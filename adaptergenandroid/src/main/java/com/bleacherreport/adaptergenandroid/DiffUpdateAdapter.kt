package com.bleacherreport.adaptergenandroid

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * A RecyclerView.Adapter class designed to allow logical injection view Lambda functions
 */
class FunctionalAdapter<T : ScopedDataList>(
        private val onCreateViewHolder: (ViewGroup, Int) -> RecyclerView.ViewHolder,
        private val onBindViewHolder: (RecyclerView.ViewHolder, Int, List<Any>) -> Unit,
        private val getItemViewType: (Int, List<Any>) -> Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), AdapterDataTarget<T> {

    private var currentDataset = listOf<Any>()
    override var shouldRunDiff = false
    private var isDiffComparable: Boolean? = null

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

    @Suppress("UNCHECKED_CAST")
    override fun updateDataset(newDataset: T) {
        if (currentDataset.isEmpty() || !checkIsDiffComparable(newDataset)) {
            resetData(newDataset)
        } else {
            val diffResult = generateDiffResult(
                    currentDataset as List<DiffComparable>,
                    newDataset.list as List<DiffComparable>
            )
            currentDataset = newDataset.list
            diffResult.dispatchUpdatesTo(this)
        }
    }

    override fun resetData(newDataset: T) {
        currentDataset = newDataset.list
        notifyDataSetChanged()
    }

    override fun setEmpty() {
        currentDataset = emptyList()
    }

    private fun checkIsDiffComparable(dataSet: T): Boolean {
        if (isDiffComparable == null) {
            isDiffComparable = shouldRunDiff && dataSet.isDiffComparable
        }
        return isDiffComparable ?: false
    }
}