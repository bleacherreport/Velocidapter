package com.bleacherreport.adaptergendemo

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.adaptergenanotations.Bind
import com.bleacherreport.adaptergenanotations.ViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_number.*

@ViewHolder(adapters = [MainActivity.SingleViewHolderTypeAdapter, MainActivity.MultiViewHolderTypeAdapter], layoutResId = R.layout.item_number)
class NumberViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    @Bind
    fun bindModel(number: Int, position: Int) {
        textView.text = number.toString()
    }
}