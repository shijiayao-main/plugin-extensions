package com.jiaoay.plugins.core

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.didiglobal.booster.gradle.GTE_V3_6
import com.didiglobal.booster.gradle.getAndroid
import com.jiaoay.plugins.core.spi.VariantProcessor
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.reflect.TypeOf

class ExtensionsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        pluginProject = project
        project.extensions.findByName("android") ?: throw GradleException("$project is not an Android project")

        if (!GTE_V3_6) {
            project.gradle.addListener(
                TransformTaskExecutionListener(
                    project = project
                )
            )
        }

//        Config
        project.loadConfig()

        val appPlugin = project.plugins.hasPlugin("com.android.application") || project.plugins.hasPlugin("com.android.dynamic-feature")
        val libPlugin = project.plugins.hasPlugin("com.android.library")

        if (appPlugin) {
            val extensions: AppExtension? = project.extensions.findByType(TypeOf.typeOf(AppExtension::class.java))
            extensions?.registerTransform(ExtensionsPluginTransform.newInstance(project))
        } else if (libPlugin) {
            val extensions: LibraryExtension? = project.extensions.findByType(TypeOf.typeOf(LibraryExtension::class.java))
            extensions?.registerTransform(ExtensionsPluginTransform.newInstance(project))
        }

//         AutoService
        val processors = loadVariantProcessors(project)
        if (project.state.executed) {
            project.setup(processors)
        } else {
            project.afterEvaluate {
                project.setup(processors)
            }
        }
        pluginProject = null
    }

    private fun Project.loadConfig() {
        loadPluginConfig(buildscript.classLoader).forEach { config ->
            val configName = config.getConfigName()
            val configClass = config.getConfigClass()
            project.extensions.create(configName, configClass)
        }
    }

    private fun Project.setup(processors: List<VariantProcessor>) {

        val android = project.getAndroid<BaseExtension>()
        when (android) {
            is AppExtension -> android.applicationVariants
            is LibraryExtension -> android.libraryVariants
            else -> emptyList<BaseVariant>()
        }.takeIf<Collection<BaseVariant>>(Collection<BaseVariant>::isNotEmpty)?.let { variants ->
            variants.forEach { variant ->
                processors.forEach { processor ->
                    processor.process(variant)
                }
            }
        }
    }

}