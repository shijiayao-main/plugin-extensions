package com.jiaoay.plugins.trace.config

import com.jiaoay.plugins.core.config.PluginConfig

class TraceConfigImpl : PluginConfig {
    override fun getConfigClass(): Class<*> {
        return TraceConfig::class.java
    }

    override fun getConfigName(): String {
        return TraceConfig.NAME
    }
}
