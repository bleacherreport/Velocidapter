package com.bleacherreport.velocidapterdemo.diff

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterandroid.DiffComparable
import com.bleacherreport.velocidapteranotations.Bind
import com.bleacherreport.velocidapteranotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_string.*

@ViewHolder(adapters = [MainActivity.DiffTypeAdapter], layoutResId = R.layout.item_string)
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