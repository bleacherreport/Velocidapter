package com.bleacherreport.velocidapterdemo.parentchild

import android.annotation.SuppressLint
import android.util.Log
import com.bleacherreport.velocidapterannotations.*
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemStringBinding

@ViewHolder(adapters = [MainActivity.ParentChildAdapter])
class ChildViewHolder(binding: ItemStringBinding) : ParentViewHolder(binding) {

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(model: ChildPoko, position: Int) {
        binding.textView.text = "ChildPoko ${model.string}"
        listPosition = position
    }

    @OnAttachToWindow
    override fun onAttachToWindow() {
        Log.d("ChildViewHolder", "$listPosition attached to window")
    }

    @OnDetachFromWindow
    override fun onDetachFromWindow() {
        Log.d("ChildViewHolder", "$listPosition from window")
    }

    @Unbind
    fun unbindModel() {
       Log.i("ChildViewHolder", "$listPosition unbound" )
    }
}

class ChildPoko(string: String) : ParentPoko(string)