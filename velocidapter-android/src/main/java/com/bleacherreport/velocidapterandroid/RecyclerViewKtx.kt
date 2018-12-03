package com.bleacherreport.velocidapterandroid

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * sets layout manager on RecyclerView
 * @param layoutManager to set
 * @return the called class
 */
fun RecyclerView.with(layoutManager: RecyclerView.LayoutManager): RecyclerView {
    return this.also { it.layoutManager = layoutManager }
}

fun RecyclerView.withLinearLayoutManager(horizontal: Boolean = false,
                                         reverse: Boolean = false): RecyclerView {
    return this.apply {
        layoutManager = LinearLayoutManager(context,
                if (horizontal) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL,
                reverse)
    }
}
