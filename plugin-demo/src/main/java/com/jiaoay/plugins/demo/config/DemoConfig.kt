package com.jiaoay.plugins.demo.config

import org.gradle.api.Project

open class DemoConfig {

    companion object {
        internal const val Name = "pluginDemo"

        fun get(project: Project): DemoConfig {
            val config = project.extensions.getByName(Name)
            if (config is DemoConfig) {
                return config
            }
            return DemoConfig()
        }
    }

    var isEnable: Boolean = false

    var tag: String = "pluginDemo"
}
