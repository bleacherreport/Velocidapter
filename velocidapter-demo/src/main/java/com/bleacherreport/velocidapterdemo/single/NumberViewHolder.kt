package com.bleacherreport.velocidapterdemo.single

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_number.*

@ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter], layoutResId = R.layout.item_number)
class NumberViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    @Bind
    fun bindModel(number: Int) {
        textView.text = number.toString()
    }
}