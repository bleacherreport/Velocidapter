package com.bleacherreport.adaptergen

import com.bleacherreport.adaptergenanotations.Bind
import com.bleacherreport.adaptergenanotations.ViewHolder
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
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
import javax.lang.model.util.Elements


@AutoService(Processor::class)
class AdapterGenProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(ViewHolder::class.java.name, Bind::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }


    override fun process(set: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val adapterMap = HashMap<String, HashSet<String>>()
        val layoutResIdMap = HashMap<String, Int>()
        val methodArgumentMap = HashMap<Element, String>()

        roundEnv?.getElementsAnnotatedWith(Bind::class.java)?.forEach { element ->
            val executableType = element.asType() as ExecutableType
            val parameters = executableType.parameterTypes
            if (parameters.size > 2) {
                throw RuntimeException("@Bind method must take one Any and one Int")
            }
            val param1 = parameters[0]

            if(param1 is DeclaredType) {
                var name = (param1.asElement() as TypeElement).qualifiedName.toString()
                if(name == "java.lang.String") name = "kotlin.String"
                methodArgumentMap[element] = name
            } else {
                methodArgumentMap[element] = resolvePrimitiveType((param1 as PrimitiveType).toString())
            }

        }

        val argumentTypeMap = HashMap<String, String>()

        roundEnv?.getElementsAnnotatedWith(ViewHolder::class.java)?.forEach { element ->
            println(element.simpleName)
            val annotation = element.getAnnotation(ViewHolder::class.java)!!
            annotation.adapters.forEach { adapter ->
                if (!adapterMap.containsKey(adapter)) {
                    adapterMap[adapter] = HashSet()
                }

                adapterMap[adapter]?.add(getFullPath(element))
            }

            layoutResIdMap[getFullPath(element)] = annotation.layoutResId

            element.enclosedElements.forEach { enclosedElement ->
                methodArgumentMap[enclosedElement]?.let { argument ->
                    argumentTypeMap[getFullPath(element)] = argument
                }
            }
        }

        adapterMap.forEach { entry ->

            val builder = FileSpec.builder("com.bleacherreport.adaptergen", entry.key)

            val dataListName = entry.key + "DataList"
            builder.addType(getDataList(dataListName, entry.value, argumentTypeMap))

            val dataTargetName = entry.key + "DataTarget"
            builder.addType(getDataTarget(dataTargetName, dataListName))

            builder.addFunction(getAdapterKtx(entry.key, entry.value, layoutResIdMap, argumentTypeMap, dataListName))

            val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            builder.build().writeTo(File(kaptKotlinGeneratedDir, "${entry.key}.kt"))
        }

        return true
    }

    private fun getDataList(name: String, viewHolderTypes: Set<String>, argumentTypeMap: Map<String, String>): TypeSpec {
        val typeSpec = TypeSpec.classBuilder(name)
                .superclass(ClassName("com.bleacherreport.adaptergenandroid", "ScopedDataList"))

        viewHolderTypes.forEach { viewHolderType ->
            val argumentClassName = argumentTypeMap[viewHolderType]!!
            val simpleClassName = argumentClassName.split(".").last()
            typeSpec.addFunction(FunSpec.builder("add")
                    .addParameter("model", ClassName("", argumentClassName))
                    .addCode("listInternal.add(model)")
                    .build())

            typeSpec.addFunction(FunSpec.builder("addListOf$simpleClassName")
                    .addParameter("list", ClassName("", "kotlin.collections.List<$argumentClassName>"))
                    .addCode("listInternal.addAll(list)")
                    .build())
        }

        return typeSpec.build()
    }

    private fun getDataTarget(name: String, dataListName: String): TypeSpec {
        val typeSpec = TypeSpec.interfaceBuilder(name)
                .addSuperinterface(ClassName("", "com.bleacherreport.adaptergenandroid.AdapterDataTarget<$dataListName>"))

        return typeSpec.build()
    }

    private fun getAdapterKtx(adapterName: String, viewHolderTypes: Set<String>, layoutResIdMap: Map<String, Int>,
                              argumentTypeMap: Map<String, String>, dataListName: String): FunSpec {
        val funSpec = FunSpec.builder("attach$adapterName")
                .receiver(ClassName("", "android.support.v7.widget.RecyclerView"))
                .returns(ClassName("", "com.bleacherreport.adaptergenandroid.AdapterDataTarget<$dataListName>"))
                .addCode("""
                    |
                    |val adapter = com.bleacherreport.adaptergenandroid.FunctionalAdapter<$dataListName>(
                    |   ${getCreateViewHolderLambda(viewHolderTypes, layoutResIdMap)},
                    |   ${getBindViewHolderLamda(viewHolderTypes, argumentTypeMap)},
                    |   ${getItemTypeLambda(viewHolderTypes, argumentTypeMap)})
                    |
                    |this.adapter = adapter
                    |return adapter
                    |
                    |""".trimMargin())

        return funSpec.build()
    }


    private fun getCreateViewHolderLambda(viewHolderTypes: Set<String>, layoutResIdMap: Map<String, Int>): String {
        var code = """
            |   { viewGroup, type ->
        """.trimMargin()

        viewHolderTypes.forEachIndexed { index, viewHolderClass ->
            code += """
                |
                |        if(type == $index) {
                |           val view = android.view.LayoutInflater.from(viewGroup.context).inflate(${layoutResIdMap[viewHolderClass]}, viewGroup, false)
                |           return@FunctionalAdapter $viewHolderClass(view)
                |        }
                |""".trimMargin()
        }

        code += """
            |       throw RuntimeException("Type not found ViewHolder set.")
            |       }""".trimMargin()

        return code
    }


    private fun getBindViewHolderLamda(viewHolderTypes: Set<String>, argumentTypeMap: Map<String, String>): String {
        var code = """
            |   { viewHolder, position, dataset ->
        """.trimMargin()

        viewHolderTypes.forEach { viewHolderClass ->
            code += """
                |
                |        if(viewHolder::class == $viewHolderClass::class) {
                |           (viewHolder as $viewHolderClass).bindModel(
                |               dataset[position] as ${argumentTypeMap[viewHolderClass]}, position)
                |        }
                |""".trimMargin()
        }
        code += """
            |       }""".trimMargin()
        return code
    }


    private fun getItemTypeLambda(viewHolderTypes: Set<String>, argumentTypeMap: Map<String, String>): String {
        var code = """
            |   { position, dataset ->
            |       val dataItem = dataset[position]
            |""".trimMargin()

        viewHolderTypes.forEachIndexed { index, viewHolderClass ->
            code += """
                |
                |       if(dataItem::class == ${argumentTypeMap[viewHolderClass]}::class) return@FunctionalAdapter $index
                |""".trimMargin()
        }
        code += """
            |
            |       return@FunctionalAdapter -1
            |       }
            |""".trimMargin()

        return code
    }

    private fun getFullPath(element: Element): String {
        return if(element is TypeElement) {
            var enclosing = element
            while (enclosing.kind != ElementKind.PACKAGE) {
                enclosing = enclosing.enclosingElement
            }
            val packageElement = enclosing as PackageElement
            var path = packageElement.qualifiedName.toString() + "." + element.simpleName.toString()
            if(path == "java.lang.String") path = "kotlin.String"
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