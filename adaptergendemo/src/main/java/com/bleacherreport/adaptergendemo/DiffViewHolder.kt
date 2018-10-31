package com.bleacherreport.adaptergendemo

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.adaptergenandroid.DiffComparable
import com.bleacherreport.adaptergenanotations.Bind
import com.bleacherreport.adaptergenanotations.ViewHolder
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

class DiffPoko(val id : Int, val time: Long) : DiffComparable {
    override fun isSame(that: Any): Boolean {
        return if(that is DiffPoko) {
            id == that.id
        } else {
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiffPoko

        if (id != other.id) return false
        if (time != other.time) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + time.hashCode()
        return result
    }
}