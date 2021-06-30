package com.bleacherreport.velocidapterdemo.multi

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemStringBinding

@ViewHolder(adapters = [MainActivity.MultiAdapter])
class StringViewHolder(binding: ItemStringBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    @Bind
    fun ItemStringBinding.nameOtherThanBindModel(string: String, position: Int) {
        textView.text = "$string @ position $position"
    }
}

@ViewHolder(adapters = ["*"])
fun ItemStringBinding.bind(string: Float) {
    textView.text = "$string @ position"
}