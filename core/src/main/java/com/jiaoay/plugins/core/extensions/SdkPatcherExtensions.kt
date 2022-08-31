package com.jiaoay.plugins.core.extensions

import com.android.SdkConstants
import com.android.build.api.transform.TransformInvocation
import com.jiaoay.plugins.core.Replace
import com.jiaoay.plugins.core.asm.getValue
import com.jiaoay.plugins.core.config.ExtensionsPluginConfig
import com.jiaoay.plugins.core.isQualifiedClass
import com.jiaoay.plugins.core.logger
import java.io.File
import java.io.FileInputStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode


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