package com.horie1024.plugins.generate.datalayer.lib

import com.squareup.kotlinpoet.*
import io.swagger.models.HttpMethod
import io.swagger.models.Operation
import io.swagger.models.Swagger
import io.swagger.models.properties.RefProperty
import java.io.File

/**
 * Generate files related to the data layer
 */
class DataLayerGenerator(private val swagger: Swagger) : Generator {

    var dataClassPackageName: String = ""
    var dataClassFilePath: String? = null
    var serviceClassPackageName: String = ""
    var serviceClassFilePath: String? = null

    /**
     * Generate data class
     */
    override fun generateDataClass() {
        swagger.definitions.forEach { typeName, parameter ->

            val typeSpecBuilder = TypeSpec.classBuilder(typeName).addModifiers(KModifier.DATA)
            val funSpecBuilder = FunSpec.constructorBuilder()

            parameter.properties.forEach {

                val propertyName = it.key.split("_").let { it[0] + it.getOrElse(1, { "" }).capitalize() }

                val type = when (it.value.type) {
                    "string" -> String::class.asTypeName()
                    "number" -> Int::class.asTypeName()
                    "integer" -> Int::class.asTypeName()
                    "array" -> {
                        val type = it.value.getDefinitionsTypeName()
                        if (type != null) {
                            ParameterizedTypeName.get(List::class.asTypeName(), ClassName("", type))
                        } else {
                            Any::class.asTypeName()
                        }
                    }
                    else -> {
                        // TODO 以下のパターンに対応
//                         "complete": {
//                        "type": "boolean",
//                        "default": false
                        //}

                        val ref = (it.value as? RefProperty)?.simpleRef
                        if (ref != null) {
                            ClassName("", ref)
                        } else {
                            Any::class.asTypeName()
                        }
                    }
                }

                typeSpecBuilder
                        .addProperty(PropertySpec.builder(propertyName, type)
                                .initializer(propertyName)
                                .build())
                funSpecBuilder.addParameter(propertyName, type)
            }

            FileSpec.builder(dataClassPackageName, typeName)
                    .addType(typeSpecBuilder.primaryConstructor(funSpecBuilder.build()).build())
                    .build()
                    .let {
                        if (dataClassFilePath != null) it.writeTo(File(dataClassFilePath)) else it.writeTo(System.out)
                    }
        }
    }

    /**
     * Generate Retrofit interface
     */
    override fun generateRetrofitInterface() {

        val tagList = mutableMapOf<String, MutableMap<String, MutableList<Map.Entry<HttpMethod, Operation>>>>()

        swagger.paths.forEach { path, spec ->
            spec.operationMap.map {
                tagList.getOrPut(it.value.tags[0], { mutableMapOf() }).getOrPut(path, { mutableListOf() }).add(it)
            }
        }

        tagList.forEach {
            val (tag, paths) = it
            val funList = mutableListOf<FunSpec.Builder>()

            paths.forEach { path, operationMap ->
                operationMap.forEach {
                    val (method, operation) = it

                    val returnType = operation.getReturnType()

                    FunSpec.builder(operation.operationId)
                            .addModifiers(KModifier.ABSTRACT)
                            .buildHttpMethodAnnotation(method, path, returnType)
                            .buildParameters(operation.parameters)
                            .let { funList.add(it) }
                }
            }

            FileSpec.builder(serviceClassPackageName, "${tag.capitalize()}Service")
                    .addType(TypeSpec
                            .interfaceBuilder("${tag.capitalize()}Service")
                            .apply { funList.forEach { addFunction(it.build()) } }
                            .build())
                    .build()
                    .let {
                        if (serviceClassFilePath != null) it.writeTo(File(serviceClassFilePath)) else it.writeTo(System.out)
                    }
        }
    }
}