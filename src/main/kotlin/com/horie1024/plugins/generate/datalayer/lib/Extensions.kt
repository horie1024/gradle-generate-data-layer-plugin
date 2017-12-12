package com.horie1024.plugins.generate.datalayer.lib

import com.squareup.kotlinpoet.*
import io.reactivex.Single
import io.swagger.models.HttpMethod
import io.swagger.models.Operation
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.Property
import io.swagger.models.properties.RefProperty
import retrofit2.http.*

internal fun Property.getDefinitionsTypeName(): String? {
    return if (this.type == "array") {
        val ref = ((this as? ArrayProperty)?.items as? RefProperty)?.simpleRef
        return ref
        // TODO ?: (this as? ArrayProperty)?.items?.type // stringの場合にString::class.asTypeNameを返す
    } else {
        (this as? RefProperty)?.simpleRef
    }
}

internal fun Operation.getReturnType(): ParameterizedTypeName? {

    this.responses.forEach {
        if (it.key != "200") return ParameterizedTypeName.get(Single::class.asTypeName(), Unit::class.asTypeName())

        val refTypeName = it.value.schema.getDefinitionsTypeName() ?: return ParameterizedTypeName.get(Single::class.asTypeName(), Unit::class.asTypeName())
        val className = ClassName("", refTypeName) // TODO packagenameはdata classが格納されているpackagenameになるようにする

        return ParameterizedTypeName.get(Single::class.asTypeName(), when (it.value.schema.type) {
            "array" -> {
                ParameterizedTypeName.get(List::class.asTypeName(), className)
            }
            else -> className
        })
    }

    return null
}

internal fun FunSpec.Builder.buildHttpMethodAnnotation(method: HttpMethod, path: String, returnType: ParameterizedTypeName?): FunSpec.Builder {
    return when (method) {
        HttpMethod.POST -> {
            this.also { builder ->
                returnType?.let { builder.returns(it) }
            }.addAnnotation(AnnotationSpec
                    .builder(POST::class)
                    .addMember("\"$path\"")
                    .build())
        }
        HttpMethod.GET -> {
            this.also { builder ->
                returnType?.let { builder.returns(it) }
            }.addAnnotation(AnnotationSpec
                    .builder(GET::class)
                    .addMember("\"$path\"")
                    .build())
        }
        HttpMethod.PUT -> {
            this.also { builder ->
                returnType?.let { builder.returns(it) }
            }.addAnnotation(AnnotationSpec
                    .builder(PUT::class)
                    .addMember("\"$path\"")
                    .build())
        }
        HttpMethod.PATCH -> this
        HttpMethod.DELETE -> {
            this.also { builder ->
                returnType?.let { builder.returns(it) }
            }.addAnnotation(AnnotationSpec
                    .builder(DELETE::class)
                    .addMember("\"$path\"")
                    .build())
        }
        HttpMethod.HEAD -> this
        HttpMethod.OPTIONS -> this
    }
}

internal fun FunSpec.Builder.buildParameters(parameters: List<Parameter>): FunSpec.Builder {

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
                        .builder(Path::class)
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
                        .builder(Query::class)
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