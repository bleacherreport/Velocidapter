package com.bleacherreport.adaptergendemo.singleviewholdertype

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.bleacherreport.adaptergenanotations.Bind
import com.bleacherreport.adaptergenanotations.ViewHolder
import com.bleacherreport.adaptergendemo.MainActivity
import com.bleacherreport.adaptergendemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_number.*

@ViewHolder(adapters = [MainActivity.numberAdapter], layoutResId = R.layout.item_number)
class NumberViewHolder(override val containerView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(containerView), LayoutContainer {

    @Bind
    fun bindModel(number: NumberBox, position: Int) {
        textView.text = number.position.toString()
    }
}

//Wrapper class as primitives are not supported
class NumberBox(val position: Int)