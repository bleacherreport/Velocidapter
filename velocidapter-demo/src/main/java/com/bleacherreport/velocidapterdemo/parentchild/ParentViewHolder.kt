package com.bleacherreport.velocidapterdemo.parentchild

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapteranotations.Bind
import com.bleacherreport.velocidapteranotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_string.*

@ViewHolder(adapters = [MainActivity.ParentChildAdapter], layoutResId = R.layout.item_string)
open class ParentViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(model: ParentPoko, position: Int) {
        textView.text = "ParentPoko ${model.string}"
    }
}

open class ParentPoko(val string: String)