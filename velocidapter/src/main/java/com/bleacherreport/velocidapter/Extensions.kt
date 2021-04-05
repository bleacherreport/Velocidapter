package com.bleacherreport.velocidapter

import com.squareup.kotlinpoet.CodeBlock
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.PrimitiveType

fun CodeBlock.Builder.beginControlFlowElseIf(index: Int, controlFlow: String, vararg args: Any?) =
        beginControlFlow((if (index == 0) "" else " else ") + controlFlow, *args)

fun Element.methodFullName() = "$enclosingElement.$simpleName"

fun resolvePrimitiveType(typeName: String): String {
        return when (typeName) {
                "boolean" -> "kotlin.Boolean"
                "int" -> "kotlin.Int"
                "long" -> "kotlin.Long"
                "float" -> "kotlin.Float"
                "double" -> "kotlin.Double"
                "short" -> "kotlin.Short"
                "byte" -> "kotlin.Byte"
                else -> typeName
        }
}

fun getFullPath(element: Element): String {
        return if (element is TypeElement) {
                var enclosing = element
                while (enclosing.kind != ElementKind.PACKAGE) {
                        enclosing = enclosing.enclosingElement
                }
                val packageElement = enclosing as PackageElement
                var path = packageElement.qualifiedName.toString() + "." + element.simpleName.toString()
                if (path == "java.lang.String") path = "kotlin.String"
                path
        } else {
                resolvePrimitiveType((element as PrimitiveType).toString())
        }
}

fun ExecutableType.viewBindingParamNameAtPosition(position: Int): String? {
        val viewBindingParam = parameterTypes.getOrNull(position) as? DeclaredType
        val viewBindingTypeElement = (viewBindingParam?.asElement() as? TypeElement)
        if (viewBindingTypeElement?.interfaces?.firstOrNull()?.toString() != "androidx.viewbinding.ViewBinding")
                return null
        return viewBindingTypeElement.qualifiedName?.toString()
}

fun ExecutableType.paramNameAtPosition(position: Int): String? {
        return parameterTypes.getOrNull(position)?.let {
                if (it is DeclaredType) {
                        var name = (it.asElement() as TypeElement).qualifiedName.toString()
                        if (name == "java.lang.String") name = "kotlin.String"
                        name
                } else {
                        resolvePrimitiveType((it as PrimitiveType).toString())
                }
        }
}

fun Element.isSyntheticClass(): Boolean {
        return getAnnotation(Metadata::class.java)?.kind == 2
}

