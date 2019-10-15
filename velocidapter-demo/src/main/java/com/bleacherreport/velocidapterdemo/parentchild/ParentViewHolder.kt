package com.bleacherreport.velocidapterdemo.parentchild

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.OnAttachToWindow
import com.bleacherreport.velocidapterannotations.OnDetachFromWindow
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_string.*

@ViewHolder(adapters = [MainActivity.ParentChildAdapter], layoutResId = R.layout.item_string)
open class ParentViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    var listPosition = -1
    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(model: ParentPoko, position: Int) {
        textView.text = "ParentPoko ${model.string}"
        listPosition = position
    }


    @OnAttachToWindow
    open fun onAttachToWindow() {
        Log.d("ParentViewHolder", "$listPosition attached to window")
    }

    @OnDetachFromWindow
    open fun onDetachFromWindow() {
        Log.d("ParentViewHolder", "$listPosition detached from window")
    }
}

open class ParentPoko(val string: String)