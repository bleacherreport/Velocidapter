package com.bleacherreport.velocidapter

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

class ViewBindingInflater(val newViewBinding: ClassName, val oldViewBinding: ClassName) {

    fun CodeBlock.Builder.buildInflateForParent(append: String = "") {
        addStatement(
            append + "when(%T.useNewLayouts()){\n" +
                    "            true -> %T.bind(%T.inflate(%T.from(viewGroup.context), viewGroup, false).root)\n" +
                    "            false ->  %T.inflate(%T.from(viewGroup.context), viewGroup, false)\n" +
                    "        }",
            ClassName.bestGuess("com.bleacherreport.velocidapterandroid.VelocidapterSettings"),
            oldViewBinding,
            newViewBinding,
            ClassName("android.view", "LayoutInflater"),
            oldViewBinding,
            ClassName("android.view", "LayoutInflater"),
        )
    }

    fun CodeBlock.Builder.buildInflateFromLayoutInflater(append: String) {
        addStatement(
            append + "when(%T.useNewLayouts()){\n" +
                    "            true -> %T.bind(%T.inflate(inflater).root)\n" +
                    "            false ->  %T.inflate(inflater)\n" +
                    "        }",
            ClassName.bestGuess("com.bleacherreport.velocidapterandroid.VelocidapterSettings"),
            oldViewBinding,
            newViewBinding,
            oldViewBinding,
        )
    }
}