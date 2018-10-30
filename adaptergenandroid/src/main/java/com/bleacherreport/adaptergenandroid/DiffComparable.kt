package com.bleacherreport.adaptergenandroid

import androidx.recyclerview.widget.DiffUtil

/**
 * Classes that implement are diffable by FunctionalAdapter
 */
interface DiffComparable {
    /**
     * evaluates if objects is the same but with altered properties, i.e. somebody put pepperoni on my pizza but its the same pizza
     */
    fun isSame(that: Any) : Boolean

}

/**
 * Generic DiffUtil callback used to abstract diff logic via DiffComparable
 */
class DiffComparableDiffUtilCallback(private val oldList: List<DiffComparable>, private val newList: List<DiffComparable>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].isSame(newList[newItemPosition])
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

}

fun generateDiffResult(oldList: List<DiffComparable>, newList: List<DiffComparable>) : DiffUtil.DiffResult {
    return DiffUtil.calculateDiff(DiffComparableDiffUtilCallback(oldList, newList))
}