package com.jiaoay.plugins.demo.config

import com.jiaoay.plugins.core.config.PluginConfig

class DemoConfigImpl : PluginConfig {
    override fun getConfigClass(): Class<*> {
        return DemoConfig::class.java
    }

    override fun getConfigName(): String {
        return DemoConfig.NAME
    }
}
