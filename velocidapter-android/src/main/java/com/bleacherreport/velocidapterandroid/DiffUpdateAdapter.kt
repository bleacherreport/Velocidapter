package com.bleacherreport.velocidapterandroid

import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.RecyclerView

/**
 * A RecyclerView.Adapter class designed to allow logical injection view Lambda functions
 */
class FunctionalAdapter<T : ScopedDataList>(
        private val onCreateViewHolder: (ViewGroup, Int) -> RecyclerView.ViewHolder,
        private val onBindViewHolder: (RecyclerView.ViewHolder, Int, List<Any>) -> Unit,
        private val onUnbindViewHolder: (RecyclerView.ViewHolder) -> Unit,
        private val getItemViewType: (Int, List<Any>) -> Int,
        private val onAttachToWindow: ((RecyclerView.ViewHolder) -> Unit)?,
        private val onDetachFromWindow: ((RecyclerView.ViewHolder) -> Unit)?) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), AdapterDataTarget<T> {

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
        notifyDataSetChanged()
    }

    private fun checkIsDiffComparable(dataSet: T): Boolean {
        if (isDiffComparable == null) {
            isDiffComparable = shouldRunDiff && dataSet.isDiffComparable
        }
        return isDiffComparable ?: false
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        onUnbindViewHolder.invoke(holder)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        onAttachToWindow?.invoke(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        onDetachFromWindow?.invoke(holder)
    }
}