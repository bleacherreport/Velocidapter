package com.bleacherreport.velocidapter

import com.bleacherreport.velocidapterannotations.*
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.tools.Diagnostic



@AutoService(Processor::class)
class VelocidapterProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(ViewHolder::class.java.name,
            Bind::class.java.name,
            Unbind::class.java.name,
            OnAttachToWindow::class.java.name,
            OnDetachFromWindow::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        return try {
            val bindFunctionList = roundEnv?.getElementsAnnotatedWith(Bind::class.java)?.map { it }
            val unbindFunctionList = roundEnv?.getElementsAnnotatedWith(Unbind::class.java)?.map { it }
            val attachFunctionList = roundEnv?.getElementsAnnotatedWith(OnAttachToWindow::class.java)?.map { it }
            val detatchFunctionList = roundEnv?.getElementsAnnotatedWith(OnDetachFromWindow::class.java)?.map { it }

            val adapterList = HashSet<BindableAdapter>()

            fun getFunctions(
                viewHolderElement: Element,
                callback: (bindFunction: PositionBindFunction?, unbindFunction: FunctionName?, attachFunction: FunctionName?, detachFunction: FunctionName?) -> Unit,
            ) {
                var bindFunction: PositionBindFunction? = null
                var unbindFunction: FunctionName? = null
                var attachFunction: FunctionName? = null
                var detachFunction: FunctionName? = null

                viewHolderElement.enclosedElements.forEach { enclosedElement ->
                    bindFunctionList?.find { it == enclosedElement }?.let { bindElement ->
                        if (bindFunction != null) {
                            throw VelocidapterException("${viewHolderElement.simpleName} has multiple @Bind annotated methods. $VIEW_BIND_INSTRUCTION")
                        }
                        bindFunction = PositionBindFunction.from(bindElement,
                            viewHolderElement.simpleName.toString())
                    }

                    unbindFunctionList?.find { it == enclosedElement }?.let { bindElement ->
                        if (unbindFunction != null) {
                            throw VelocidapterException("${viewHolderElement.simpleName} has more than one @Unbind annotated method.")
                        }
                        unbindFunction = FunctionName.from(bindElement)
                    }

                    attachFunctionList?.find { it == enclosedElement }?.let { attachElement ->
                        if (attachFunction != null) {
                            throw VelocidapterException("${viewHolderElement.simpleName} has multiple @OnAttachToWindow annotated methods.")
                        }
                        attachFunction = FunctionName.from(attachElement)
                    }

                    detatchFunctionList?.find { it == enclosedElement }?.let { detachElement ->
                        if (detachFunction != null) {
                            throw VelocidapterException("${viewHolderElement.simpleName} has multiple @OnDetachFromWindow annotated methods.")
                        }
                        detachFunction = FunctionName.from(detachElement)
                    }
                }

                if (bindFunction == null) {
                    throw VelocidapterException("${viewHolderElement.simpleName} is missing an @Bind method. $VIEW_BIND_INSTRUCTION")
                }

                callback(bindFunction, unbindFunction, attachFunction, detachFunction)

            }

            // all ViewHolder Classes
            roundEnv?.getElementsAnnotatedWith(ViewHolder::class.java)
                ?.filter {
                    (it.kind == ElementKind.CLASS)
                }
                ?.forEach { viewHolderElement ->
                    getFunctions(viewHolderElement) { bindFunction, unbindFunction, attachFunction, detachFunction ->
                        val annotation = viewHolderElement.getAnnotation(ViewHolder::class.java)!!
                        val name = getFullPath(viewHolderElement)
                        // fetch first viewBinding from constructor
                        val binding = viewHolderElement.enclosedElements
                            .mapNotNull { element ->
                                if (element.kind != ElementKind.CONSTRUCTOR) return@mapNotNull null
                                if (element !is ExecutableElement) return@mapNotNull null
                                // must have one param
                                if (element.parameters.size != 1) return@mapNotNull null
                                val executableType = (element.asType() as ExecutableType)
                                val parameters = executableType.parameterTypes
                                val viewBindingParam = parameters.getOrNull(0) as? DeclaredType
                                val viewBindingTypeElement = (viewBindingParam?.asElement() as? TypeElement)
                                // param must be a view binding
                                if (viewBindingTypeElement?.interfaces?.firstOrNull()
                                        ?.toString() != "androidx.viewbinding.ViewBinding"
                                ) return@mapNotNull null
                                val viewBindingParamName = viewBindingTypeElement.qualifiedName?.toString()
                                    ?: return@mapNotNull null
                                viewBindingTypeElement
                            }
                            .firstOrNull()
                            ?: throw VelocidapterException("@ViewHolder for class ${viewHolderElement} must have a constructor with a single param that is of type ViewBinding")


                        val viewHolder = BindMethodViewHolderBuilder(getFullPath(viewHolderElement),
                            bindFunction!!, unbindFunction, attachFunction, detachFunction) {
                            addStatement(
                                "val binding = %T.inflate(%T.from(viewGroup.context), viewGroup, false)",
                                ClassName.bestGuess(binding.asType().toString()),
                                ClassName("android.view", "LayoutInflater"),
                            )
                            addStatement("return@FunctionalAdapter·%T(binding)", ClassName.bestGuess(name))
                        }

                        annotation.adapters.forEach { adapterName ->
                            if (!adapterList.any { it.name == adapterName }) {
                                adapterList.add(BindableAdapter(adapterName))
                            }
                            adapterList.find { it.name == adapterName }?.viewHolders?.add(viewHolder)
                        }
                    }
                }

            // all ViewBinding
            roundEnv?.getElementsAnnotatedWith(ViewHolder::class.java)
                ?.filter {
                    (it.kind == ElementKind.METHOD)
                }
                ?.forEach { viewHolderBindingElement ->

                    val bindFunction = ViewBindFunction.from(viewHolderBindingElement)
                    val annotation = viewHolderBindingElement.getAnnotation(ViewHolder::class.java)!!

                    val viewHolder = ClassViewHolderBuilder(
                        VIEW_HOLDER_VIEW_BINDING_FULL,
                        ClassName.bestGuess(bindFunction.bindingType),
                        bindFunction,
                        unbindFunction = null,
                        attachFunction = null,
                        detachFunction = null
                    )

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

            true
        } catch (e: Exception) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.localizedMessage + "\r\n")
            e.stackTrace.forEach {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, it.toString() + "\r\n")
            }
            false
        }
    }

    private fun getDataList(adapter: BindableAdapter): TypeSpec {
        val typeSpec = TypeSpec.classBuilder(adapter.dataListName)
            .superclass(ClassName("com.bleacherreport.velocidapterandroid", "ScopedDataList"))

        val dataClassNames = mutableListOf<ClassName>()
        adapter.viewHolders.forEach { viewHolder ->
            val argumentClass = viewHolder.bindFunction.argumentType
            val simpleClassName = argumentClass.substringAfterLast(".")
            val argumentClassName = ClassName.bestGuess(argumentClass)
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
            .parameterizedBy(ClassName("java.lang", "Class")
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
                .parameterizedBy(ClassName("com.bleacherreport.velocidapter", adapter.dataListName)))

        return typeSpec.build()
    }

    private fun getAdapterKtx(adapter: BindableAdapter): FunSpec {
        val funSpec = FunSpec.builder("attach${adapter.name}")
            .receiver(ClassName("androidx.recyclerview.widget", "RecyclerView"))
            .returns(ClassName("com.bleacherreport.velocidapterandroid", "AdapterDataTarget")
                .parameterizedBy(ClassName.bestGuess(adapter.dataListName)))
            .addStatement("val adapter = %T(⇥",
                ClassName("com.bleacherreport.velocidapterandroid", "FunctionalAdapter")
                    .parameterizedBy(ClassName.bestGuess(adapter.dataListName))
            )
            .addCode("%L,\n", getCreateViewHolderLambda(adapter.viewHolders))
            .addCode("%L,\n", getBindViewHolderLamda(adapter.viewHolders))
            .addCode("%L,\n", FunctionName.from(adapter.viewHolders) { unbindFunction })
            .addCode("%L,\n", getItemTypeLambda(adapter.viewHolders))
            .addCode("%L,\n", FunctionName.from(adapter.viewHolders) { attachFunction })
            .addCode("%L\n", FunctionName.from(adapter.viewHolders) { detachFunction })
            .addStatement("⇤)")
            .addStatement("this.adapter = adapter")
            .addStatement("return adapter")
        return funSpec.build()
    }

    private fun getCreateViewHolderLambda(viewHolders: Set<BaseViewHolderBuilder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ viewGroup, type ->")
            viewHolders.forEachIndexed { index, viewHolder ->
                beginControlFlow("if (type == $index)")
                viewHolder.createViewHolder(this)
                endControlFlow()
            }
            addStatement("throw RuntimeException(%S)", "Type not found ViewHolder set.")
            endControlFlow()
        }
    }

    private fun getItemTypeLambda(viewHolders: Set<BaseViewHolderBuilder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ position, dataset ->")
            addStatement("val dataItem = dataset[position]")
            viewHolders.forEachIndexed { index, viewHolder ->
                beginControlFlow(
                    "if (dataItem::class == %T::class)",
                    ClassName.bestGuess(viewHolder.bindFunction.argumentType)
                )
                addStatement("return@FunctionalAdapter $index")
                endControlFlow()
            }
            addStatement("return@FunctionalAdapter -1")
            endControlFlow()
        }
    }

    private fun getBindViewHolderLamda(viewHolders: Set<BaseViewHolderBuilder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ viewHolder, position, dataset ->")

            //ViewHolder
            viewHolders.filterIsInstance<BindMethodViewHolderBuilder>().forEachIndexed { index, viewHolder ->
                viewHolder.bindFunction.apply {
                    createBindFunction(viewHolderClassName = viewHolder.name)
                }
            }

            //ViewBinding
            viewHolders.filterIsInstance<ClassViewHolderBuilder>().takeIf { it.isNotEmpty() }?.also { viewHolders ->
                val viewHolderClassName = ClassName.bestGuess(VIEW_HOLDER_VIEW_BINDING_FULL)
                beginControlFlow("if (viewHolder::class == %T::class)", viewHolderClassName)
                addStatement(
                    "val typedViewHolder = (viewHolder as %T)",
                    viewHolderClassName,
                )
                viewHolders.forEachIndexed { index, viewHolder ->
                    viewHolder.bindFunction.apply {
                        createBindFunction(viewHolderClassName = viewHolder.name)
                    }
                }
                endControlFlow()
            }

            endControlFlow()
        }
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}

class VelocidapterException(override val message: String?) : java.lang.Exception(message)


data class BindableAdapter(val name: String, val viewHolders: MutableSet<BaseViewHolderBuilder> = LinkedHashSet()) {
    val dataListName
        get() = name + "DataList"
    val dataTargetName
        get() = name + "DataTarget"
}



