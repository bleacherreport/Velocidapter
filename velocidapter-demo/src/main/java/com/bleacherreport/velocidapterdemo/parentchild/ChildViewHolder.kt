package com.bleacherreport.velocidapterdemo.parentchild

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.OnAttachToWindow
import com.bleacherreport.velocidapterannotations.OnDetachFromWindow
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_string.*

@ViewHolder(adapters = [MainActivity.ParentChildAdapter], layoutResId = R.layout.item_string)
class ChildViewHolder(override val containerView: View) : ParentViewHolder(containerView), LayoutContainer {

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(model: ChildPoko, position: Int) {
        textView.text = "ChildPoko ${model.string}"
        listPosition = position
    }


    @OnAttachToWindow
    override fun onAttachToWindow() {
        Log.d("ChildViewHolder", "$listPosition attached to window")
    }

    @OnDetachFromWindow
    override fun onDetachFromWindow() {
        Log.d("ChildViewHolder", "$listPosition from window")
    }
}

class ChildPoko(string: String) : ParentPoko(string)