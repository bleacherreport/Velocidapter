package com.bleacherreport.velocidapterdemo.parentchild

import android.annotation.SuppressLint
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.OnAttachToWindow
import com.bleacherreport.velocidapterannotations.OnDetachFromWindow
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.bleacherreport.velocidapterdemo.MainActivity
import com.bleacherreport.velocidapterdemo.databinding.ItemStringBinding

@ViewHolder(adapters = [MainActivity.ParentChildAdapter])
open class ParentViewHolder(val binding: ItemStringBinding) : RecyclerView.ViewHolder(binding.root) {

    var listPosition = -1

    @SuppressLint("SetTextI18n")
    @Bind
    fun bindModel(model: ParentPoko, position: Int) {
        binding.textView.text = "ParentPoko ${model.string}"
        listPosition = position
    }


    @OnAttachToWindow
    open fun onAttachToWindow() {
        Log.d("ParentViewHolder", "$listPosition attached to window")
    }

    @OnDetachFromWindow
    open fun onDetachFromWindow() {
        Log.d("ParentViewHolder", "$listPosition detached from window")
    }
}

open class ParentPoko(val string: String)