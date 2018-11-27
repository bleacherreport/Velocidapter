package com.bleacherreport.adaptergendemo.multi

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.adaptergenanotations.Bind
import com.bleacherreport.adaptergenanotations.ViewHolder
import com.bleacherreport.adaptergendemo.MainActivity
import com.bleacherreport.adaptergendemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_string.*

@ViewHolder(adapters = [MainActivity.MultiAdapter], layoutResId = R.layout.item_string)
class StringViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(string: String, position: Int) {
        textView.text = "$string @ position $position"
    }
}