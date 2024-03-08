package com.jiaoay.plugins.core

import com.jiaoay.plugins.trace.PluginTraceInfo

object PluginExtensions {

    var output: ((String) -> Unit)? = null

    fun outputTrace(trace: PluginTraceInfo) {
        output?.invoke(trace.toString())
    }
}
