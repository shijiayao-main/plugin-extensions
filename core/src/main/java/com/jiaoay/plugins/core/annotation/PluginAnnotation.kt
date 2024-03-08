package com.jiaoay.plugins.core.annotation

import org.gradle.api.Project
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

interface PluginAnnotation {

    fun onPreAnnotationCheck(project: Project)

    fun checkAnnotationNode(
        className: String,
        classNode: ClassNode,
        annotationNode: AnnotationNode,
    )
}
