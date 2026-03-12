package com.jiaoay.plugins.trace.config

import org.gradle.api.Project

open class TraceConfig {

    companion object {
        internal const val NAME = "pluginTrace"

        fun get(project: Project): TraceConfig {
            val config = project.extensions.getByName(NAME)
            if (config is TraceConfig) {
                return config
            }
            return TraceConfig()
        }
    }

    var isEnable: Boolean = false

    /**
     * 耗时统计白名单, 报名中包含对应内容的类会被插桩
     */
    var traceWhiteSet: MutableSet<String>? = null

    /**
     * 耗时统计黑名单, 报名中包含对应内容的类会被过滤
     */
    var traceBlackSet: MutableSet<String>? = null
}
