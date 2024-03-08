package com.jiaoay.plugins.core.extensions

import com.jiaoay.plugins.core.config.ExtensionsPluginConfig

fun ExtensionsPluginConfig.getTargetJarList(jarName: String): List<String> {
    if (isEnableSdkPatcher.not()) {
        return listOf()
    }
    val map = replaceClassMap ?: return listOf()
    val list: MutableList<String> = ArrayList()
    map.keys.forEach {
        if (jarName.contains(it)) {
            map[it]?.let { classList ->
                list.addAll(classList)
            }
        }
    }
    return list
}

fun isTargetClass(list: List<String>, className: String): Boolean {
    if (list.isEmpty()) {
        return false
    }

    return list.contains(className)
}
