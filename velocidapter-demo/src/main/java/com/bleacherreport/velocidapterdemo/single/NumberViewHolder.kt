package com.bleacherreport.velocidapterdemo.single

import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.VelociSuffix
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemNumberBinding

/** ViewBinding Top Level Extension Function **/
@ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter],
    newBindingSuffix = VelociSuffix.VELOCI_NEW)
fun ItemNumberBinding.bind(item: NumberViewItemBindingExtension) {
    textView.text = item.text
}

/** ViewBinding Object Member Extension Function **/
object Test {
    @ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter])
    fun ItemNumberBinding.bindTest(item: NumberViewItemBindingMemberFunction) {
        textView.text = item.text
    }
}

/** ViewBinding ViewHolder Class **/
@ViewHolder(adapters = [MainActivity.SingleAdapter, MainActivity.MultiAdapter],
    newBindingSuffix = VelociSuffix.VELOCI_NEW)
class NumberViewHolder(val binding: ItemNumberBinding) : RecyclerView.ViewHolder(binding.root) {
    @Bind
    fun bind(item: NumberViewItemViewHolder, position: Int) {

        binding.bind(item.number.copy(text = item.number.text + " @ position $position"))
    }
}

data class NumberViewItemBindingExtension(val text: String)
data class NumberViewItemBindingMemberFunction(val text: String)
data class NumberViewItemViewHolder(val number: NumberViewItemBindingExtension)

