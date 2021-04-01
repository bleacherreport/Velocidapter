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
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.tools.Diagnostic

private const val VELOCIDAPTER_PATH = "com.bleacherreport.velocidapterandroid"
private const val VIEW_HOLDER_VIEW_BINDING = "VelocidapterViewHolder"
private const val VIEW_HOLDER_VIEW_BINDING_FULL = "$VELOCIDAPTER_PATH.$VIEW_HOLDER_VIEW_BINDING"


private const val VIEW_BIND_INSTRUCTION = """
A Velocidapter ViewBinding method must have one @ViewHolderBind annotated method which must follow the below steps:
  1) The method must extend a ViewBinding or pass a ViewBinding as the first argument
  2) The next argument must be the data model you bind with (Any)"""

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
                        bindFunction = createBindFunction(bindElement, viewHolderElement.simpleName.toString())
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
                            // get constructors
                            .mapNotNull {
                                when (it.kind == ElementKind.CONSTRUCTOR) {
                                    true -> it as ExecutableElement
                                    false -> null
                                }
                            }
                            .mapNotNull { element ->
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
                            }?.firstOrNull()
                            ?: throw VelocidapterException("@ViewHolder for class ${viewHolderElement} must have a constructor with a single param that is of type ViewBinding $VIEW_BIND_INSTRUCTION")


                        val viewHolder = BindableLayoutViewHolder(getFullPath(viewHolderElement),
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

                    val bindFunction = createViewBindingFunction(viewHolderBindingElement)
                    val annotation = viewHolderBindingElement.getAnnotation(ViewHolder::class.java)!!

                    val viewHolder = ViewBindableViewHolder(
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

    private fun createViewBindingFunction(element: Element): ViewBindFunction {
        val executableType = (element.asType() as ExecutableType)
        val parameters = executableType.parameterTypes


        val viewBindingParam = parameters.getOrNull(0) as? DeclaredType
        val viewBindingTypeElement = (viewBindingParam?.asElement() as? TypeElement)
        if (viewBindingTypeElement?.interfaces?.firstOrNull()?.toString() != "androidx.viewbinding.ViewBinding")
            throw VelocidapterException("@ViewHolderBind method ${element.methodFullName()} should be an extension method for a ViewBinding. $VIEW_BIND_INSTRUCTION")
        val viewBindingParamName = viewBindingTypeElement.qualifiedName?.toString()
            ?: throw VelocidapterException("@ViewHolderBind method ${element.methodFullName()} should be an extension method for a ViewBinding. $VIEW_BIND_INSTRUCTION")

        val dataParamName = parameters.getOrNull(1)?.let {
            if (it is DeclaredType) {
                var name = (it.asElement() as TypeElement).qualifiedName.toString()
                if (name == "java.lang.String") name = "kotlin.String"
                name
            } else {
                resolvePrimitiveType((it as PrimitiveType).toString())
            }
        }
            ?: throw VelocidapterException("@ViewHolderBind method ${element.methodFullName()} second param should be a data param. $VIEW_BIND_INSTRUCTION")


        if (parameters.size > 2) {
            throw VelocidapterException("@ViewHolderBind method ${element.methodFullName()} has too many params. $VIEW_BIND_INSTRUCTION")
        }

        return ViewBindFunction(
            element = element,
            bindingType = viewBindingParamName,
            argumentType = dataParamName
        )
    }


    private fun createBindFunction(element: Element, viewHolderName: String): PositionBindFunction {
        val parameters = (element.asType() as ExecutableType).parameterTypes
        if (parameters.size != 2 && parameters.size != 1) {
            throw VelocidapterException("@Bind method $viewHolderName.${element.simpleName} does not have the right number of parameters. $VIEW_BIND_INSTRUCTION")
        }

        val dataParam = parameters[0]
        if (parameters.size > 1) {
            if (parameters[1] !is PrimitiveType || parameters[1].kind != TypeKind.INT) {
                throw VelocidapterException("@Bind method $viewHolderName.${element.simpleName} second parameter is not an Int. $VIEW_BIND_INSTRUCTION")
            }
        }

        val dataArgumentType = if (dataParam is DeclaredType) {
            var name = (dataParam.asElement() as TypeElement).qualifiedName.toString()
            if (name == "java.lang.String") name = "kotlin.String"
            name
        } else {
            resolvePrimitiveType((dataParam as PrimitiveType).toString())
        }

        return PositionBindFunction(element, dataArgumentType, parameters.size > 1)
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
            .addCode("%L,\n", getFunctionNames(adapter.viewHolders) { unbindFunction })
            .addCode("%L,\n", getItemTypeLambda(adapter.viewHolders))
            .addCode("%L,\n", getFunctionNames(adapter.viewHolders) { attachFunction })
            .addCode("%L\n", getFunctionNames(adapter.viewHolders) { detachFunction })
            .addStatement("⇤)")
            .addStatement("this.adapter = adapter")
            .addStatement("return adapter")
        return funSpec.build()
    }


    private fun getCreateViewHolderLambda(viewHolders: Set<BaseViewHolder>): CodeBlock {
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

    private fun getItemTypeLambda(viewHolders: Set<BaseViewHolder>): CodeBlock {
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

    private fun getBindViewHolderLamda(viewHolders: Set<BaseViewHolder>): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ viewHolder, position, dataset ->")

            //ViewHolder
            viewHolders.filterIsInstance<BindableLayoutViewHolder>().forEachIndexed { index, viewHolder ->
                val viewHolderClassName = ClassName.bestGuess(viewHolder.name)
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
                    ClassName.bestGuess(viewHolder.bindFunction.argumentType)
                )
                endControlFlow()
            }

            //ViewBinding
            viewHolders.filterIsInstance<ViewBindableViewHolder>().takeIf { it.isNotEmpty() }?.also { viewHolders ->
                val viewHolderClassName = ClassName.bestGuess(VIEW_HOLDER_VIEW_BINDING_FULL)
                beginControlFlow("if (viewHolder::class == %T::class)", viewHolderClassName)
                addStatement(
                    "val typedViewHolder = (viewHolder as %T)",
                    viewHolderClassName,
                )
                viewHolders.forEachIndexed { index, viewHolder ->
                    beginControlFlowElseIf(
                        index,
                        "if (typedViewHolder.dataType == %T::class)",
                        ClassName.bestGuess(viewHolder.bindFunction.argumentType)
                    )

                    addStatement(
                        "typedViewHolder.bind(dataset[position] as %T, position)",
                        ClassName.bestGuess(viewHolder.bindFunction.argumentType),
                    )

                    endControlFlow()
                }
                endControlFlow()
            }

            endControlFlow()
        }
    }

    fun CodeBlock.Builder.beginControlFlowElseIf(index: Int, controlFlow: String, vararg args: Any?) =
        beginControlFlow((if (index == 0) "" else " else ") + controlFlow, *args)

    private fun getFunctionNames(
        viewHolders: Set<BaseViewHolder>,
        fetch: BaseViewHolder.() -> FunctionName?,
    ): CodeBlock {
        return buildCodeBlock {
            beginControlFlow("{ viewHolder ->")
            viewHolders.filterIsInstance<BindableLayoutViewHolder>().forEach { viewHolder ->
                val functionName = viewHolder.fetch() ?: return@forEach
                beginControlFlow(
                    "if (viewHolder::class == %T::class)",
                    ClassName.bestGuess(viewHolder.name)
                )
                addStatement("(viewHolder as %T).%N()",
                    ClassName.bestGuess(viewHolder.name),
                    functionName.functionName)
                endControlFlow()
            }
            viewHolders.filterIsInstance<ViewBindableViewHolder>().filter { it.fetch() != null }
                .takeIf { it.isNotEmpty() }?.also { viewHolders ->
                    beginControlFlow(
                        "if (viewHolder::class == %T::class)",
                        ClassName.bestGuess(VIEW_HOLDER_VIEW_BINDING_FULL)
                    )
                    addStatement("val typedViewHolder = viewHolder as %T",
                        ClassName.bestGuess(VIEW_HOLDER_VIEW_BINDING_FULL)
                    )
                    viewHolders.forEachIndexed { index, viewHolder ->
                        beginControlFlowElseIf(
                            index,
                            "if (typedViewHolder.dataType == %T::class)",
                            ClassName.bestGuess(viewHolder.bindFunction.argumentType),
                        )
                        viewHolder.fetch()?.also {
                            addStatement("typedViewHolder.${it.functionName}()",
                                ClassName.bestGuess(viewHolder.name))
                        }
                        endControlFlow()
                    }
                    endControlFlow()
                }
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

interface BaseBindFunction {
    val element: Element
    val argumentType: String
    val functionName: String
        get() = element.simpleName.toString().split("(")[0]
    val functionFullName: String
        get() = element.simpleName.toString().split("(")[0]

}

class ViewBindFunction(
    override val element: Element,
    val bindingType: String,
    override val argumentType: String,
) : BaseBindFunction

class PositionBindFunction(
    override val element: Element,
    override val argumentType: String,
    val hasPositionParameter: Boolean,
) :
    BaseBindFunction

class FunctionName(
    val functionName: String,
) {
    companion object {
        fun from(element: Element) = FunctionName(element.simpleName.toString().split("(")[0])
    }
}

data class BindableAdapter(val name: String, val viewHolders: MutableSet<BaseViewHolder> = LinkedHashSet()) {
    val dataListName
        get() = name + "DataList"
    val dataTargetName
        get() = name + "DataTarget"
}

interface BaseViewHolder {
    val name: String
    val bindFunction: BaseBindFunction
    val unbindFunction: FunctionName?
    val attachFunction: FunctionName?
    val detachFunction: FunctionName?
    val createViewHolder: CodeBlock.Builder.() -> Unit
}

data class BindableLayoutViewHolder(
    override val name: String,
    override val bindFunction: PositionBindFunction,
    override val unbindFunction: FunctionName?,
    override val attachFunction: FunctionName?,
    override val detachFunction: FunctionName?,
    override val createViewHolder: CodeBlock.Builder.() -> Unit,
) : BaseViewHolder

data class ViewBindableViewHolder(
    override val name: String,
    val binding: ClassName,
    override val bindFunction: ViewBindFunction,
    override val unbindFunction: FunctionName?,
    override val attachFunction: FunctionName?,
    override val detachFunction: FunctionName?,
) : BaseViewHolder {
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


fun Element.methodFullName() = "$enclosingElement.$simpleName"

