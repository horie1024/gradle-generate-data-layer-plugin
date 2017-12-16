package com.horie1024.plugins.generate.datalayer.lib

import com.squareup.kotlinpoet.*
import io.swagger.models.HttpMethod
import io.swagger.models.Operation
import io.swagger.models.Swagger
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.Property
import io.swagger.models.properties.RefProperty

abstract class Generator(protected val swagger: Swagger) {

    var dataClassPackageName: String = ""
    var dataClassFilePath: String? = null
    var serviceClassPackageName: String = ""
    var serviceClassFilePath: String? = null

    enum class ReactiveX(val className: ClassName) {
        SINGLE(ClassName("io.reactivex", "Single"))
    }

    enum class Retrofit(val className: ClassName) {
        PATH(ClassName("retrofit2.http", "Path")),
        QUERY(ClassName("retrofit2.http", "Query")),
        GET(ClassName("retrofit2.http", "GET")),
        POST(ClassName("retrofit2.http", "POST")),
        PUT(ClassName("retrofit2.http", "PUT")),
        PATCH(ClassName("retrofit2.http", "PATCH")),
        DELETE(ClassName("retrofit2.http", "DELETE")),
        HEAD(ClassName("retrofit2.http", "HEAD")),
        OPTIONS(ClassName("retrofit2.http", "OPTIONS"))
    }

    abstract fun generateDataClass()

    abstract fun generateRetrofitInterface()

    protected fun Property.getDefinitionsTypeName(): String? {
        return if (this.type == "array") {
            val ref = ((this as? ArrayProperty)?.items as? RefProperty)?.simpleRef
            return ref
            // TODO ?: (this as? ArrayProperty)?.items?.type // stringの場合にString::class.asTypeNameを返す
        } else {
            (this as? RefProperty)?.simpleRef
        }
    }

    protected fun Operation.getReturnType(): ParameterizedTypeName? {

        responses.forEach {
            if (it.key != "200") return ParameterizedTypeName.get(ReactiveX.SINGLE.className, Unit::class.asTypeName())

            val refTypeName = it.value.schema.getDefinitionsTypeName() ?: return ParameterizedTypeName.get(ReactiveX.SINGLE.className, Unit::class.asTypeName())
            val className = ClassName("", refTypeName) // TODO packagenameはdata classが格納されているpackagenameになるようにする

            return ParameterizedTypeName.get(ReactiveX.SINGLE.className, when (it.value.schema.type) {
                "array" -> {
                    ParameterizedTypeName.get(List::class.asTypeName(), className)
                }
                else -> className
            })
        }

        return null
    }

    protected fun FunSpec.Builder.buildHttpMethodAnnotation(method: HttpMethod, path: String, returnType: ParameterizedTypeName?): FunSpec.Builder {
        return when (method) {
            HttpMethod.POST -> {
                this.also { builder ->
                    returnType?.let { builder.returns(it) }
                }.addAnnotation(AnnotationSpec
                        .builder(Retrofit.POST.className)
                        .addMember("\"$path\"")
                        .build())
            }
            HttpMethod.GET -> {
                this.also { builder ->
                    returnType?.let { builder.returns(it) }
                }.addAnnotation(AnnotationSpec
                        .builder(Retrofit.GET.className)
                        .addMember("\"$path\"")
                        .build())
            }
            HttpMethod.PUT -> {
                this.also { builder ->
                    returnType?.let { builder.returns(it) }
                }.addAnnotation(AnnotationSpec
                        .builder(Retrofit.PUT.className)
                        .addMember("\"$path\"")
                        .build())
            }
            HttpMethod.PATCH -> this
            HttpMethod.DELETE -> {
                this.also { builder ->
                    returnType?.let { builder.returns(it) }
                }.addAnnotation(AnnotationSpec
                        .builder(Retrofit.DELETE.className)
                        .addMember("\"$path\"")
                        .build())
            }
            HttpMethod.HEAD -> this
            HttpMethod.OPTIONS -> this
        }
    }

    protected fun FunSpec.Builder.buildParameters(parameters: List<Parameter>): FunSpec.Builder {

        parameters.forEach {
            when (it) {
                is PathParameter -> {
                    val paramBuilder = ParameterSpec.run {

                        val className = when (it.type) {
                            "string" -> String::class.asTypeName().asNonNullable()
                            "number" -> Int::class.asTypeName().asNonNullable()
                            else -> Any::class.asClassName().asNullable()
                        }

                        builder(it.name, className)

                    }.apply {
                        if (!it.required) {
                            defaultValue(
                                    when (it.type) {
                                        "string" -> "\"\""
                                        "number" -> "0"
                                        "integer" -> "0"
                                        "array" -> ""
                                        else -> throw IllegalStateException()
                                    }
                            )
                        }
                    }.addAnnotation(AnnotationSpec
                            .builder(Retrofit.PATH.className)
                            .addMember("\"${it.name}\"")
                            .build())

                    this.addParameter(paramBuilder.build())
                }
                is QueryParameter -> {
                    val paramBuilder = ParameterSpec.run {
                        val className = when (it.type) {
                            "string" -> {
                                String::class.asTypeName().let { type -> if (it.required) type.asNonNullable() else type.asNullable() }
                            }
                            "number" -> {
                                Int::class.asTypeName().let { type -> if (it.required) type.asNonNullable() else type.asNullable() }
                            }
                            else -> Any::class.asClassName().let { type -> if (it.required) type.asNonNullable() else type.asNullable() }
                        }

                        builder(it.name, className)
                    }.apply {
                        if (!it.required) {
                            defaultValue("null")
                        }
                    }.addAnnotation(AnnotationSpec
                            .builder(Retrofit.QUERY.className)
                            .addMember("\"${it.name}\"")
                            .build())

                    this.addParameter(paramBuilder.build())
                }
                is BodyParameter -> {
                }
            }
        }

        return this
    }
}