package com.jiaoay.plugins.core.config

class ExtensionsPluginConfigImpl : PluginConfig {
    override fun getConfigClass(): Class<*> {
        return ExtensionsPluginConfig::class.java
    }

    override fun getConfigName(): String {
        return ExtensionsPluginConfig.Name
    }
}