package com.bleacherreport.velocidapterdemo.multi

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemStringBinding

@ViewHolder(adapters = [MainActivity.MultiAdapter])
class StringViewHolder(val binding: ItemStringBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    @Bind
    fun nameOtherThanBindModel(string: String, position: Int) {
        binding.textView.text = "$string @ position $position"
    }
}