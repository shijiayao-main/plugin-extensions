package com.jiaoay.plugins.core.annotation

import com.jiaoay.plugins.core.SdkPatcher
import com.jiaoay.plugins.core.asm.getValue
import com.jiaoay.plugins.core.config.ExtensionsPluginConfig
import org.gradle.api.Project
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class SdkPatcherAnnotation : PluginAnnotation {

    private val sdkPatcherAnnotationType: Type by lazy {
        Type.getType(SdkPatcher::class.java)
    }

    private var config: ExtensionsPluginConfig? = null

    override fun onPreAnnotationCheck(project: Project) {
        if (config == null) {
            config = ExtensionsPluginConfig.get(project)
        }
    }

    override fun checkAnnotationNode(
        className: String,
        classNode: ClassNode,
        annotationNode: AnnotationNode,
    ) {
        val annotationNodeDesc = annotationNode.desc
        if (annotationNodeDesc != sdkPatcherAnnotationType.descriptor) {
            return
        }
        val packageName = annotationNode.getValue<String>("name")?.replace(":", "-") ?: return

        if (packageName.isEmpty()) {
            return
        }
        logger("package name: $packageName, className: $className")
        config?.replaceClassMap?.let { map ->
            map[packageName]?.let {
                if (it.contains(className).not()) {
                    logger("is not in replaceClassMap")
                    it.add(className)
                } else {
                    logger("is in replaceClassMap")
                }
            } ?: let {
                map[packageName] = mutableListOf(className)
            }
        } ?: let {
            val replaceClassMap: MutableMap<String, MutableList<String>> = HashMap()
            replaceClassMap[packageName] = mutableListOf(className)
            config?.replaceClassMap = replaceClassMap
        }
    }
}
