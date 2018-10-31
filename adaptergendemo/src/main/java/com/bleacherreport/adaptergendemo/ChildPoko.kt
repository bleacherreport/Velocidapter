package com.bleacherreport.adaptergendemo

import android.annotation.SuppressLint
import android.view.View
import com.bleacherreport.adaptergenanotations.Bind
import com.bleacherreport.adaptergenanotations.ViewHolder
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_string.*

@ViewHolder(adapters = [MainActivity.ParentAndChildHolderTypeAdapter], layoutResId = R.layout.item_string)
class ChildViewHolder(override val containerView: View) : ParentViewHolder(containerView), LayoutContainer {

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(model: ChildPoko, position: Int) {
        textView.text = "ChildPoko ${model.string}"
    }
}

class ChildPoko(string: String) : ParentPoko(string)