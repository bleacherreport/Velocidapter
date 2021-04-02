package com.bleacherreport.velocidapter

import com.bleacherreport.velocidapterannotations.ViewHolder
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

interface BaseViewHolderBuilder {
    val name: String
    val bindFunction: BaseBindFunction
    val unbindFunction: FunctionName?
    val attachFunction: FunctionName?
    val detachFunction: FunctionName?
    val createViewHolder: CodeBlock.Builder.() -> Unit
}

data class BindMethodViewHolderBuilder(
    override val name: String,
    override val bindFunction: PositionBindFunction,
    override val unbindFunction: FunctionName?,
    override val attachFunction: FunctionName?,
    override val detachFunction: FunctionName?,
    override val createViewHolder: CodeBlock.Builder.() -> Unit,
) : BaseViewHolderBuilder

data class ClassViewHolderBuilder(
    override val name: String,
    val binding: ClassName,
    override val bindFunction: ViewBindFunction,
    override val unbindFunction: FunctionName?,
    override val attachFunction: FunctionName?,
    override val detachFunction: FunctionName?,
) : BaseViewHolderBuilder {
    override val createViewHolder: CodeBlock.Builder.() -> Unit = {
        addStatement(
            "val inflater = %T.from(viewGroup.context)",
            ClassName("android.view", "LayoutInflater")
        )
        addStatement(
            "val binding = %T.inflate(inflater, viewGroup, false)",
            binding
        )
        addStatement(
            "return@FunctionalAdapter·%T(binding, ${bindFunction.argumentType}::class) {data, viewHolder, position -> ",
            ClassName.bestGuess(name)
        )
        val annotation = bindFunction.element.getAnnotation(ViewHolder::class.java)!!

        var startingStatement = "    %M(binding, "
        var enclosingName = bindFunction.element.enclosingElement.toString()
        if (!annotation.isClassMethod) {
            startingStatement = "    binding.%M("
            enclosingName = enclosingName.split(".").let {
                it.takeIf { it.lastOrNull()?.endsWith("Kt") == true }?.dropLast(1) ?: it
            }.joinToString(".")
        }

        addStatement(
            "${startingStatement}data as %T)",
            MemberName(enclosingName, bindFunction.functionName),
            ClassName.bestGuess(bindFunction.argumentType)
        )

        addStatement("}")
    }
}
