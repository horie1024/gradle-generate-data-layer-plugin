package com.horie1024.plugins.generate.datalayer.plugin

import com.horie1024.plugins.generate.datalayer.lib.DataLayerGenerator
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateDataLayerPluginProperties : DefaultTask() {

    init {
        group = "build"
        description = "Generate data layer from swagger.yml"
    }

    @TaskAction
    fun createGenerateDataLayerProperties() {

        val extensions = project.extensions.getByName("generate_data_layer") as Options

        val swagger: Swagger = SwaggerParser().read(extensions.swagger_json_path)

        DataLayerGenerator(swagger).apply {
            dataClassFilePath = extensions.data_layer_path
            serviceClassFilePath = extensions.data_layer_path
        }.run {
            generateDataClass()
            generateRetrofitInterface()
        }
    }
}