package com.bleacherreport.velocidapter

import com.bleacherreport.velocidapterannotations.Bind
import com.bleacherreport.velocidapterannotations.ViewHolder
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind

private const val BIND_INSTRUCTION = """
A Velocidapter ViewHolder must have one @Bind annotated method which can be formatted one of two ways:
  1) The method takes two argument - the first being your data model you bind with (Any), and the second being this element's position in the list (Int)
  2) The method takes one argument - the data model you bind with (Any)"""

@AutoService(Processor::class)
class VelocidapterProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(ViewHolder::class.java.name, Bind::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val bindFunctionList = roundEnv?.getElementsAnnotatedWith(Bind::class.java)?.map { it }
        val adapterList = HashSet<BindableAdapter>()
        roundEnv?.getElementsAnnotatedWith(ViewHolder::class.java)?.forEach { viewHolderElement ->
            var bindFunction: BindFunction? = null
            viewHolderElement.enclosedElements.forEach { enclosedElement ->
                bindFunctionList?.find { it == enclosedElement }?.let { bindElement ->
                    if (bindFunction != null) {
                        throw VelocidapterException("${viewHolderElement.simpleName} has multiple @Bind annotated methods. $BIND_INSTRUCTION")
                    }
                    bindFunction = createBindFunction(bindElement, viewHolderElement.simpleName.toString())
                }
            }

            if (bindFunction == null) {
                throw VelocidapterException("${viewHolderElement.simpleName} is missing an @Bind method. $BIND_INSTRUCTION")
            }

            val annotation = viewHolderElement.getAnnotation(ViewHolder::class.java)!!
            val viewHolder = BindableViewHolder(getFullPath(viewHolderElement), annotation.layoutResId, bindFunction!!)
            annotation.adapters.forEach { adapterName ->
                if (!adapterList.any { it.name == adapterName }) {
                    adapterList.add(BindableAdapter(adapterName))
                }
                adapterList.find { it.name == adapterName }?.viewHolders?.add(viewHolder)
            }
        }

        adapterList.forEach { entry ->
            val builder = FileSpec.builder("com.bleacherreport.velocidapter", entry.name)

            builder.addType(getDataList(entry))
            builder.addType(getDataTarget(entry))
            builder.addFunction(getAdapterKtx(entry))

            val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            builder.build().writeTo(File(kaptKotlinGeneratedDir, "${entry.name}.kt"))
        }

        return true
    }

    private fun createBindFunction(element: Element, viewHolderName: String): BindFunction {
        val parameters = (element.asType() as ExecutableType).parameterTypes
        if (parameters.size != 2 && parameters.size != 1) {
            throw VelocidapterException("@Bind method $viewHolderName.${element.simpleName} does not have the right number of parameters. $BIND_INSTRUCTION")
        }

        val dataParam = parameters[0]
        if (parameters.size > 1) {
            if (parameters[1] !is PrimitiveType || parameters[1].kind != TypeKind.INT) {
                throw VelocidapterException("@Bind method $viewHolderName.${element.simpleName} second parameter is not an Int. $BIND_INSTRUCTION")
            }
        }

        val dataArgumentType = if (dataParam is DeclaredType) {
            var name = (dataParam.asElement() as TypeElement).qualifiedName.toString()
            if (name == "java.lang.String") name = "kotlin.String"
            name
        } else {
            resolvePrimitiveType((dataParam as PrimitiveType).toString())
        }

        return BindFunction(element, dataArgumentType, parameters.size > 1)
    }

    private fun getDataList(adapter: BindableAdapter): TypeSpec {
        val typeSpec = TypeSpec.classBuilder(adapter.dataListName)
                .superclass(ClassName("com.bleacherreport.velocidapterandroid", "ScopedDataList"))

        val dataClassNames = mutableListOf<ClassName>()
        adapter.viewHolders.forEach { viewHolder ->
            val argumentClass = viewHolder.bindFunction.argumentType
            val simpleClassName = argumentClass.substringAfterLast(".")
            val argumentClassName = ClassName("", argumentClass)
            dataClassNames.add(argumentClassName)
            typeSpec.addFunction(FunSpec.builder("add")
                    .addParameter("model", argumentClassName)
                    .addStatement("listInternal.add(model)")
                    .build())

            typeSpec.addFunction(FunSpec.builder("addListOf$simpleClassName")
                    .addParameter("list", ClassName("kotlin.collections", "List")
                            .parameterizedBy(argumentClassName))
                    .addStatement("listInternal.addAll(list)")
                    .build())
        }

        typeSpec.addProperty(PropertySpec.builder("listClasses", ClassName("kotlin.collections", "List")
                .parameterizedBy(ClassName("", "Class")
                        .parameterizedBy(STAR)))
                .initializer("listOf(%L)", dataClassNames.map { CodeBlock.of("%T::class.java", it) }.joinToCode())
                .addModifiers(KModifier.PRIVATE)
                .build())
                .build()

        typeSpec.addProperty(PropertySpec.builder("isDiffComparable", Boolean::class)
                .addModifiers(KModifier.OVERRIDE)
                .delegate(CodeBlock.builder()
                        .beginControlFlow("lazy")
                        .addStatement(
                                "listClasses.all { %T::class.java.isAssignableFrom(it) }",
                                ClassName("com.bleacherreport.velocidapterandroid", "DiffComparable")
                        )
                        .endControlFlow()
                        .build())
                .build())
                .build()

        return typeSpec.build()
    }

    private fun getDataTarget(adapter: BindableAdapter): TypeSpec {
        val typeSpec = TypeSpec.interfaceBuilder(adapter.dataTargetName)
                .addSuperinterface(ClassName("com.bleacherreport.velocidapterandroid", "AdapterDataTarget")
                        .parameterizedBy(ClassName("", adapter.dataListName)))

        return typeSpec.build()
    }

    private fun getAdapterKtx(adapter: BindableAdapter): FunSpec {
        val funSpec = FunSpec.builder("attach${adapter.name}")
                .receiver(ClassName("androidx.recyclerview.widget", "RecyclerView"))
                .returns(ClassName("com.bleacherreport.velocidapterandroid", "AdapterDataTarget")
                        .parameterizedBy(ClassName("", adapter.dataListName)))
                .addStatement("val adapter = %T(⇥", ClassName("com.bleacherreport.velocidapterandroid", "FunctionalAdapter")
                        .parameterizedBy(ClassName("", adapter.dataListName))
                )
                .addCode("%L,\n", getCreateViewHolderLambda(adapter.viewHolders))
                .addCode("%L,\n", getBindViewHolderLamda(adapter.viewHolders))
                .addCode("%L\n", getItemTypeLambda(adapter.viewHolders))
                .addStatement("⇤)")
                .addStatement("this.adapter = adapter")
                .addStatement("return adapter")
        return funSpec.build()
    }


    private fun getCreateViewHolderLambda(viewHolders: Set<BindableViewHolder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ viewGroup, type ->")
            viewHolders.forEachIndexed { index, viewHolder ->
                beginControlFlow("if (type == $index)")
                addStatement(
                        "val view = %T.from(viewGroup.context).inflate(%L, viewGroup, false)",
                        ClassName("android.view", "LayoutInflater"),
                        viewHolder.layoutResId
                )
                addStatement("return@FunctionalAdapter·%T(view)", ClassName("", viewHolder.name))
                endControlFlow()
            }
            addStatement("throw RuntimeException(%S)", "Type not found ViewHolder set.")
            endControlFlow()
        }
    }


    private fun getBindViewHolderLamda(viewHolders: Set<BindableViewHolder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ viewHolder, position, dataset ->")
            viewHolders.forEach { viewHolder ->
                val viewHolderClassName = ClassName("", viewHolder.name)
                val bindStatementFormat = if (viewHolder.bindFunction.hasPositionParameter) {
                    "(viewHolder as %T).%N(dataset[position] as %T, position)"
                } else {
                    "(viewHolder as %T).%N(dataset[position] as %T)"
                }
                beginControlFlow("if (viewHolder::class == %T::class)", viewHolderClassName)
                addStatement(
                        bindStatementFormat,
                        viewHolderClassName,
                        viewHolder.bindFunction.functionName,
                        ClassName("", viewHolder.bindFunction.argumentType)
                )
                endControlFlow()
            }
            endControlFlow()
        }
    }


    private fun getItemTypeLambda(viewHolders: Set<BindableViewHolder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ position, dataset ->")
            addStatement("val dataItem = dataset[position]")
            viewHolders.forEachIndexed { index, viewHolder ->
                beginControlFlow(
                        "if (dataItem::class == %T::class)",
                        ClassName("", viewHolder.bindFunction.argumentType)
                )
                addStatement("return@FunctionalAdapter $index")
                endControlFlow()
            }
            addStatement("return@FunctionalAdapter -1")
            endControlFlow()
        }
    }

    private fun getFullPath(element: Element): String {
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

    private fun resolvePrimitiveType(typeName: String): String {
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

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}

class VelocidapterException(override val message: String?) : java.lang.Exception(message)

class BindFunction(val element: Element, val argumentType: String, val hasPositionParameter: Boolean) {
    val functionName: String
        get() = element.simpleName.toString().split("(")[0]
}

data class BindableAdapter(val name: String, val viewHolders: MutableSet<BindableViewHolder> = LinkedHashSet()) {
    val dataListName
        get() = name + "DataList"
    val dataTargetName
        get() = name + "DataTarget"
}

data class BindableViewHolder(val name: String, val layoutResId: Int, val bindFunction: BindFunction)
