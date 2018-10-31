package com.bleacherreport.adaptergendemo.diff

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.adaptergenandroid.DiffComparable
import com.bleacherreport.adaptergenanotations.Bind
import com.bleacherreport.adaptergenanotations.ViewHolder
import com.bleacherreport.adaptergendemo.MainActivity
import com.bleacherreport.adaptergendemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_string.*

@ViewHolder(adapters = [MainActivity.DIFF_TYPE_ADAPTER], layoutResId = R.layout.item_string)
class DiffViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(diffPoko: DiffPoko, position: Int) {
        textView.text = "${diffPoko.id} updated at ${diffPoko.time}"
    }
}

data class DiffPoko(val id : Int, val time: Long) : DiffComparable {
    override fun isSame(that: Any): Boolean {
        return if(that is DiffPoko) {
            id == that.id
        } else {
            false
        }
    }
}