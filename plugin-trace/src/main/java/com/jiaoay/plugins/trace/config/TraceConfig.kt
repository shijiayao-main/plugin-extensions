package com.jiaoay.plugins.trace.config

import org.gradle.api.Project

open class TraceConfig {

    companion object {
        internal const val Name = "pluginTrace"

        fun get(project: Project): TraceConfig {
            val config = project.extensions.getByName(Name)
            if (config is TraceConfig) {
                return config
            }
            return TraceConfig()
        }
    }

    var isEnable: Boolean = false

    var traceClassSet: MutableSet<String>? = null
}
