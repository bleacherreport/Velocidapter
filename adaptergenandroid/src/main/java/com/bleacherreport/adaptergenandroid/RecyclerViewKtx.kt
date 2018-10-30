package com.bleacherreport.adaptergenandroid

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * sets layout manager on RecyclerView
 * @param layoutManager to set
 * @return the called class
 */
fun RecyclerView.with(layoutManager: RecyclerView.LayoutManager): RecyclerView {
    this.layoutManager = layoutManager
    return this
}

fun RecyclerView.withLinearLayoutManager(reverse: Boolean = false, horizontal: Boolean = false): RecyclerView {
    if(horizontal) {
        this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, reverse)
    } else {
        this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, reverse)
    }
    return this
}
