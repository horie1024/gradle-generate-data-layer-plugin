package com.horie1024.plugins.generate.datalayer.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class GenerateInfraLayerPlugin : Plugin<Project> {

    override fun apply(project: Project?) {

        project ?: return

        with(project) {
            extensions.create("generate_data_layer", Options::class.java)
            tasks.create("generateDataLayer", GenerateDataLayerPluginProperties::class.java).let { tasks.add(it) }
        }
    }
}

open class Options {
    var data_layer_path: String? = null
    var swagger_json_path: String? = null
}