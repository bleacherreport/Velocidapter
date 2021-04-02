package com.bleacherreport.velocidapterdemo.single

import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemNumberBinding

@ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
fun ItemNumberBinding.bind(number: NumberViewItemBindingExtension) {
    textView.text = number.number
}

object Test {
    @ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter], isMemberFunction = true)
    fun ItemNumberBinding.bindTest(number: Int) {
        textView.text = number.toString() + " bind method"
    }
}

@ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
class NumberViewHolder(val binding: ItemNumberBinding) : RecyclerView.ViewHolder(binding.root) {

    @Bind
    fun bind(item: NumberViewItemViewHolder) {
        binding.bind(item.number)
    }
}

class NumberViewItemBindingExtension(val number: String)
class NumberViewItemViewHolder(val number: NumberViewItemBindingExtension)

