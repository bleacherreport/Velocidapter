package com.bleacherreport.velocidapter

import com.bleacherreport.velocidapterannotations.VelociSuffix
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

interface BaseViewHolderBuilder {
    val element: Element
    val name: String
    val bindFunction: BaseBindFunction
    val unbindFunction: FunctionName?
    val attachFunction: FunctionName?
    val detachFunction: FunctionName?
    val createViewHolder: CodeBlock.Builder.(FileSpec.Builder) -> Unit
    val annotation: ViewHolder
}

data class BindMethodViewHolderBuilder(
    override val element: Element,
    override val name: String,
    val constructorParams: Map<String, TypeElement>,
    override val bindFunction: ViewHolderBindFunction,
    override val unbindFunction: FunctionName?,
    override val attachFunction: FunctionName?,
    override val detachFunction: FunctionName?,
    override val createViewHolder: CodeBlock.Builder.(FileSpec.Builder) -> Unit,
) : BaseViewHolderBuilder {
    override val annotation by lazy {
        element.getAnnotation(ViewHolder::class.java)!!
    }
}


data class ClassViewHolderBuilder(
    override val element: Element,
    override val name: String,
    val binding: ClassName,
    override val bindFunction: ViewBindingFunction,
    override val unbindFunction: FunctionName?,
    override val attachFunction: FunctionName?,
    override val detachFunction: FunctionName?,
) : BaseViewHolderBuilder {
    override val annotation by lazy {
        element.getAnnotation(ViewHolder::class.java)!!
    }
    override val createViewHolder: CodeBlock.Builder.(FileSpec.Builder) -> Unit = {
        addStatement(
            "val inflater = %T.from(viewGroup.context)",
            ClassName("android.view", "LayoutInflater")
        )
        element.getAnnotation(ViewHolder::class.java)!!.newBindingSuffix.takeIf { it != VelociSuffix.VELOCI_NONE }
            ?.also {
                val newBinding = "${binding.canonicalName.dropLast("Binding".length)}${it}Binding"
                val newBindingClass = ClassName.bestGuess(newBinding)
                addStatementNewLayout(
                    binding,
                    ViewBindingInflater(newBindingClass, binding)
                )
            } ?: addStatement(
            "val binding = %T.inflate(inflater, viewGroup, false)",
            binding
        )
        addStatement(
            "return@FunctionalAdapter·%T(binding, ${bindFunction.argumentType}::class) {data, viewHolder, position -> ",
            ClassName.bestGuess(name)
        )

        val isTopLevel = element.enclosingElement.isSyntheticClass()
        var enclosingName = element.enclosingElement.toString()
        if (isTopLevel) {
            enclosingName = enclosingName.split(".").dropLast(1).joinToString(".")
            it.addImport(enclosingName, bindFunction.functionName)
            addStatement(
                "   binding.${bindFunction.functionName}(data as %T)",
                ClassName.bestGuess(bindFunction.argumentType)
            )
        } else {
            addStatement(
                "%T.apply {\r\n" +
                        "    binding.${bindFunction.functionName}(data as %T)\r\n" +
                        "}",
                ClassName.bestGuess(enclosingName),
                ClassName.bestGuess(bindFunction.argumentType)
            )
        }
        addStatement("}")
    }
}
