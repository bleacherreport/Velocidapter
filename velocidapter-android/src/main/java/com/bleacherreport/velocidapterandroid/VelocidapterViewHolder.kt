package com.bleacherreport.velocidapterandroid

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass


class VelocidapterViewHolder(
    val binding: ViewBinding,
    val dataType: KClass<*>,
    private val bindFunction: (Any?, VelocidapterViewHolder, Int) -> Unit,
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(data: Any?, position: Int) {
        bindFunction(data, this, position)
    }
}
