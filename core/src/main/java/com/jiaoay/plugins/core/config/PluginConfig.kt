package com.jiaoay.plugins.core.config

interface PluginConfig {
    fun getConfigClass(): Class<*>

    fun getConfigName(): String
}
