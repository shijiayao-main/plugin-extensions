package com.jiaoay.plugins.core.config

import org.gradle.api.Project

open class ExtensionsPluginConfig {

    companion object {
        internal const val Name = "extensionsPlugin"

        fun get(project: Project): ExtensionsPluginConfig {
            val config = project.extensions.getByName(Name)
            if (config is ExtensionsPluginConfig) {
                return config
            }
            return ExtensionsPluginConfig()
        }
    }

    var isEnableSdkPatcher: Boolean = false

    // 如果设置为false则不会自动查找annotation，需要手动配置replaceClassMap等参数以确保正常运行
    var autoSearchAnnotation: Boolean = true

    var replaceClassMap: MutableMap<String, MutableList<String>>? = null
}