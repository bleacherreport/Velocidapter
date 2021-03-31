package com.bleacherreport.velocidapterdemo.single

import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewBinding
import com.bleacherreport.velocidapterannotations.ViewHolderBind
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemNumberBinding

@ViewBinding(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
fun ItemNumberBinding.bind(number: Int) {
    textView.text = number.toString()
}

object Test {
    @ViewBinding(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter], isExtensionFunction = false)
    fun bind(itemNumberBinding: ItemNumberBinding, number: Float) {
        itemNumberBinding.textView.text = number.toString()
    }
}

@ViewHolderBind(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
class NumberViewHolder(val binding: ItemNumberBinding) : RecyclerView.ViewHolder(binding.root) {

    @Bind
    fun bind(item: NumberViewItem) {
        binding.bind(item.number)
    }
}

class NumberViewItem(val number: Int)
