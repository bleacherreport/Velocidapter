package com.bleacherreport.velocidapter

import com.bleacherreport.velocidapterannotations.VelociSuffix
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import javax.lang.model.element.Element

interface BaseViewHolderBuilder {
    val element: Element
    val name: String
    val bindFunction: BaseBindFunction
    val unbindFunction: FunctionName?
    val attachFunction: FunctionName?
    val detachFunction: FunctionName?
    val createViewHolder: CodeBlock.Builder.() -> Unit
    val annotation: ViewHolder
}

data class BindMethodViewHolderBuilder(
    override val element: Element,
    override val name: String,
    override val bindFunction: ViewHolderBindFunction,
    override val unbindFunction: FunctionName?,
    override val attachFunction: FunctionName?,
    override val detachFunction: FunctionName?,
    override val createViewHolder: CodeBlock.Builder.() -> Unit,
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
    override val createViewHolder: CodeBlock.Builder.() -> Unit = {
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
                    "val binding = when(%T.useNewLayouts()){\n" +
                            "            true -> %T.bind(%T.inflate(%T.from(viewGroup.context), viewGroup, false).root)\n" +
                            "            false ->  %T.inflate(%T.from(viewGroup.context), viewGroup, false)\n" +
                            "        }",
                    ClassName.bestGuess("com.bleacherreport.velocidapterandroid.VelocidapterSettings"),
                    binding,
                    newBindingClass,
                    ClassName("android.view", "LayoutInflater"),
                    binding,
                    ClassName("android.view", "LayoutInflater"),
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
            addStatement(
                "    binding.%M(data as %T)",
                MemberName(enclosingName, bindFunction.functionName),
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
