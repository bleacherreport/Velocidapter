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
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

val errors = mutableListOf<String>()

fun TypeMirror.typeElement(): TypeElement? {
    val viewBindingParam = this as? DeclaredType
    return (viewBindingParam?.asElement() as? TypeElement)
}

private fun TypeMirror.takeIfViewBinding(): TypeElement? {
    val viewBindingTypeElement = typeElement() ?: return null
    // param must be a view binding
    if (viewBindingTypeElement.interfaces?.firstOrNull()
            ?.toString() != "androidx.viewbinding.ViewBinding"
    ) return null
    viewBindingTypeElement.qualifiedName?.toString()
        ?: return null
    return viewBindingTypeElement
}

@AutoService(Processor::class)
class VelocidapterProcessor : AbstractProcessor() {

    init {
        errors.clear()
    }

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
            val addToAll = mutableListOf<BindMethodViewHolderBuilder>()

            fun getFunctions(
                viewHolderElement: Element,
                viewBinding: String,
                callback: (bindFunction: ViewHolderBindFunction?, unbindFunction: FunctionName?, attachFunction: FunctionName?, detachFunction: FunctionName?) -> Unit,
            ) {
                var bindFunction: ViewHolderBindFunction? = null
                var unbindFunction: FunctionName? = null
                var attachFunction: FunctionName? = null
                var detachFunction: FunctionName? = null

                viewHolderElement.enclosedElements.forEach { enclosedElement ->
                    bindFunctionList?.find { it == enclosedElement }?.let { bindElement ->
                        if (bindFunction != null) {
                            errors.add("${viewHolderElement.simpleName} has multiple @Bind annotated methods. $VIEW_HOLDER_BIND_INSTRUCTION")
                        }
                        bindFunction = ViewHolderBindFunction.from(
                            bindElement,
                            viewHolderName = viewHolderElement.simpleName.toString(),
                            bindingType = viewBinding
                        )
                    }

                    unbindFunctionList?.find { it == enclosedElement }?.let { bindElement ->
                        if (unbindFunction != null) {
                            errors.add("${viewHolderElement.simpleName} has more than one @Unbind annotated method.")
                        }
                        unbindFunction = FunctionName.from(bindElement)
                    }

                    attachFunctionList?.find { it == enclosedElement }?.let { attachElement ->
                        if (attachFunction != null) {
                            errors.add("${viewHolderElement.simpleName} has multiple @OnAttachToWindow annotated methods.")
                        }
                        attachFunction = FunctionName.from(attachElement)
                    }

                    detatchFunctionList?.find { it == enclosedElement }?.let { detachElement ->
                        if (detachFunction != null) {
                            errors.add("${viewHolderElement.simpleName} has multiple @OnDetachFromWindow annotated methods.")
                        }
                        detachFunction = FunctionName.from(detachElement)
                    }
                }


                callback(
                    bindFunction,
                    unbindFunction,
                    attachFunction,
                    detachFunction
                )
            }


            fun addAdapter(adapterName: String, viewHolder: BaseViewHolderBuilder) {
                if (adapterName == "*") {
                    adapterList.forEach {
                        it.viewHolders.add(viewHolder)
                    }
                    return
                }
                if (!adapterList.any { it.name == adapterName }) {
                    adapterList.add(BindableAdapter(adapterName))
                }
                adapterList.find { it.name == adapterName }?.viewHolders?.add(viewHolder)
            }


            // all ViewHolder Classes
            roundEnv?.getElementsAnnotatedWith(ViewHolder::class.java)
                ?.filter {
                    (it.kind == ElementKind.CLASS)
                }
                ?.forEach { viewHolderElement ->

                    val annotation = viewHolderElement.getAnnotation(ViewHolder::class.java)!!
                    val name = getFullPath(viewHolderElement)
                    // fetch first viewBinding from constructor
                    val items: Pair<TypeElement, ExecutableElement> = viewHolderElement.enclosedElements
                        .mapNotNull { element ->
                            if (element.kind != ElementKind.CONSTRUCTOR) return@mapNotNull null
                            if (element !is ExecutableElement) return@mapNotNull null
                            // must have one param
                            val executableType = (element.asType() as ExecutableType)
                            val parameters = executableType.parameterTypes
                            parameters.forEach {
                                it.takeIfViewBinding()?.also { return@mapNotNull it to element }
                            }
                            null
                        }
                        .firstOrNull()
                        ?: kotlin.run {
                            errors.add("@ViewHolder for class ${viewHolderElement.simpleName} must have a constructor with a ViewBinding param")
                            return@forEach
                        }

                    val binding = items.first

                    getFunctions(viewHolderElement,
                        binding.qualifiedName?.toString()!!) { bindFunction, unbindFunction, attachFunction, detachFunction ->

                        if (bindFunction == null) {
                            errors.add("${viewHolderElement.simpleName} is missing an @Bind method. $VIEW_HOLDER_BIND_INSTRUCTION")
                            return@getFunctions
                        }

                        val executableType = (items.second.asType() as ExecutableType)
                        val items = executableType.parameterTypes.mapIndexedNotNull { index, typeMirror ->
                            (items.second.parameters[index]?.toString()
                                ?: return@mapIndexedNotNull null) to
                                    (typeMirror.typeElement() ?: return@mapIndexedNotNull null)
                        }.toMap()


                        val viewHolder = BindMethodViewHolderBuilder(
                            viewHolderElement,
                            getFullPath(viewHolderElement),
                            items.filter { it.value.asType().takeIfViewBinding() == null },
                            bindFunction,
                            unbindFunction,
                            attachFunction,
                            detachFunction) {
                            addStatement(
                                "val binding = %T.inflate(%T.from(viewGroup.context), viewGroup, false)",
                                ClassName.bestGuess(binding.asType().toString()),
                                ClassName("android.view", "LayoutInflater"),
                            )

                            addStatement("return@FunctionalAdapter·%T(${
                                items.mapNotNull {
                                    if (it.value.asType().takeIfViewBinding() != null) return@mapNotNull "binding"
                                    it.key
                                }.joinToString()
                            })", ClassName.bestGuess(name))
                        }

                        annotation.adapters.forEach { adapterName ->
                            addAdapter(adapterName, viewHolder)
                        }
                    }
                }

            // all ViewBinding
            roundEnv?.getElementsAnnotatedWith(ViewHolder::class.java)
                ?.filter {
                    (it.kind == ElementKind.METHOD)
                }
                ?.forEach { viewHolderBindingElement ->

                    val bindFunction = ViewBindingFunction.from(viewHolderBindingElement) ?: return@forEach
                    val annotation = viewHolderBindingElement.getAnnotation(ViewHolder::class.java)!!

                    val viewHolder = ClassViewHolderBuilder(
                        viewHolderBindingElement,
                        VIEW_HOLDER_VIEW_BINDING_FULL,
                        ClassName.bestGuess(bindFunction.bindingType),
                        bindFunction,
                        unbindFunction = null,
                        attachFunction = null,
                        detachFunction = null
                    )

                    annotation.adapters.forEach { adapterName ->
                        addAdapter(adapterName, viewHolder = viewHolder)
                    }
                }

            if (errors.isNotEmpty()) {
                printMessage("")
                errors.forEach {
                    printMessage("ERROR = $it")
                }
                return false
            }

            adapterList.firstOrNull { it.name == "*" }?.let { all ->
                adapterList.remove(all)
                adapterList.forEach {
                    it.viewHolders.addAll(all.viewHolders)
                }
            }

            adapterList.forEach { entry ->
                val builder = FileSpec.builder("com.bleacherreport.velocidapter", entry.name)

                builder.addType(getDataList(entry))
                builder.addType(getDataTarget(entry))
                builder.addFunction(attachAdapter(entry))
                builder.addFunction(builder.createAdapter(entry))

                val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                builder.build().writeTo(File(kaptKotlinGeneratedDir, "${entry.name}.kt"))
            }

            true
        } catch (e: Exception) {
            printMessage("")
            printMessage("ERROR = ${e.message}")
            false
        }
    }

    fun printMessage(message: String?, kind: Diagnostic.Kind = Diagnostic.Kind.ERROR) {
        processingEnv.messager.printMessage(kind, "$message\r\n")
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

    private fun attachAdapter(adapter: BindableAdapter): FunSpec {
        val constructors = mutableMapOf<String, TypeElement>()
        adapter.viewHolders.forEach {
            if (it is BindMethodViewHolderBuilder) {
                constructors.putAll(it.constructorParams)
            }
        }

        val attachSpec = FunSpec.builder("attach${adapter.name}")
            .apply {
                constructors.forEach { (s, typeMirror) ->
                    addParameter(s, typeMirror.asClassName())
                }
            }
            .receiver(ClassName("androidx.recyclerview.widget", "RecyclerView"))
            .returns(ClassName("com.bleacherreport.velocidapterandroid", "AdapterDataTarget")
                .parameterizedBy(ClassName("com.bleacherreport.velocidapter", adapter.dataListName)))
            .addStatement("val adapter = %T.create${adapter.name}(${
                constructors.map { it.key }.joinToString()
            })", ClassName("com.bleacherreport.velocidapterandroid", "VelociCreator"))
            .addStatement("this.adapter = adapter")
            .addStatement("return adapter")

        return attachSpec.build()

    }

    private fun FileSpec.Builder.createAdapter(adapter: BindableAdapter): FunSpec {

        val constructors = mutableMapOf<String, TypeElement>()
        adapter.viewHolders.forEach {
            if (it is BindMethodViewHolderBuilder) {
                constructors.putAll(it.constructorParams)
            }
        }

        val funSpec = FunSpec.builder("create${adapter.name}")
            .apply {
                constructors.forEach { (s, typeMirror) ->
                    addParameter(s, typeMirror.asClassName())
                }
            }
            .receiver(ClassName("com.bleacherreport.velocidapterandroid", "VelociCreator"))
            .returns(ClassName("com.bleacherreport.velocidapterandroid", "FunctionalAdapter")
                .parameterizedBy(ClassName("com.bleacherreport.velocidapter", adapter.dataListName)))
            .addStatement("val adapter = %T(⇥",
                ClassName("com.bleacherreport.velocidapterandroid", "FunctionalAdapter")
                    .parameterizedBy(ClassName("com.bleacherreport.velocidapter", adapter.dataListName))
            )
            .addCode("%L,\n", getCreateViewHolderLambda(adapter.viewHolders))
            .addCode("%L,\n", getBindViewHolderLamda(adapter.viewHolders))
            .addCode("%L,\n", FunctionName.createFunctionFrom(adapter.viewHolders) { unbindFunction })
            .addCode("%L,\n", getItemTypeLambda(adapter.viewHolders))
            .addCode("%L,\n", FunctionName.createFunctionFrom(adapter.viewHolders) { attachFunction })
            .addCode("%L\n", FunctionName.createFunctionFrom(adapter.viewHolders) { detachFunction })
            .addStatement("⇤)")
            .addStatement("return adapter")

        return funSpec.build()
    }

    private fun FileSpec.Builder.getCreateViewHolderLambda(viewHolders: Set<BaseViewHolderBuilder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ viewGroup, type ->")
            viewHolders.forEachIndexed { index, viewHolder ->
                beginControlFlow("if (type == $index)")
                viewHolder.createViewHolder(this, this@getCreateViewHolderLambda)
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
            viewHolders.filterIsInstance<BindMethodViewHolderBuilder>().forEach { viewHolder ->
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
                viewHolders.forEach { viewHolder ->
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


data class BindableAdapter(val name: String, val viewHolders: MutableSet<BaseViewHolderBuilder> = LinkedHashSet()) {
    val dataListName
        get() = name + "DataList"
    val dataTargetName
        get() = name + "DataTarget"
}



