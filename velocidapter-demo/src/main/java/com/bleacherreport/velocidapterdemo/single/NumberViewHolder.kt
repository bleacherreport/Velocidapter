package com.bleacherreport.velocidapterdemo.single

import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemNumberBinding

@ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
fun ItemNumberBinding.bind(item: NumberViewItemBindingExtension) {
    textView.text = item.text
}

object Test {
    @ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter], isMemberFunction = true)
    fun ItemNumberBinding.bindTest(item: NumberViewItemBindingMemberFunction) {
        textView.text = item.text
    }
}

@ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
class NumberViewHolder(val binding: ItemNumberBinding) : RecyclerView.ViewHolder(binding.root) {

    @Bind
    fun bind(item: NumberViewItemViewHolder, position: Int) {
        binding.bind(item.number.copy(text = item.number.text + " @ position $position"))
    }
}

data class NumberViewItemBindingExtension(val text: String)
data class NumberViewItemBindingMemberFunction(val text: String)
data class NumberViewItemViewHolder(val number: NumberViewItemBindingExtension)

