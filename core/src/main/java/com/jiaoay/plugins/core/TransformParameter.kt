package com.jiaoay.plugins.core

import com.jiaoay.plugins.core.transform.Transformer
import java.io.Serializable
import org.gradle.api.Project
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.plugins.PluginContainer

data class TransformParameter(
    val name: String,
    val buildscript: ScriptHandler,
    val plugins: PluginContainer,
    val properties: Map<String, Any?>,
    val transformers: Set<Class<Transformer>>
) : Serializable

fun Project.newTransformParameter(name: String): TransformParameter {
    return TransformParameter(
        name = name,
        buildscript = buildscript,
        plugins = plugins,
        properties = properties,
        transformers = lookupTransformers(buildscript.classLoader)
    )
}