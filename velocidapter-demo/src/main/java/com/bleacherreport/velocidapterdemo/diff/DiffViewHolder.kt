package com.bleacherreport.velocidapterdemo.diff

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterandroid.DiffComparable
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemStringBinding

@ViewHolder(adapters = [MainActivity.DiffTypeAdapter])
class DiffViewHolder(val binding: ItemStringBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(diffPoko: DiffPoko, position: Int) {
        binding.textView.text = "${diffPoko.id} updated at ${diffPoko.time}"
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